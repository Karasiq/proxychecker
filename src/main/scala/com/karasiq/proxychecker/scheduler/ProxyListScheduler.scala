package com.karasiq.proxychecker.scheduler

import akka.actor.{ActorSystem, Cancellable}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.Exception


object ProxyListScheduler {
  def apply(interval: FiniteDuration)(sourceLoad: (String, Set[String]) ⇒ Unit)(implicit ac: ActorSystem): ProxyListScheduler = {
    new ProxyListSchedulerImpl(ac, interval, sourceLoad)
  }
}

trait ProxyListScheduler {
  def schedule(listName: String, sources: Set[String]): Unit
  def cancel(listName: String): Unit
}


private class ProxyListSchedulerImpl(ac: ActorSystem, interval: FiniteDuration, sourceLoad: (String, Set[String]) ⇒ Unit) extends ProxyListScheduler {
  private implicit val ec = ac.dispatcher

  private val map = collection.concurrent.TrieMap.empty[String, Cancellable]

  override def schedule(listName: String, sources: Set[String]): Unit = {
    cancel(listName)
    if (sources.nonEmpty) Exception.ignoring(classOf[IllegalStateException]) {
      val sc = ac.scheduler.schedule(0 nanos, interval) {
        sourceLoad(listName, sources)
      }
      map.put(listName, sc)
    }
  }

  override def cancel(listName: String): Unit = {
    map.remove(listName).foreach(_.cancel())
  }
}
