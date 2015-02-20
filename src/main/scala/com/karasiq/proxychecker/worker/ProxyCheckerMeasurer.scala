package com.karasiq.proxychecker.worker

import java.io.IOException
import java.net.URI
import javax.net.ssl.SSLException

import scala.concurrent.duration._
import scalaj.http.{Http, HttpOptions}

trait ProxyCheckerMeasurer {
  def apply(proxy: Proxy): Iterator[MeasuredProxy]
}

final private class ProxyCheckerMeasurerImpl(testPageUrl: String, testPageCheckString: String, connectTimeout: FiniteDuration, readTimeout: FiniteDuration) extends ProxyCheckerMeasurer {
  private def httpUrlOf(url: String): String = {
    new URI("https", null, new URI(url).getHost, 80, null, null, null).toString
  }

  private def check(proxy: URI): Option[FiniteDuration] = {
    import java.net.Proxy.Type._

    import scala.util.control.Exception.{catching, ignoring}
    val start = System.nanoTime()
    // TODO: safer https check
    catching(classOf[IOException], classOf[scalaj.http.HttpException], classOf[AssertionError]).opt {
      val page = Http(if (proxy.getScheme == "https") httpUrlOf(testPageUrl) else testPageUrl)
        .options(HttpOptions.connTimeout(connectTimeout.toMillis.toInt), HttpOptions.readTimeout(readTimeout.toMillis.toInt))
        .proxy(proxy.getHost, proxy.getPort, if (proxy.getScheme == "socks") SOCKS else HTTP)
      ignoring(classOf[SSLException], classOf[NoSuchElementException])(assert(page.asString.contains(testPageCheckString)))
    } map (_ ⇒ (System.nanoTime() - start).nanos)
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
  def apply(testPageUrl: String, testPageCheckString: String, connectTimeout: FiniteDuration = 10.seconds, readTimeout: FiniteDuration = 5.second): ProxyCheckerMeasurer = new ProxyCheckerMeasurerImpl(testPageUrl, testPageCheckString, connectTimeout, readTimeout)
}

