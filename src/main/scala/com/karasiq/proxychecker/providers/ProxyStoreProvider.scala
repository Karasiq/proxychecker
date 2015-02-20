package com.karasiq.proxychecker.providers

import com.karasiq.proxychecker.store.ProxyStore


/**
 * Provider for proxy storage
 */
trait ProxyStoreProvider {
  def proxyStore: ProxyStore
}
