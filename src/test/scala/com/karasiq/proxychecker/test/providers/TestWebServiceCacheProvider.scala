package com.karasiq.proxychecker.test.providers

import com.karasiq.proxychecker.providers.{ProxyStoreProvider, WebServiceCacheProvider}
import com.karasiq.proxychecker.store.ProxyList
import com.karasiq.proxychecker.webservice.WebServiceCache

import scala.concurrent.Future

trait TestWebServiceCacheProvider extends WebServiceCacheProvider { self: ProxyStoreProvider ⇒
  override def webServiceCache: WebServiceCache = new WebServiceCache {
    override def clear(): Unit = ()
    override def remove(key: String): Unit = ()
    override def apply(key: String): Future[ProxyList] = Future.successful(proxyStore(key))
    override def close(): Unit = ()
  }
}
