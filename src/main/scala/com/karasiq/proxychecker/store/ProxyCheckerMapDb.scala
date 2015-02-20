package com.karasiq.proxychecker.store

import java.nio.file.Paths

import com.pidorashque.mapdb.{MapDbFile, MapDbFileProducer}
import com.typesafe.config.ConfigFactory
import org.mapdb.DBMaker

private[proxychecker] object ProxyCheckerMapDb {
  private object ProxyStoreDbProducer extends MapDbFileProducer {
    override protected def setSettings[T <: DBMaker[T]](dbMaker: DBMaker[T]): T = {
      dbMaker
        .mmapFileEnableIfSupported()
        .compressionEnable()
    }
  }

  private lazy val mapDbFilePath: String = ConfigFactory.load().getString("proxyChecker.mapDb.path")

  @inline
  def openDatabase(): MapDbFile = ProxyStoreDbProducer(Paths.get(mapDbFilePath))

  @inline
  def close(): Unit = {
    ProxyStoreDbProducer.close()
  }
}
