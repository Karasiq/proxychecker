package com.karasiq.proxychecker.providers

import akka.actor.ActorRef

/**
 * Provider for proxy checker actor
 */
trait ProxyCheckerMeasurerActorProvider {
  def proxyCheckerMeasurerActor: ActorRef
}
