package com.karasiq.proxychecker.store

import java.net.URI
import java.time.Instant

import com.karasiq.geoip.GeoipResult

import scala.concurrent.duration._

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
    ProxyStoreEntry(stripAddress(address), speed, if (protocol.nonEmpty) protocol else getProtocol(address), lastCheck, geoip)

  def isAlive(latency: Duration) = latency.isFinite()
}

final case class ProxyStoreEntry(address: String, speed: Duration, protocol: String, lastCheck: Instant, geoip: GeoipResult) {
  def isAlive: Boolean = ProxyStoreEntry.isAlive(speed)

  override def toString: String = {
    s"ProxyStoreEntry($protocol://$address, $speed, $lastCheck, $geoip)"
  }

  override def equals(obj: scala.Any): Boolean = obj match {
    case pse: ProxyStoreEntry ⇒ pse.address == this.address
    case _ ⇒ false
  }

  override def hashCode(): Int = address.hashCode
}
