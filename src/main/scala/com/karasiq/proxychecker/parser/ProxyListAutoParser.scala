package com.karasiq.proxychecker.parser

/**
 * Auto-detects proxy list type
 */
class ProxyListAutoParser extends ProxyListParser {
  private val nmapParser = new ProxyNmapParser
  private val regexParser = new ProxyRegexParser
  private val plainParser = new ProxyPlainParser

  /**
   * @param raw Raw proxy paste
   * @return List of host:port
   */
  override def apply(raw: String): Iterator[String] = {
    val parser = {
      if (raw.contains("Nmap scan report")) nmapParser
      else if ("([a-z][a-z0-9+\\-.]*://)?[a-z0-9\\-._~%]+".r.findFirstMatchIn(raw).exists(_.start(0) == 0)) plainParser
      else regexParser
    }
    parser(raw)
  }

  /**
   * @param lines Lines iterator
   * @return List of host:port
   */
  override def apply(lines: Iterator[String]): Iterator[String] = {
    val stream = lines.toStream
    val parser = {
      if (stream.exists(_.startsWith("Nmap scan report"))) nmapParser
      else if ("([a-z][a-z0-9+\\-.]*://)?[a-z0-9\\-._~%]+".r.findFirstMatchIn(stream.head).exists(_.start(0) == 0)) plainParser
      else regexParser
    }
    parser(stream.toIterator)
  }
}
