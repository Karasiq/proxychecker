package com.karasiq.proxychecker.providers

import akka.actor.ActorRef

trait UpdateWatcherProvider {
  def updateWatcher: ActorRef
}
