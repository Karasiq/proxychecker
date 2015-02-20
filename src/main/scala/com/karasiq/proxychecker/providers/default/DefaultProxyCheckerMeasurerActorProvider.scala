package com.karasiq.proxychecker.providers.default

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Props}
import akka.contrib.throttle.{Throttler, TimerBasedThrottler}
import com.karasiq.proxychecker.providers.{ActorSystemProvider, ProxyCheckerMeasurerActorProvider, ProxyCheckerMeasurerEventBusProvider}
import com.karasiq.proxychecker.worker.{MeasuredProxy, Proxy, ProxyCheckerMeasurer, ProxyCheckerMeasurerActor}
import com.typesafe.config.ConfigFactory
import retry.Success

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.ref.WeakReference

// Retry wrapper
trait WithRetry extends ProxyCheckerMeasurerActor {
  private val cfg = WeakReference(ConfigFactory.load().getConfig("proxyChecker"))
  private val retryCount = cfg().getInt("maxRetries")
  private val retryDelay = cfg().getDuration("retryDelay", TimeUnit.SECONDS).seconds

  override abstract def checkProxy(proxy: Proxy): Future[Iterator[MeasuredProxy]] = {
    import odelay.Timer.{default => timer}
    implicit val success = Success[Iterator[MeasuredProxy]](_.nonEmpty)
    retry.Pause(retryCount, retryDelay)(timer) { () ⇒
      super.checkProxy(proxy)
    }
  }
}

trait DefaultProxyCheckerMeasurerActorProvider extends ProxyCheckerMeasurerActorProvider {
  self: ActorSystemProvider with ProxyCheckerMeasurerEventBusProvider ⇒
  private val proxyCheckerMeasurer: ProxyCheckerMeasurer = {
    val cfg = ConfigFactory.load().getConfig("proxyChecker")

    ProxyCheckerMeasurer(cfg.getString("checkUrl"), cfg.getString("checkSubString"), cfg.getDuration("connectTimeout", TimeUnit.MILLISECONDS).millis, cfg.getDuration("readTimeout", TimeUnit.MILLISECONDS).millis)
  }

  private def props = Props(new ProxyCheckerMeasurerActor(proxyCheckerMeasurer, proxyCheckerMeasurerEventBus) with WithRetry)

  override final val proxyCheckerMeasurerActor: ActorRef = {
    val freq = ConfigFactory.load().getInt("proxyChecker.frequency")
    val actor = actorSystem.actorOf(props, "checker")
    val throttler = actorSystem.actorOf(Props(classOf[TimerBasedThrottler], Throttler.Rate(freq, 1 seconds)))
    throttler ! Throttler.SetTarget(Some(actor))
    throttler
  }
}
