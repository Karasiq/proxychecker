package com.karasiq.proxychecker.store

import java.net.URI
import java.time.Instant

import com.karasiq.geoip.GeoipResult
import com.karasiq.proxychecker.watcher.ProxyWatcher
import com.karasiq.proxychecker.worker.MeasuredProxy

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

sealed trait ProxyStatus

object ProxyStoreEntry {
  @inline
  private def defaultProto(address: String) = {
    if (address.contains("://")) address
    else "http://" + address
  }

  def getProtocol(address: String) = {
    addressToUri(address).getScheme
  }

  def addressToUri(address: String) = {
    new URI(defaultProto(address))
  }

  def stripAddress(address: String) = {
    val uri = addressToUri(address)
    s"${uri.getHost}:${uri.getPort}"
  }

  def apply(address: String, protocol: String = "", speed: Duration = Duration.Inf, geoip: GeoipResult = GeoipResult(), lastCheck: Instant = Instant.now()): ProxyStoreEntry =
    new ProxyStoreEntryImpl(stripAddress(address), speed, if (protocol.nonEmpty) protocol else getProtocol(address), lastCheck, geoip)

  def isAlive(latency: Duration) = latency.isFinite()
}

sealed trait ProxyStoreEntry {
  def address: String
  def speed: Duration
  def protocol: String
  def lastCheck: Instant
  def geoip: GeoipResult
  final def isAlive: Boolean = ProxyStoreEntry.isAlive(speed)

  override def toString: String = {
    s"ProxyStoreEntry($protocol://$address, $speed, $lastCheck, $geoip)"
  }

  override def equals(obj: scala.Any): Boolean = obj match {
    case pse: ProxyStoreEntry ⇒ pse.address == this.address
    case _ ⇒ false
  }

  override def hashCode(): Int = address.hashCode
}

final private class ProxyStoreEntryImpl(val address: String, val speed: Duration = Duration.Inf, val protocol: String = "", val lastCheck: Instant, val geoip: GeoipResult) extends ProxyStoreEntry with Serializable

object ProxyCollection {
  implicit def proxyCollectionToSeq(c: ProxyCollection): Seq[ProxyStoreEntry] = c.valuesIterator.toVector
}

trait ProxyCollection extends mutable.AbstractMap[String, ProxyStoreEntry] {
  final def put(e: ProxyStoreEntry): Unit = this += (e.address → e)

  final def update(address: String, detectedProtocol: String, latency: Duration = Duration.Inf): Unit = {
    val value = this.get(address)
    value.foreach { v ⇒
      val newValue = ProxyStoreEntry(v.address, detectedProtocol, latency, v.geoip, if (ProxyStoreEntry.isAlive(latency)) Instant.now() else v.lastCheck)
      update(address, newValue)
    }
  }
}

case class ProxyList(name: String, sources: Set[String], proxies: ProxyCollection)

object ProxyList {
  implicit def proxyListToCollection(pl: ProxyList): ProxyCollection = pl.proxies
}

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