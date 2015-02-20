package com.karasiq.proxychecker.parser

/**
 * Parser for host:port paste
 */
class ProxyPlainParser extends ProxyListParser {
  /**
   * @param raw Raw proxy paste
   * @return List of host:port
   */
  override def apply(raw: String): Iterator[String] = raw.lines

  /**
   * @param lines Lines iterator
   * @return List of host:port
   */
  override def apply(lines: Iterator[String]): Iterator[String] = lines
}
