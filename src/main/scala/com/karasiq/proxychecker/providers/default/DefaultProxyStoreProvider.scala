package com.karasiq.proxychecker.providers.default

import com.karasiq.proxychecker.providers.ProxyStoreProvider
import com.karasiq.proxychecker.store.ProxyStore
import com.typesafe.config.ConfigFactory

trait DefaultProxyStoreProvider extends ProxyStoreProvider {
   override final val proxyStore: ProxyStore = {
     val cfg = ConfigFactory.load().getConfig("proxyChecker.store")
     val class_ = Class.forName(cfg.getString("class"))
     assert(classOf[ProxyStore].isAssignableFrom(class_), "Invalid proxy store class")
     class_.newInstance().asInstanceOf[ProxyStore]
   }
 }
