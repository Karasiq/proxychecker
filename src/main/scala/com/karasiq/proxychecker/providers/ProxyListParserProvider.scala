package com.karasiq.proxychecker.providers

import com.karasiq.proxychecker.parser.ProxyListParser


/**
 * Provider for proxy list parser
 */
trait ProxyListParserProvider {
  def proxyListParser: ProxyListParser
}
