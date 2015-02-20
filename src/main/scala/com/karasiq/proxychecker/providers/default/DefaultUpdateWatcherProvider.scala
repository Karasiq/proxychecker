package com.karasiq.proxychecker.providers.default

import akka.actor.Props
import com.karasiq.proxychecker.providers._
import com.karasiq.proxychecker.store.{ProxyStore, ProxyStoreWatcher}
import com.karasiq.proxychecker.webservice.{ProxyCacheWatcher, WebServiceCache}
import com.karasiq.proxychecker.worker.ProxyCheckerMeasurerEventBus

trait DefaultUpdateWatcherProvider extends UpdateWatcherProvider { provider: ActorSystemProvider with ProxyCheckerMeasurerEventBusProvider with ProxyStoreProvider with WebServiceCacheProvider ⇒
   override final val updateWatcher = actorSystem.actorOf(Props(new ProxyStoreWatcher with ProxyCacheWatcher {
     override def proxyStore: ProxyStore = provider.proxyStore

     override def proxyCheckerMeasurerEventBus: ProxyCheckerMeasurerEventBus = provider.proxyCheckerMeasurerEventBus

     override def webServiceCache: WebServiceCache = provider.webServiceCache
   }), "proxyUpdateWatcher")

   proxyCheckerMeasurerEventBus.subscribe(updateWatcher, null)
 }
