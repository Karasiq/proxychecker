package com.karasiq.proxychecker.store

import java.time.Instant

import scala.collection.mutable
import scala.concurrent.duration._

object ProxyCollection {
  implicit def proxyCollectionToSeq(c: ProxyCollection): Seq[ProxyStoreEntry] = c.valuesIterator.toVector
}

trait ProxyCollection extends mutable.Map[String, ProxyStoreEntry] {
  final def put(e: ProxyStoreEntry): Unit = this += (e.address → e)

  final def update(address: String, detectedProtocol: String, latency: Duration = Duration.Inf): Unit = {
    val value = this.get(address)
    value.foreach { v ⇒
      val newValue = ProxyStoreEntry(v.address, detectedProtocol, latency, v.geoip, if (ProxyStoreEntry.isAlive(latency)) Instant.now() else v.lastCheck)
      update(address, newValue)
    }
  }
}

abstract class ProxyCollectionImpl[M <: mutable.Map[String, ProxyStoreEntry]](@transient protected val entryMap: M) extends ProxyCollection {
  override def +=(kv: (String, ProxyStoreEntry)): this.type = {
    entryMap.+=(kv)
    this
  }

  override def -=(key: String): this.type = {
    entryMap.-=(key)
    this
  }

  override def get(key: String): Option[ProxyStoreEntry] = {
    entryMap.get(key)
  }

  override def iterator: Iterator[(String, ProxyStoreEntry)] = {
    entryMap.iterator
  }
}
