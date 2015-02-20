package com.karasiq.proxychecker.webservice

import akka.actor._
import akka.io.IO
import akka.kernel.Bootable
import com.karasiq.proxychecker.providers.default.DefaultProxyCheckerServicesProvider
import com.karasiq.proxychecker.store.ProxyCheckerMapDb
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.can.server.UHttp

import scala.concurrent.duration._
import scala.language.postfixOps

class ProxyCheckerBoot extends Bootable with DefaultProxyCheckerServicesProvider with ProxyCheckerWebServiceProvider  {
  private def startWebService(): Unit = {
    val cfg = ConfigFactory.load().getConfig("proxyChecker.webService")
    val server = actorSystem.actorOf(ProxyCheckerService.props(), "webService")
    IO(UHttp)(actorSystem) ! Http.Bind(server, cfg.getString("host"), cfg.getInt("port"))
  }

  override def startup(): Unit = {
    startWebService()
  }

  override def shutdown(): Unit = {
    actorSystem.shutdown()
    actorSystem.awaitTermination(5 minutes)
    ProxyCheckerMapDb.close()
  }
}
