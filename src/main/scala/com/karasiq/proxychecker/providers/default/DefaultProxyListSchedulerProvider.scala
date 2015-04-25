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

/**
 * Proxy list scheduler configuration
 * @param threads Threads number
 * @param interval Reloading interval
 * @param retention Dead proxy retention
 * @param refresh Recheck existing proxies
 * @param encoding Sources encoding
 */
private final case class SchedulerConfig(threads: Int, interval: FiniteDuration, retention: FiniteDuration, refresh: Boolean, encoding: String) {
  def createExecutionContext() = {
    ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(this.threads))
  }
}

private object SchedulerConfig {
  /**
   * Loads configuration from Typesafe Config
   * @param cfg Configuration object
   * @return Scheduler configuration
   */
  def apply(cfg: Config): SchedulerConfig = {
    SchedulerConfig(cfg.getInt("threads"), cfg.getDuration("interval", TimeUnit.SECONDS).seconds,
      cfg.getDuration("retention", TimeUnit.SECONDS).seconds, cfg.getBoolean("refresh"), cfg.getString("encoding"))
  }

  private def defaultConfigKey = "proxyChecker.scheduler"

  /**
   * Loads default configuration
   * @return Scheduler configuration
   */
  def apply(): SchedulerConfig = {
    val cfg = ConfigFactory.load().getConfig(defaultConfigKey)
    apply(cfg)
  }
}

private object URLLoader {
  private def timeout: Int = 10000
  
  def openURL[T](url: String)(f: InputStream ⇒ T): T = {
    val inputStream: InputStream = {
      val connection = new URL(url).openConnection()
      connection.setConnectTimeout(timeout)
      connection.setReadTimeout(timeout)
      connection.getInputStream
    }

    Exception.allCatch.andFinally(inputStream.close()) {
      f(inputStream)
    }
  }
}

trait DefaultProxyListSchedulerProvider extends ProxyListSchedulerProvider {
  self: ActorSystemProvider with WebServiceCacheProvider with ProxyStoreProvider with ProxyListParserProvider with ProxyCheckerMeasurerActorProvider with GeoipResolverProvider ⇒
  private val log: LoggingAdapter = Logging.getLogger(actorSystem, "ProxyListScheduler")

  private val config: SchedulerConfig = SchedulerConfig()

  implicit val listSchedulerEc = config.createExecutionContext()

  private def scheduleRemoving(proxyList: ProxyList, retention: FiniteDuration, address: String): Unit = {
    def removeProxy(): Unit = {
      def proxyIsDead(p: ProxyStoreEntry) = !p.isAlive && p.lastCheck.until(Instant.now(), ChronoUnit.SECONDS) > 30
      if (proxyList.contains(address) && proxyIsDead(proxyList(address))) {
        proxyList.remove(address)
        log.debug("Removing dead proxy: {}", address)
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
    import GeoipResolver._ // Implicits
    list.put(ProxyStoreEntry(address, geoip = geoipResolver(ProxyStoreEntry.addressToUri(address).getHost))) // Add to list
    scanProxy(address) // Schedule for checking
  }

  private def refreshProxy(address: String): Unit = {
    proxyStore.update(address) // Reset to dead
    scanProxy(address)
  }

  private def updateListFrom(list: ProxyList, url: String): Unit = {
    @inline
    def loadAsync(url: String): Future[Set[String]] = Future {
      log.debug("Loading: {}", url)
      URLLoader.openURL(url) { inputStream ⇒
        val source = Source.fromInputStream(inputStream, config.encoding)
        proxyListParser(source.getLines()).toSet
      }
    }

    loadAsync(url).onComplete {
      case Success(proxies) ⇒
        log.info("Parsed {} proxies from {} to [{}]", proxies.size, url, list.name)
        if (proxies.nonEmpty) {
          proxies.foreach { address ⇒
            addProxy(list, address) // Queue proxy for checking
            scheduleRemoving(list, config.retention, address) // Auto-remove dead proxy
          }
          webServiceCache.remove(list.name) // Update cache
        }

      case Failure(e) ⇒
        log.error(e, "Error loading from source: {}", url)
    }
  }

  private def refreshExisting(list: ProxyList): Unit = {
    val toRefresh = list.valuesIterator.filter(_.lastCheck.until(Instant.now(), ChronoUnit.SECONDS) > config.interval.toSeconds).toVector
    if (toRefresh.nonEmpty) {
      log.info("Rescanning {} proxies from list [{}]", toRefresh.size, list.name)
      toRefresh.foreach { p ⇒
        refreshProxy(p.address)
        scheduleRemoving(list, config.retention, p.address)
      }
      webServiceCache.remove(list.name)
    }
  }

  private def loadSourcesToList(listName: String, sources: Set[String]): Unit = {
    log.debug("Loading {} sources to list [{}]", sources.size, listName)
    val list: ProxyList = proxyStore(listName)
    if (config.refresh) refreshExisting(list)

    // Load from sources
    sources.foreach(url ⇒ updateListFrom(list, url))
  }

  override final val proxyListScheduler = ProxyListScheduler(config.interval)(loadSourcesToList)(actorSystem)

  private def scanAllListsIn(duration: FiniteDuration) = {
    log.info("Proxy lists will be updated in {}", duration)
    actorSystem.scheduler.scheduleOnce(duration) {
      // Initial scanning
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
