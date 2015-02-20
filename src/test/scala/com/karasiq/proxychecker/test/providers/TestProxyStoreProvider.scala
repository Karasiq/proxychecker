package com.karasiq.proxychecker.test.providers

import com.karasiq.proxychecker.providers.ProxyStoreProvider
import com.karasiq.proxychecker.store.{InMemoryProxyStore, ProxyStore}

trait TestProxyStoreProvider extends ProxyStoreProvider {
 override final val proxyStore: ProxyStore = new InMemoryProxyStore
}
