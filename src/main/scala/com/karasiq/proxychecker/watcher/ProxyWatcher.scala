package com.karasiq.proxychecker.watcher

import akka.actor.Actor
import com.karasiq.proxychecker.worker.MeasuredProxy

trait ProxyWatcher extends Actor {
  def onResultReceive(r: MeasuredProxy): Unit = ()

  override final def receive: Receive = {
    case r: MeasuredProxy ⇒
      onResultReceive(r)
  }
}