package com.karasiq.proxychecker.providers

import com.karasiq.proxychecker.worker.ProxyCheckerMeasurerEventBus

trait ProxyCheckerMeasurerEventBusProvider {
  def proxyCheckerMeasurerEventBus: ProxyCheckerMeasurerEventBus
}
