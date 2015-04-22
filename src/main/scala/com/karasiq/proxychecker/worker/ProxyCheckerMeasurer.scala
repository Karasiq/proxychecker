package com.karasiq.proxychecker.worker

import java.io.IOException
import java.net.URI

import scala.concurrent.duration._
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

  private def check(proxy: URI): Option[FiniteDuration] = {
    import java.net.Proxy.Type._

    import scala.util.control.Exception.catching
    val start = System.nanoTime()
    checkFor(proxy) match {
      case (url, subString) ⇒
        catching(classOf[IOException], classOf[scalaj.http.HttpException], classOf[AssertionError]).opt {
          val page = Http(url)
            .options(HttpOptions.connTimeout(connectTimeout.toMillis.toInt), HttpOptions.readTimeout(readTimeout.toMillis.toInt), HttpOptions.allowUnsafeSSL)
            .proxy(proxy.getHost, proxy.getPort, if (proxy.getScheme == "socks") SOCKS else HTTP)
          assert(page.asString.contains(subString))
        } map (_ ⇒ (System.nanoTime() - start).nanos)

      case _ ⇒
        None
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

