package com.karasiq.proxychecker.worker

import akka.actor.ActorRef
import akka.event.{EventBus, ScanningClassification}

trait ProxyCheckerMeasurerEventBus extends EventBus {
  override final type Event = MeasuredProxy
  override final type Classifier = Null
  override final type Subscriber = ActorRef
}

final class ProxyCheckerMeasurerEventBusImpl extends ProxyCheckerMeasurerEventBus with ScanningClassification {
  override protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event
  }

  override protected def compareClassifiers(a: Classifier, b: Classifier): Int = 0

  override protected def matches(classifier: Classifier, event: Event): Boolean = true

  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.compareTo(b)
}

object ProxyCheckerMeasurerEventBus {
  def apply(): ProxyCheckerMeasurerEventBus = new ProxyCheckerMeasurerEventBusImpl
}
