package com.karasiq.proxychecker.webservice

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import akka.actor._
import com.karasiq.geoip.GeoipResolver
import com.karasiq.proxychecker.providers.ProxyCheckerServicesProvider
import com.karasiq.proxychecker.store.{InMemoryProxyCollection, ProxyList, ProxyStoreEntry, ProxyStoreJsonProtocol}
import com.karasiq.proxychecker.worker
import com.karasiq.proxychecker.worker.MeasuredProxy
import com.typesafe.config.ConfigFactory
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.frame.TextFrame
import spray.can.{Http, websocket}
import spray.http.{ContentTypes, ContentType, HttpEntity, StatusCodes}
import spray.httpx.marshalling.Marshaller
import spray.json._
import spray.routing._
import spray.httpx._

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.control
import ProxyStoreJsonProtocol._

private class Marshallers(implicit ec: ExecutionContext) {
  implicit val StringSeq = Marshaller.delegate[Seq[String], String](ContentTypes.`application/json`)(seq ⇒ seq.toJson.compactPrint)

  implicit val ProxyStoreSeqJson = Marshaller.delegate[Future[Seq[ProxyStoreEntry]], Future[String]](ContentTypes.`application/json`)(f ⇒ f.map[String](seq ⇒ seq.toJson.compactPrint))
}

trait ProxyCheckerWebServiceProvider { self: ProxyCheckerServicesProvider ⇒
  private implicit def executionContext: ExecutionContext = actorSystem.dispatcher

  private val marshallers = new Marshallers()

  import marshallers._

  // Default (in-memory) list
  private val defaultListName: String = ""
  proxyStore += (defaultListName → ProxyList(defaultListName, Set(), new InMemoryProxyCollection))

  @inline
  private def cachedProxyList(listName: String): Future[ProxyList] = {
    webServiceCache(listName)
  }

  private val config = ConfigFactory.load()

  private val readOnly = config.getBoolean("proxyChecker.readOnly")

  private val temporaryRetention = config.getDuration("proxyChecker.temporaryRetention", TimeUnit.SECONDS).seconds

  private def addProxy(listName: String, address: String, temp: Boolean, retention: FiniteDuration = temporaryRetention): Unit = {
    import GeoipResolver._

    val proxyList = proxyStore(listName)

    if (!proxyList.contains(address)) {
      // Retrieve geoip data
      val geoip = geoipResolver(ProxyStoreEntry.addressToUri(address).getHost)

      // Persist
      proxyList.put(ProxyStoreEntry(address, geoip = geoip))

      // Check proxy
      proxyCheckerMeasurerActor ! worker.Proxy(address)
    } else {
      refreshProxy(address)
    }

    if (temp) actorSystem.scheduler.scheduleOnce(retention) {
      def proxyIsDead(p: ProxyStoreEntry) = !p.isAlive && p.lastCheck.until(Instant.now(), ChronoUnit.SECONDS) > 30

      control.Exception.ignoring(classOf[NoSuchElementException]) {
        if (proxyIsDead(proxyList(address))) {
          proxyList.remove(address)
          actorSystem.log.debug("Removing temporal proxy: {}", address)
          webServiceCache.remove(listName)
        }
      }
    }
  }

  private def parseProxyList(listName: String, text: String, temp: Boolean): Unit = {
    val proxyList = proxyListParser(text).toSeq.distinct
    actorSystem.log.info("Adding {} proxies to list [{}] ({})", proxyList.size, listName, if (temp) "temporary" else "permanent")
    proxyList.foreach(addProxy(listName, _, temp))
    webServiceCache.remove(listName)
  }

  private def removeProxy(listName: String, address: String): Unit = {
    proxyStore(listName).remove(address)
  }

  private def refreshProxy(address: String): Unit = {
    proxyStore.update(address, latency = Duration.Inf) // Reset to "dead"
    proxyCheckerMeasurerActor ! worker.Proxy(address)
  }

  private def filterProxies(alive: Option[Boolean] = None, protocol: Option[String] = None, country: Option[String] = None, newerThan: Option[Int] = None, olderThan: Option[Int] = None, latency: Option[Int] = None)(pl: ProxyList): Seq[ProxyStoreEntry] = {
    lazy val now = Instant.now()
    pl.valuesIterator.filter { proxy ⇒
      alive.fold(true)(proxy.isAlive ==) &&
        protocol.fold(true)(proxy.protocol ==) &&
        country.fold(true)(proxy.geoip.code ==) &&
        newerThan.fold(true)(rc ⇒ proxy.lastCheck.until(now, ChronoUnit.SECONDS) < rc) &&
        olderThan.fold(true)(rc ⇒ proxy.lastCheck.until(now, ChronoUnit.SECONDS) > rc) &&
        latency.fold(true)(lt ⇒ proxy.speed.toMillis <= lt)
    }.toSeq
  }

  sealed class ProxyCheckerHttpService extends HttpService {
    override implicit def actorRefFactory: ActorRefFactory = actorSystem

    private def processFilteredList(listName: String): Directive1[Future[Seq[ProxyStoreEntry]]] = {
      import shapeless._
      val params = parameters("alive".as[Boolean].?, "protocol".?, "country".?, "newerThan".as[Int].?, "olderThan".as[Int].?, "latency".as[Int].?)
      params.hmap {
        case alive :: protocol :: country :: newerThan :: olderThan :: latency :: HNil ⇒
          cachedProxyList(listName).map(filterProxies(alive, protocol, country, newerThan, olderThan, latency))
      }
    }

