package com.karasiq.proxychecker.parser

import com.typesafe.config.ConfigFactory

import scala.util.matching.Regex
import scala.collection.JavaConversions._


/**
 * Parses ip:port from text with regular expression
 */
class ProxyRegexParser extends ProxyListParser {
  val regexList: Seq[Regex] = ConfigFactory.load().getStringList("proxyChecker.parser.regexes").map(_.r)

  /**
   * @param raw Raw proxy paste
   * @return List of host:port
   */
  override def apply(raw: String): Iterator[String] = {
    regexList
      .flatMap(_.findAllMatchIn(raw))
      .map(m ⇒ s"${m.group(1)}:${m.group(2)}")
      .distinct
      .toIterator
  }

  /**
   * @param lines Lines iterator
   * @return List of host:port
   */
  override def apply(lines: Iterator[String]): Iterator[String] = {
    lines.flatMap(apply) // Find in each line
  }
}
