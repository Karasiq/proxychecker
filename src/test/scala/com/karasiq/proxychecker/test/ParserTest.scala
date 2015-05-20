package com.karasiq.proxychecker.test

import com.karasiq.proxychecker.parser.{ProxyListParser, ProxyNmapParser, ProxyPlainParser, ProxyRegexParser}
import org.scalatest.{FlatSpec, Matchers}

class ParserTest extends FlatSpec with Matchers {
  private def testIps: Vector[String] = Vector("1.2.3.4:8080", "5.6.7.8:3128")

  private def test(parser: ProxyListParser, input: String): Unit = {
    parser(input).toVector shouldBe testIps
  }

  "Plain parser" should "parse plain IPs" in {
    val parser = new ProxyPlainParser
    val input = "1.2.3.4:8080\r\n5.6.7.8:3128\r\nNot IP:port"

    test(parser, input)
  }

  "Regex parser" should "parse IPs from arbitrary data" in {
    val parser = new ProxyRegexParser
    val input =
      """
        |<div>1.2.3.4</div><div>8080</div>
        |5.6.7.8  3128
      """.stripMargin

    test(parser, input)
  }

  "Nmap parser" should "parse nmap log" in {
    val parser = new ProxyNmapParser
    val input =
      """
        |Nmap scan report for 1.2.3.4
        |Host is up (0.35s latency).
        |Not shown: 10 closed ports, 1 filtered port
        |PORT     STATE SERVICE    VERSION
        |8080/tcp open  http-proxy Open HTTP Proxy
        |
        |Nmap scan report for 5.6.7.8
        |Host is up (0.35s latency).
        |Not shown: 10 closed ports, 1 filtered port
        |PORT     STATE SERVICE
        |3128/tcp open  http-proxy
        ||  proxy-open-http: Potentially OPEN proxy.
        ||_ Methods successfully tested: GET HEAD CONNECT
      """.stripMargin

    test(parser, input)
  }
}