    /**
     * Extracts list name from parameter or from cookie
     */
    private def listName: Directive1[String] = {
      import shapeless._
      (optionalCookie("list") & parameter("list".?)).hmap {
        case cookie :: parameter :: HNil ⇒
          parameter.orElse(cookie.map(_.content))
            .filter(proxyStore.contains) // Only valid list name
            .getOrElse(defaultListName)
      }
    }

    final def route: Route = listName { listName ⇒
      get {
        (path("proxylist.txt") & processFilteredList(listName)) { list ⇒
          complete(list.map(_.map(_.address).mkString("\n")))
        } ~
        compressResponse((): Unit) {
          path("lists.json")(complete {
            proxyStore.keysIterator
              .filter(_.nonEmpty)
              .toSeq
          }) ~
          (path("proxylist.json") & processFilteredList(listName)) { list ⇒
            complete(list)
          } ~
          pathPrefix("flag") {
            getFromResourceDirectory("flags")
          } ~
          pathSingleSlash(getFromResource("webapp/index.html")) ~
          getFromResourceDirectory("webapp")
        }
      } ~
      validate(!readOnly, "Not available in read-only mode") {
        (get & path("sources.json"))(complete {
          proxyStore(listName).sources.toJson.compactPrint
        }) ~
        post {
          (path("proxylist") & entity(as[String]) & parameter("temp".as[Boolean].?(false)))((text, temp) ⇒ complete {
            parseProxyList(listName, text, temp)
            StatusCodes.OK
          }) ~
            pathPrefix("rescan") {
              parameter("address")(address ⇒ complete {
                actorSystem.log.info("Rescanning proxy: {}", address)
                refreshProxy(address)
                webServiceCache.clear()
                StatusCodes.OK
              }) ~
                processFilteredList(listName)(f ⇒ onSuccess(f) { pl ⇒
                  actorSystem.log.info("Rescanning {} proxies from list [{}]", pl.size, listName)
                  pl.foreach(p ⇒ refreshProxy(p.address))
                  webServiceCache.remove(listName)
                  complete(StatusCodes.OK)
                })
            } ~
            (path("sources") & parameter("list") & entity(as[String]))((list, sources) ⇒ complete {
              val src = sources.lines.filter(_.nonEmpty).toSet
              actorSystem.log.info("Subscriptions changed for list {} ({} addresses)", list, src.size)
              proxyStore.update(list, proxyStore(list).copy(sources = src))
              proxyListScheduler.schedule(list, src)
              StatusCodes.OK
            })
        } ~
        put {
          (path("list") & parameters("list", "inmemory".as[Boolean].?(false)))((newListName, inMemory) ⇒ complete {
            if (inMemory) {
              actorSystem.log.info("In-memory list created: {}", newListName)
              proxyStore += (newListName → ProxyList(newListName, Set(), new InMemoryProxyCollection))
            } else {
              actorSystem.log.info("List created: {}", newListName)
              proxyStore.createList(newListName)
            }

            StatusCodes.OK
          })
        } ~
        delete {
          pathPrefix("proxy") {
            parameter("address")(address ⇒ complete {
              actorSystem.log.info("Deleting proxy {} from [{}]", address, listName)
              removeProxy(listName, address)
              webServiceCache.remove(listName)
              StatusCodes.OK
            }) ~
              processFilteredList(listName)(onSuccess(_) { toDelete ⇒
                actorSystem.log.info("Deleting {} proxies from [{}]", toDelete.size, listName)
                val proxyList = proxyStore(listName).proxies
                toDelete.foreach(p ⇒ proxyList.remove(p.address))
                webServiceCache.remove(listName)
                complete(StatusCodes.OK)
              })
          } ~
            path("list")(complete {
              actorSystem.log.info("Deleting list: {}", listName)
              proxyListScheduler.cancel(listName)
              proxyStore -= listName
              webServiceCache.remove(listName)
              StatusCodes.OK
            })
        }
      }
    }
  }

  object ProxyCheckerService {
    def props() = Props(new ProxyCheckerService)
  }

  /**
   * Http service
   */
  final class ProxyCheckerService extends Actor with ActorLogging {
    def receive = {
      // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
      case Http.Connected(remoteAddress, localAddress) ⇒
        val serverConnection = sender()
        val conn = context.actorOf(ProxyCheckerWorker.props(serverConnection))
        serverConnection ! Http.Register(conn)
    }
  }

  private object ProxyCheckerWorker {
    def props(serverConnection: ActorRef) = Props(new ProxyCheckerWorker(serverConnection))
  }

  /**
   * WebSocket/Http worker
   */
  private final class ProxyCheckerWorker(val serverConnection: ActorRef) extends ProxyCheckerHttpService with websocket.WebSocketServerWorker {
    private var currentList: String = ""

    @scala.throws[Exception](classOf[Exception])
    override def postStop(): Unit = {
      proxyCheckerMeasurerEventBus.unsubscribe(self)
      super.postStop()
    }

    @scala.throws[Exception](classOf[Exception])
    override def preStart(): Unit = {
      super.preStart()
      proxyCheckerMeasurerEventBus.subscribe(self, null) // Receive proxy updates
    }

    override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

    def businessLogic: Receive = {
      case TextFrame(cmd) if cmd.startsWith("list=") ⇒
        val list = cmd.drop(5)
        log.debug("WebSocket subscription list changed to {}", list.utf8String)
        currentList = list.utf8String

      case r: MeasuredProxy if proxyStore(currentList).contains(r.address) ⇒
        val entry = ProxyStoreEntry(r.address, r.protocol, r.time)
        send(TextFrame(entry.toJson.compactPrint)) // Web-Socket push

      case x: FrameCommandFailed ⇒
        log.error("Frame command failed: {}", x)
    }

    def businessLogicNoUpgrade: Receive = runRoute(route)
  }
}

