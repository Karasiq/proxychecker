package com.karasiq.proxychecker.providers

import com.karasiq.proxychecker.scheduler.ProxyListScheduler

trait ProxyListSchedulerProvider {
  def proxyListScheduler: ProxyListScheduler
}
