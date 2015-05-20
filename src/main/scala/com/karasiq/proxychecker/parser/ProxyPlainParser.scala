package com.karasiq.proxychecker.parser

import java.net.InetSocketAddress

import scala.util.Try

/**
 * Parser for host:port paste
 */
class ProxyPlainParser extends ProxyListParser {
  private def isAddress(line: String): Boolean = {
    val address = Try(line.split(":", 2).toList match {
      case host :: port :: Nil ⇒
        InetSocketAddress.createUnresolved(host, port.toInt)

      case _ ⇒
        throw new IllegalArgumentException
    })
    address.isSuccess
  }

  /**
   * @param raw Raw proxy paste
   * @return List of host:port
   */
  override def apply(raw: String): Iterator[String] = this.apply(raw.lines)

  /**
   * @param lines Lines iterator
   * @return List of host:port
   */
  override def apply(lines: Iterator[String]): Iterator[String] = lines.filter(isAddress)
}
