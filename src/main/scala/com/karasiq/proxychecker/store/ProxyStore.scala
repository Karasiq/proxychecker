package com.karasiq.proxychecker.store

import com.karasiq.proxychecker.watcher.ProxyWatcher
import com.karasiq.proxychecker.worker.MeasuredProxy

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

trait ProxyStore extends mutable.AbstractMap[String, ProxyList] {
  def createList(name: String): ProxyList

  def update(address: String, protocol: String = "", latency: Duration = Duration.Inf): Unit = {
    valuesIterator
      .filter(_.contains(address))
      .foreach(_.update(address, if(protocol.nonEmpty) protocol else ProxyStoreEntry.getProtocol(address), latency))
  }
}

trait ProxyStoreWatcher extends ProxyWatcher {
  def proxyStore: ProxyStore

  abstract override def onResultReceive(r: MeasuredProxy): Unit = {
    super.onResultReceive(r)
    proxyStore.update(r.address, r.protocol, r.time)
  }
}

abstract class ProxyStoreImpl[M <: mutable.Map[String, ProxyList]](@transient protected val listMap: M) extends ProxyStore {
  override def +=(kv: (String, ProxyList)): this.type = {
    listMap.put(kv._1, kv._2)
    this
  }

  override def -=(key: String): this.type = {
    val pl: ProxyList = apply(key)
    pl.foreach(p ⇒ pl.remove(p._1))
    listMap.remove(key)
    this
  }

  override def get(key: String): Option[ProxyList] = {
    listMap.get(key)
  }

  override def iterator: Iterator[(String, ProxyList)] = {
    listMap.iterator
  }

  override def contains(name: String): Boolean = {
    listMap.contains(name)
  }
}