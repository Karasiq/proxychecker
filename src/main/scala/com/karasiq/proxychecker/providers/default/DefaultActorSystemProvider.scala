package com.karasiq.proxychecker.providers.default

import akka.actor.ActorSystem
import com.karasiq.proxychecker.providers.ActorSystemProvider

trait DefaultActorSystemProvider extends ActorSystemProvider {
  override final val actorSystem: ActorSystem = ActorSystem("proxyChecker")
}
