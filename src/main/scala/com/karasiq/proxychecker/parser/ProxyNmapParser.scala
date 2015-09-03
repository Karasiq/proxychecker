package com.karasiq.proxychecker.parser

import scala.annotation.tailrec
import scala.util.Try

/**
 * Parser for nmap scan report with NSE: `--script http-open-proxy,socks-open-proxy`
 */
class ProxyNmapParser extends ProxyListParser {
  private object PortLine {
    def unapply(line: String): Option[Int] = for {
      portLine <- Option(line).filter(_.contains("/tcp open"))
      port <- Try(portLine.split("/tcp open", 2)(0).toInt).toOption
    } yield port
  }
  
  private object IpLine {
    def unapply(line: String): Option[String] = for {
      ipLine <- Option(line).filter(_.contains("Nmap scan report for "))
      ip <- Try(ipLine.split("Nmap scan report for ", 2)(1)).filter(_.nonEmpty).toOption
    } yield ip
  }
  
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
    def findNext(lines: Iterator[String], ip: Option[String], port: Option[Int]): Option[String] = {
      def parseAddress(ipOption: Option[String], portOption: Option[Int]): Option[String] = for {
        ip <- ipOption
        port <- portOption
      } yield s"$ip:$port"

      if (!lines.hasNext) None else {
        val (newIp, newPort, found) = lines.next() match {
          case IpLine(foundIp) ⇒
            (Some(foundIp), None, false)

          case line @ PortLine(foundPort) ⇒
            (ip, Some(foundPort), line.contains("Open SOCKS Proxy") || line.contains("Open HTTP Proxy"))

          case line if line.contains("Potentially OPEN proxy") ⇒
            (ip, port, true)

          case _ ⇒
            (ip, port, false)
        }

        if (found) parseAddress(newIp, newPort)
        else findNext(lines, newIp, newPort)
      }
    }

    Iterator.continually(findNext(lines, None, None))
      .takeWhile(_.nonEmpty).flatten
  }
}
