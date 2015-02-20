package com.karasiq.proxychecker.test.providers

import com.karasiq.proxychecker.providers.{ActorSystemProvider, ProxyListSchedulerProvider}
import com.karasiq.proxychecker.scheduler.ProxyListScheduler

trait TestProxyListSchedulerProvider extends ProxyListSchedulerProvider { self: ActorSystemProvider ⇒
  override def proxyListScheduler: ProxyListScheduler = new ProxyListScheduler {
    override def schedule(listName: String, sources: Set[String]): Unit = {
      actorSystem.log.info(s"Test schedule: $sources → $listName")
    }

    override def cancel(listName: String): Unit = {
      actorSystem.log.info(s"Test schedule cancelled: $listName")
    }
  }
}
