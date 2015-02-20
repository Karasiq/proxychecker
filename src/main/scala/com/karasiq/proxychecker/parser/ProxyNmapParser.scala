package com.karasiq.proxychecker.parser

import scala.collection.mutable.ListBuffer

/**
 * Parser for nmap scan report with NSE: `--script http-open-proxy,socks-open-proxy`
 */
class ProxyNmapParser extends ProxyListParser {
  /**
   * @param raw Raw proxy paste
   * @return List of host:port
   */
  override def apply(raw: String): Iterator[String] = {
    apply(raw.lines)
  }

  /**
   * @param lines Lines iterator
   * @return List of host:port
   */
  override def apply(lines: Iterator[String]): Iterator[String] = {
    var lastIp = ""
    var lastPort = ""
    val buffer = ListBuffer.empty[String]
    def addToBuffer(): Unit = buffer.append(lastIp.split("Nmap scan report for ")(1) + ":" + lastPort.split("/tcp open")(0))
    
    lines.foreach { line ⇒
      if (line.startsWith("Nmap scan report for ")) lastIp = line
      if (line.contains("/tcp open")) {
        lastPort = line
        if (line.contains("Open SOCKS Proxy") || line.contains("Open HTTP Proxy")) addToBuffer() // Custom nmap-service-probes
      }
      if (line.contains("Potentially OPEN proxy")) addToBuffer()
    }
    buffer.iterator
  }
}
