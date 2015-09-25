package com.karasiq.proxychecker.store

import scala.collection.JavaConversions._

private object MapDbProxyCollection {
  @inline
  private def nameFor(listName: String): String = {
    "proxyStoreList_" + listName
  }

  def apply(listName: String): ProxyCollection = {
    new MapDbProxyCollection(nameFor(listName))
  }
}

final private class MapDbProxyCollection(name_ : String) extends ProxyCollection with Serializable {
  private val name: String = name_

  assert(name ne null)

  @transient
  private lazy val mapDb = ProxyCheckerMapDb.openDatabase()

  @transient
  private lazy val map = mapDb.db // Underlying
    .hashMap[String, ProxyStoreEntry](name)

  @inline
  private def commit(): Unit = {
    mapDb.commitScheduler.commit()
  }

  override def +=(kv: (String, ProxyStoreEntry)): this.type = {
    map.put(kv._1, kv._2)
    commit()
    this
  }

  override def -=(key: String): this.type = {
    map.remove(key)
    commit()
    this
  }

  override def get(key: String): Option[ProxyStoreEntry] = {
    if (map.containsKey(key)) Some(map.get(key))
    else None
  }

  override def contains(key: String): Boolean = {
    map.containsKey(key)
  }

  override def iterator: Iterator[(String, ProxyStoreEntry)] = {
    map.iterator
  }

  override def keySet: collection.Set[String] = {
    map.keySet() // Get keys without loading the values
  }

  override def keys: Iterable[String] = this.keySet.toIterable

  override def keysIterator: Iterator[String] = this.keySet.toIterator
}

final class MapDbProxyStore extends ProxyStore {
  private val mapDb = ProxyCheckerMapDb.openDatabase()

  private val listMap = mapDb.db.hashMap[String, ProxyList]("proxyStore")

  @inline
  private def commit(): Unit = {
    mapDb.commitScheduler.commit()
  }

  override def +=(kv: (String, ProxyList)): this.type = {
    listMap.put(kv._1, kv._2)
    commit()
    this
  }

  override def -=(key: String): this.type = {
    val pl = apply(key)
    pl.foreach(p ⇒ pl.remove(p._1))
    listMap.remove(key)
    commit()
    this
  }

  override def get(key: String): Option[ProxyList] = {
    if (listMap.containsKey(key)) Some(listMap.get(key))
    else None
  }

  override def iterator: Iterator[(String, ProxyList)] = {
    listMap.iterator
  }

  override def contains(name: String): Boolean = {
    listMap.containsKey(name)
  }

  override def createList(name: String): ProxyList = {
    assert(!contains(name), "List already exists: " + name)
    val collection = MapDbProxyCollection(name)
    val list = ProxyList(name, Set(), collection)
    this += (name → list)
    list
  }
}
