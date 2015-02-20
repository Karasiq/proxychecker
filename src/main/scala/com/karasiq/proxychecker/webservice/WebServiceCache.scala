package com.karasiq.proxychecker.webservice

import java.io.Closeable

import com.karasiq.proxychecker.store.{ProxyList, ProxyStore}
import com.karasiq.proxychecker.watcher.ProxyWatcher
import com.karasiq.proxychecker.worker.{MeasuredProxy, ProxyCheckerMeasurerEventBus}
import spray.caching.LruCache

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

trait WebServiceCache extends Closeable {
  /**
   * Get cached proxy list
   * @param key List name
   * @return Cached list
   */
  def apply(key: String): Future[ProxyList]

  /**
   * Evict cached list
   * @param key List name
   */
  def remove(key: String): Unit

  /**
   * Evict all lists
   */
  def clear(): Unit
}

class SprayWebServiceCache(proxyStore: ProxyStore)(implicit ec: ExecutionContext) extends WebServiceCache {

  private val proxyListCache = LruCache[ProxyList](maxCapacity = 50, timeToIdle = 10 minutes)

  override def clear(): Unit = proxyListCache.clear()

  override def apply(key: String): Future[ProxyList] = proxyListCache(key)(proxyStore(key))

  override def remove(key: String): Unit = proxyListCache.remove(key)

  override def close(): Unit = ()
}

trait ProxyCacheWatcher extends ProxyWatcher {
  def proxyCheckerMeasurerEventBus: ProxyCheckerMeasurerEventBus
  def webServiceCache: WebServiceCache

  abstract override def onResultReceive(r: MeasuredProxy): Unit = {
    super.onResultReceive(r)
    webServiceCache.clear()
  }
}