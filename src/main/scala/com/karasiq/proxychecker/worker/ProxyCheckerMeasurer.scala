package com.karasiq.proxychecker.worker

import java.net.URI

import scala.concurrent.duration._
import scala.util.control
import scalaj.http.{Http, HttpOptions}

trait ProxyCheckerMeasurer {
  def apply(proxy: Proxy): Iterator[MeasuredProxy]
}

final private class ProxyCheckerMeasurerImpl(testPageUrl: String, testPageCheckString: String,
                                            httpsTestPageUrl: String, httpsTestPageCheckString: String,
                                            connectTimeout: FiniteDuration, readTimeout: FiniteDuration) extends ProxyCheckerMeasurer {

  private def checkFor(proxy: URI): (String, String) = {
    if (proxy.getScheme == "https") (httpsTestPageUrl, httpsTestPageCheckString)
    else (testPageUrl, testPageCheckString)
  }

  private def elapsedFrom(start: Long) = {
    (System.nanoTime() - start).nanos
  }

  private def tryLoadPage(proxy: URI, url: String): Option[String] = {
    import java.net.Proxy.Type._
    control.Exception.nonFatalCatch.opt {
      Http(url)
        .options(HttpOptions.connTimeout(connectTimeout.toMillis.toInt), HttpOptions.readTimeout(readTimeout.toMillis.toInt), HttpOptions.allowUnsafeSSL)
        .proxy(proxy.getHost, proxy.getPort, if (proxy.getScheme == "socks") SOCKS else HTTP)
        .asString
    }
  }

  private def check(proxy: URI): Option[FiniteDuration] = {
    val start: Long = System.nanoTime()
    checkFor(proxy) match {
      case (url, subString) ⇒
        tryLoadPage(proxy, url) match {
          case Some(page) if page.contains(subString) ⇒
            Some(elapsedFrom(start))

          case _ ⇒
            None // Checking error
        }

      case _ ⇒ throw new IllegalArgumentException("No settings for proxy: " + proxy)
    }
  }

  override def apply(proxy: Proxy): Iterator[MeasuredProxy] = {
    proxy.protocols.toIterator.flatMap { proto ⇒
      val uri = new URI(s"$proto://${proxy.address}")
      val result = check(uri)
      result.map(r ⇒ MeasuredProxy(proxy.address, proto, r))
    }
  }
}

object ProxyCheckerMeasurer {
  def apply(testPageUrl: String, testPageCheckString: String, httpsTestPageUrl: String, httpsTestPageCheckString: String, connectTimeout: FiniteDuration = 10.seconds, readTimeout: FiniteDuration = 5.second): ProxyCheckerMeasurer = new ProxyCheckerMeasurerImpl(testPageUrl, testPageCheckString, httpsTestPageUrl, httpsTestPageCheckString, connectTimeout, readTimeout)
}

