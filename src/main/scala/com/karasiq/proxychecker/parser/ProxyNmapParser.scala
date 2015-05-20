package com.karasiq.proxychecker.parser

import scala.annotation.tailrec
import scala.util.Try

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
    @tailrec
    def findNext(ip: Option[String], port: Option[String], lines: Iterator[String]): Option[String] = {
      def parseAddress(ipLine: Option[String], portLine: Option[String]): Option[String] = {
        for {
          ip <- ipLine.flatMap(l ⇒ Try(l.split("Nmap scan report for ", 2)(1)).toOption)
          port <- portLine.flatMap(l ⇒ Try(l.split("/tcp open", 2)(0)).toOption)
        } yield s"$ip:$port"
      }

      if (!lines.hasNext) None
      else {
        val (newIp, newPort, found) = lines.next() match {
          case line if line.startsWith("Nmap scan report for ") ⇒
            (Some(line), port, false)

          case line if line.contains("/tcp open") ⇒
            (ip, Some(line), line.contains("Open SOCKS Proxy") || line.contains("Open HTTP Proxy"))

          case line if line.contains("Potentially OPEN proxy") ⇒
            (ip, port, true)

          case _ ⇒
            (ip, port, false)
        }

        if (found) parseAddress(newIp, newPort)
        else findNext(newIp, newPort, lines)
      }
    }

    Iterator.continually(findNext(None, None, lines)).takeWhile(_.nonEmpty).flatten
  }
}
