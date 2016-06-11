package com.karasiq.proxychecker.providers

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.karasiq.common.Lazy

trait ActorSystemProvider {
  def actorSystem: ActorSystem

  private val _actorMaterializer = Lazy(ActorMaterializer(ActorMaterializerSettings(actorSystem))(actorSystem))
  final implicit def actorMaterializer = _actorMaterializer()
}
