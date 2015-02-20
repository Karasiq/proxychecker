package com.karasiq.proxychecker.parser

trait ProxyListParser {
  /**
   * @param raw Raw proxy paste
   * @return List of host:port
   */
  def apply(raw: String): Iterator[String]

  /**
   * @param lines Lines iterator
   * @return List of host:port
   */
  def apply(lines: Iterator[String]): Iterator[String]
}
