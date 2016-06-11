package com.karasiq.proxychecker.worker

import java.net.{InetSocketAddress, URI}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.io.Tcp.SO
import akka.stream.scaladsl.{Keep, Sink, Source, Tcp}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.ByteString
import com.karasiq.networkutils.http.headers.Host
import com.karasiq.networkutils.proxy.Proxy
import com.karasiq.parsers.http.{HttpMethod, HttpRequest}
import com.karasiq.proxy.ProxyChain

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait ProxyCheckerMeasurer {
  def apply(proxy: ProxyCheckRequest): Source[MeasuredProxy, NotUsed]
}

final private class ProxyCheckerMeasurerImpl(testPageUrl: String, testPageCheckString: String,
                                            connectTimeout: FiniteDuration, readTimeout: FiniteDuration)(implicit ec: ExecutionContext, as: ActorSystem, am: ActorMaterializer) extends ProxyCheckerMeasurer {

  private def httpRequest(proxyUri: URI, url: String): Future[ByteString] = {
    import com.karasiq.networkutils.uri._
    val proxy = Proxy(proxyUri)
    val (host, port, path) = {
      val uri = new URI(url)
      (uri.getHost, Option(uri.getPort).filter(_ > 0).getOrElse(80), Option(uri.getPath).filter(_.nonEmpty).getOrElse("/"))
    }

    val (connection, request) = if (proxy.scheme.toLowerCase == "http") {
      val outgoingConnection = Tcp().outgoingConnection(proxy.toInetSocketAddress, None, List(SO.TcpNoDelay(true), SO.KeepAlive(true)), halfClose = true, connectTimeout, readTimeout)
      (outgoingConnection, HttpRequest((HttpMethod.GET, url, Seq(Host(host)))))
    } else {
      (ProxyChain.connect(InetSocketAddress.createUnresolved(host, port), Seq(proxy)), HttpRequest((HttpMethod.GET, path, Seq(Host(host)))))
    }

    val (queue, future) = Source.queue(1, OverflowStrategy.fail)
      .viaMat(connection)(Keep.left)
      .toMat(Sink.head)(Keep.both)
      .run()

    queue.offer(request)
    future.onComplete(_ ⇒ queue.complete())
    future
  }

  private def check(proxy: URI): Future[FiniteDuration] = {
    val start: Long = System.nanoTime()
    httpRequest(proxy, testPageUrl).map { data ⇒
      assert(data.utf8String.contains(testPageCheckString), "Invalid response received")
      (System.nanoTime() - start).nanos
    }
  }

  def apply(proxy: ProxyCheckRequest): Source[MeasuredProxy, NotUsed] = {
    Source(proxy.protocols.toVector)
      .flatMapConcat { proto ⇒
        val uri = new URI(s"$proto://${proxy.address}")
        val result = check(uri)
        Source
          .fromFuture(result)
          .map(time ⇒ MeasuredProxy(proxy.address, proto, time))
          .recoverWithRetries(1, { case _ ⇒ Source.empty })
      }
  }
}

object ProxyCheckerMeasurer {
  def apply(testPageUrl: String, testPageCheckString: String, connectTimeout: FiniteDuration = 10.seconds, readTimeout: FiniteDuration = 5.second)(implicit ec: ExecutionContext, as: ActorSystem, am: ActorMaterializer): ProxyCheckerMeasurer = {
    new ProxyCheckerMeasurerImpl(testPageUrl, testPageCheckString, connectTimeout, readTimeout)
  }
}

