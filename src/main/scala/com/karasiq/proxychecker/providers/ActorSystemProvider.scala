package com.karasiq.proxychecker.providers

import akka.actor.ActorSystem

trait ActorSystemProvider {
  def actorSystem: ActorSystem
}
