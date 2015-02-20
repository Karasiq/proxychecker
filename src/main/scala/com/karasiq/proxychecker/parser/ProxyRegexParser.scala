package com.karasiq.proxychecker.parser

import com.typesafe.config.ConfigFactory


/**
 * Parses ip:port from text with regular expression
 */
class ProxyRegexParser extends ProxyListParser {
  val ipPortRegex = ConfigFactory.load().getString("proxyChecker.parser.regex").r

  /**
   * @param raw Raw proxy paste
   * @return List of host:port
   */
  override def apply(raw: String): Iterator[String] = {
    val matches = ipPortRegex.findAllMatchIn(raw)
    matches.map(m ⇒ s"${m.group(1)}:${m.group(2)}")
  }

  /**
   * @param lines Lines iterator
   * @return List of host:port
   */
  override def apply(lines: Iterator[String]): Iterator[String] = {
    lines.flatMap(apply) // Find in each line
  }
}
