package com.karasiq.proxychecker.store

import scala.language.implicitConversions

object ProxyList {
  implicit def proxyListToCollection(pl: ProxyList): ProxyCollection = pl.proxies
}

case class ProxyList(name: String, sources: Set[String], proxies: ProxyCollection)
