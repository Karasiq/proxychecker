package com.karasiq.proxychecker.test.providers

import akka.actor.Props
import com.karasiq.proxychecker.providers.{ActorSystemProvider, ProxyCheckerMeasurerEventBusProvider, UpdateWatcherProvider}
import com.karasiq.proxychecker.watcher.ProxyWatcher
import com.karasiq.proxychecker.worker.MeasuredProxy

trait TestUpdateWatcherProvider extends UpdateWatcherProvider { self: ActorSystemProvider with ProxyCheckerMeasurerEventBusProvider ⇒
  override final val updateWatcher = actorSystem.actorOf(Props(new ProxyWatcher {
    override def onResultReceive(r: MeasuredProxy): Unit = {
      actorSystem.log.info("Update received: {}", r)
    }
  }), "testUpdateWatcher")

  proxyCheckerMeasurerEventBus.subscribe(updateWatcher, null)
}
