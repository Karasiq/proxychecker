package com.karasiq.proxychecker.providers.default

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.{ActorRef, Props}
import akka.contrib.throttle.{Throttler, TimerBasedThrottler}
import akka.stream.scaladsl.Source
import com.karasiq.proxychecker.providers.{ActorSystemProvider, ProxyCheckerMeasurerActorProvider, ProxyCheckerMeasurerEventBusProvider}
import com.karasiq.proxychecker.worker.{MeasuredProxy, ProxyCheckRequest, ProxyCheckerMeasurer, ProxyCheckerMeasurerActor}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps

// Retry wrapper
trait WithRetry extends ProxyCheckerMeasurerActor {
  private def cfg = ConfigFactory.load().getConfig("proxyChecker")
  private val retryCount = cfg.getInt("maxRetries")

  override abstract def checkProxy(proxy: ProxyCheckRequest): Source[MeasuredProxy, NotUsed] = {
    super.checkProxy(proxy)
      .recoverWithRetries(retryCount, { case _ ⇒ super.checkProxy(proxy) })
  }
}

trait DefaultProxyCheckerMeasurerActorProvider extends ProxyCheckerMeasurerActorProvider {
  self: ActorSystemProvider with ProxyCheckerMeasurerEventBusProvider ⇒
  private val proxyCheckerMeasurer: ProxyCheckerMeasurer = {
    val cfg = ConfigFactory.load().getConfig("proxyChecker")
    ProxyCheckerMeasurer(cfg.getString("checkUrl"), cfg.getString("checkSubString"), cfg.getDuration("connectTimeout", TimeUnit.MILLISECONDS).millis, cfg.getDuration("readTimeout", TimeUnit.MILLISECONDS).millis)(actorSystem.dispatcher, actorSystem, actorMaterializer)
  }

  private def props = Props(new ProxyCheckerMeasurerActor(proxyCheckerMeasurer, proxyCheckerMeasurerEventBus) with WithRetry)

  override final val proxyCheckerMeasurerActor: ActorRef = {
    val frequency = ConfigFactory.load().getInt("proxyChecker.frequency")
    val actor = actorSystem.actorOf(props, "checker")
    val throttler = actorSystem.actorOf(Props(classOf[TimerBasedThrottler], Throttler.Rate(frequency, 1 seconds)))
    throttler ! Throttler.SetTarget(Some(actor))
    throttler
  }
}
