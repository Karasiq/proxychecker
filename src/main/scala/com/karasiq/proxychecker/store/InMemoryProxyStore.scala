package com.karasiq.proxychecker.store

import scala.collection.concurrent.TrieMap

class InMemoryProxyCollection extends ProxyCollection with Serializable {
  @transient // Not save
  protected lazy val underlying = TrieMap.empty[String, ProxyStoreEntry]

  override def +=(kv: (String, ProxyStoreEntry)): this.type = {
    underlying.+=(kv)
    this
  }

  override def -=(key: String): this.type = {
    underlying.-=(key)
    this
  }

  override def get(key: String): Option[ProxyStoreEntry] = {
    underlying.get(key)
  }

  override def iterator: Iterator[(String, ProxyStoreEntry)] = {
    underlying.iterator
  }
}

class InMemoryProxyStore extends ProxyStore {
  protected val underlying = TrieMap.empty[String, ProxyList]

  override def +=(kv: (String, ProxyList)): this.type = {
    underlying += kv
    this
  }

  override def -=(key: String): this.type = {
    underlying -= key
    this
  }

  override def get(key: String): Option[ProxyList] = {
    underlying.get(key)
  }

  override def iterator: Iterator[(String, ProxyList)] = {
    underlying.iterator
  }

  override def createList(name: String): ProxyList = {
    val collection = new InMemoryProxyCollection()
    val list = ProxyList(name, Set(), collection)
    this += (name → list)
    list
  }
}
