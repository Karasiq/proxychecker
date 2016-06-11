package com.karasiq.proxychecker.worker

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

case class ProxyCheckRequest(address: String, protocols: Set[String] = Set("http", "https", "socks")) {
  assert(protocols.nonEmpty, "Invalid protocols")

  override def toString: String = s"ProxyCheckRequest($address)"
}

case class MeasuredProxy(address: String, protocol: String, time: FiniteDuration) {
  override def toString: String = s"MeasuredProxy($protocol://$address, ${time.toMillis} ms)"
}

object ProxyCheckerMeasurerActor {
  def props(checker: ProxyCheckerMeasurer, eventBus: ProxyCheckerMeasurerEventBus) = {
    Props(classOf[ProxyCheckerMeasurerActor], checker, eventBus)
  }
}

class ProxyCheckerMeasurerActor(checker: ProxyCheckerMeasurer, eventBus: ProxyCheckerMeasurerEventBus) extends Actor with ActorLogging {
  private implicit val actorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  def postCheck(result: MeasuredProxy): Unit = {
    log.debug("Proxy check result: {}", result)
    eventBus.publish(result)
  }

  def checkProxy(proxy: ProxyCheckRequest): Source[MeasuredProxy, NotUsed] = {
    log.debug("Checking proxy: {}", proxy)
    checker(proxy)
  }

  override final def receive: Receive = {
    case proxy: ProxyCheckRequest ⇒
      checkProxy(proxy).runForeach(postCheck)
  }
}

