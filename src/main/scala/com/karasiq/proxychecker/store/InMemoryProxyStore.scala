package com.karasiq.proxychecker.store

import scala.collection.concurrent.TrieMap

class InMemoryProxyCollection extends ProxyCollectionImpl(TrieMap.empty[String, ProxyStoreEntry]) with Serializable

class InMemoryProxyStore extends ProxyStoreImpl(TrieMap.empty[String, ProxyList]) {
  override def createList(name: String): ProxyList = {
    val collection = new InMemoryProxyCollection()
    val list = ProxyList(name, Set(), collection)
    this += (name → list)
    list
  }
}
