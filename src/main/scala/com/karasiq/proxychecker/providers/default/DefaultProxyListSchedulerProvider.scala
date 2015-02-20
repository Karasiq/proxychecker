package com.karasiq.proxychecker.providers.default

import java.io.InputStream
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.{Executors, TimeUnit}

import akka.event.{Logging, LoggingAdapter}
import com.karasiq.geoip.GeoipResolver
import com.karasiq.proxychecker.providers._
import com.karasiq.proxychecker.scheduler.ProxyListScheduler
import com.karasiq.proxychecker.store.{ProxyList, ProxyStoreEntry}
import com.karasiq.proxychecker.worker.Proxy
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.language.postfixOps
import scala.util.control.Exception
import scala.util.{Failure, Success}

private class SchedulerConfig(cfg: Config) {
  val threads: Int = cfg.getInt("threads")
  val interval: FiniteDuration = cfg.getDuration("interval", TimeUnit.SECONDS).seconds
  val retention: FiniteDuration = cfg.getDuration("retention", TimeUnit.SECONDS).seconds
  val refresh: Boolean = cfg.getBoolean("refresh")
  val encoding: String = cfg.getString("encoding")
}

trait DefaultProxyListSchedulerProvider extends ProxyListSchedulerProvider {
  self: ActorSystemProvider with WebServiceCacheProvider with ProxyStoreProvider with ProxyListParserProvider with ProxyCheckerMeasurerActorProvider with GeoipResolverProvider ⇒
  private val log: LoggingAdapter = Logging.getLogger(actorSystem, "ProxyListScheduler")

  private val config: SchedulerConfig = new SchedulerConfig(ConfigFactory.load().getConfig("proxyChecker.scheduler"))

  implicit val listSchedulerEc = ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(config.threads))

  private def scheduleRemoving(proxyList: ProxyList, retention: FiniteDuration, address: String): Unit = {
    def removeProxy(): Unit = {
      def proxyIsDead(p: ProxyStoreEntry) = !p.isAlive && p.lastCheck.until(Instant.now(), ChronoUnit.SECONDS) > 30
      if (proxyList.contains(address) && proxyIsDead(proxyList(address))) {
        proxyList.remove(address)
        log.debug("Removing temporal proxy: {}", address)
        webServiceCache.remove(proxyList.name)
      }
    }

    Exception.ignoring(classOf[IllegalStateException]) {
      actorSystem.scheduler.scheduleOnce(retention)(removeProxy())(actorSystem.dispatcher)
    }
  }

  private def scanProxy(address: String): Unit = {
    proxyCheckerMeasurerActor ! Proxy(address)
  }

  private def addProxy(list: ProxyList, address: String): Unit = {
    import GeoipResolver._
    list.put(ProxyStoreEntry(address, geoip = geoipResolver(ProxyStoreEntry.addressToUri(address).getHost)))
    scanProxy(address)
  }

  private def refreshProxy(address: String): Unit = {
    proxyStore.update(address) // Reset to dead
    scanProxy(address)
  }

  private def loadFromURL(list: ProxyList, url: String): Unit = {
    def load(url: String): Seq[String] = {
      log.debug("Loading: {}", url)

      // Open connection
      val inputStream: InputStream = {
        val connection = new URL(url).openConnection()
        connection.setConnectTimeout(10000)
        connection.setReadTimeout(10000)
        connection.getInputStream
      }

      Exception.allCatch.andFinally(inputStream.close()) {
        val source = Source.fromInputStream(inputStream, config.encoding)
        proxyListParser(source.getLines()).toSeq.distinct
      }
    }

    Future(load(url)).onComplete {
      case Success(proxies) ⇒
        log.info("Parsed {} proxies from {} to [{}]", proxies.length, url, list.name)
        if (proxies.nonEmpty) {
          proxies.foreach { address ⇒
            addProxy(list, address)
            scheduleRemoving(list, config.retention, address)
          }
          webServiceCache.remove(list.name)
        }

      case Failure(e) ⇒
        log.error(e, "Error loading from source: {}", url)
    }
  }

  private def refreshExisting(list: ProxyList): Unit = {
    val toRefresh = list.valuesIterator.filter(_.lastCheck.until(Instant.now(), ChronoUnit.SECONDS) > config.interval.toSeconds).toSeq
    if (toRefresh.nonEmpty) {
      log.info("Rescanning {} proxies from list [{}]", toRefresh.size, list.name)
      toRefresh.foreach { p ⇒
        refreshProxy(p.address)
        scheduleRemoving(list, config.retention, p.address)
      }
      webServiceCache.remove(list.name)
    }
  }

  override final val proxyListScheduler = ProxyListScheduler(config.interval)((listName, sources) ⇒ {
    log.debug("Loading {} sources to list [{}]", sources.size, listName)
    val list = proxyStore(listName)
    if (config.refresh) refreshExisting(list)

    // Load from sources
    sources.foreach(loadFromURL(list, _))
  })(actorSystem)

  private def scanAllListsIn(duration: FiniteDuration) = {
    log.info("Proxy lists will be updated in {}", duration)
    actorSystem.scheduler.scheduleOnce(duration) {
      proxyStore.valuesIterator.foreach(list ⇒ proxyListScheduler.schedule(list.name, list.sources))
    }
  }

  override def finalize(): Unit = {
    listSchedulerEc.shutdown()
    super.finalize()
  }

  // Initial list loading
  scanAllListsIn(5 seconds)
}
