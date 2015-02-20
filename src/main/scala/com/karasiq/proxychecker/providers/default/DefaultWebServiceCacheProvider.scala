package com.karasiq.proxychecker.providers.default

import com.karasiq.proxychecker.providers.{ActorSystemProvider, ProxyStoreProvider, WebServiceCacheProvider}
import com.karasiq.proxychecker.webservice.{SprayWebServiceCache, WebServiceCache}

trait DefaultWebServiceCacheProvider extends WebServiceCacheProvider { self: ActorSystemProvider with ProxyStoreProvider ⇒
  override final val webServiceCache: WebServiceCache = new SprayWebServiceCache(proxyStore)(actorSystem.dispatcher)
}
