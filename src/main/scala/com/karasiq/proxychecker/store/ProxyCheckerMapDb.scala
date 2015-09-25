package com.karasiq.proxychecker.store

import java.nio.file.Paths

import com.karasiq.mapdb.{MapDbFile, MapDbFileProducer}
import com.typesafe.config.ConfigFactory
import org.mapdb.DBMaker.Maker

private[proxychecker] object ProxyCheckerMapDb {
  private object ProxyStoreDbProducer extends MapDbFileProducer {
    override protected def setSettings(dbMaker: Maker): Maker = {
      dbMaker
        .fileMmapEnableIfSupported()
        .compressionEnable()
    }
  }

  private val mapDbFilePath: String = ConfigFactory.load().getString("proxyChecker.mapDb.path")

  @inline
  def openDatabase(): MapDbFile = ProxyStoreDbProducer(Paths.get(mapDbFilePath))

  @inline
  def close(): Unit = {
    ProxyStoreDbProducer.close()
  }
}
