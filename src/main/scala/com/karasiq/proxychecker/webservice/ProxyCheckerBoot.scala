package com.karasiq.proxychecker.webservice

import akka.actor._
import akka.io.IO
import com.karasiq.proxychecker.providers.default.DefaultProxyCheckerServicesProvider
import com.karasiq.proxychecker.store.mapdb.ProxyCheckerMapDb
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.can.server.UHttp

import scala.concurrent.duration._
import scala.language.postfixOps

object ProxyCheckerBoot extends App with DefaultProxyCheckerServicesProvider with ProxyCheckerWebServiceProvider  {
  def startup(): Unit = {
    val cfg = ConfigFactory.load().getConfig("proxyChecker.webService")
    val server = actorSystem.actorOf(ProxyCheckerService.props(), "webService")
    IO(UHttp)(actorSystem) ! Http.Bind(server, cfg.getString("host"), cfg.getInt("port"))
  }

  def shutdown(): Unit = {
    actorSystem.shutdown()
    actorSystem.awaitTermination(5 minutes)
    ProxyCheckerMapDb.close()
  }

  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    override def run(): Unit = shutdown()
  }))

  startup()
}
