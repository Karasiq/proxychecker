package com.karasiq.proxychecker.worker

import java.util.concurrent.Executors

import akka.actor.{Actor, ActorLogging, Props}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

case class Proxy(address: String, protocols: Set[String] = Set("http", "https", "socks")) {
  assert(protocols.nonEmpty, "Invalid protocols")

  override def toString: String = s"Proxy($address)"
}

case class MeasuredProxy(address: String, protocol: String, time: FiniteDuration) {
  override def toString: String = s"MeasuredProxy($protocol://$address, ${time.toMillis} ms)"
}

object ProxyCheckerMeasurerActor {
  def props(checker: ProxyCheckerMeasurer, eventBus: ProxyCheckerMeasurerEventBus) =
    Props(classOf[ProxyCheckerMeasurerActor], checker, eventBus)
}

class ProxyCheckerMeasurerActor(checker: ProxyCheckerMeasurer, eventBus: ProxyCheckerMeasurerEventBus) extends Actor with ActorLogging {
  protected implicit val executionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool()) // Checker pool

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    executionContext.shutdown()
    super.postStop()
  }

  def postCheck(r: MeasuredProxy): Unit = {
    log.debug("Proxy check result: {}", r)
    eventBus.publish(r)
  }

  def checkProxy(proxy: Proxy): Future[Iterator[MeasuredProxy]] = Future {
    log.debug("Checking proxy: {}", proxy)
    checker(proxy)
  }

  override final def receive: Receive = {
    case proxy: Proxy ⇒
      checkProxy(proxy).onSuccess {
        case results ⇒
          results.foreach(postCheck)
      }
  }
}

