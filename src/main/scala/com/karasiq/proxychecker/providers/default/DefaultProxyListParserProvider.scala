package com.karasiq.proxychecker.providers.default

import com.karasiq.proxychecker.parser.ProxyListParser
import com.karasiq.proxychecker.providers.ProxyListParserProvider
import com.typesafe.config.ConfigFactory

trait DefaultProxyListParserProvider extends ProxyListParserProvider {
  override final val proxyListParser: ProxyListParser = {
    val cfg = ConfigFactory.load().getConfig("proxyChecker.parser")
    val class_ = Class.forName(cfg.getString("class"))
    assert(classOf[ProxyListParser].isAssignableFrom(class_), "Invalid proxy list parser class")
    class_.newInstance().asInstanceOf[ProxyListParser]
  }
}
