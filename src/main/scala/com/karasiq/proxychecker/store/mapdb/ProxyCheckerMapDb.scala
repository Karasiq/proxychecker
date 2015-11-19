package com.karasiq.proxychecker.store.mapdb

import java.io.Closeable
import java.nio.file.{Path, Paths}

import com.karasiq.mapdb.{MapDbFile, MapDbSingleFileProducer}
import com.typesafe.config.ConfigFactory
import org.mapdb.DBMaker.Maker

private[proxychecker] object ProxyCheckerMapDb extends Closeable {
  private def mapDbFilePath: Path = Paths.get(ConfigFactory.load().getString("proxyChecker.mapDb.path"))

  private object ProxyStoreDbProducer extends MapDbSingleFileProducer(mapDbFilePath) {
    override protected def setSettings(dbMaker: Maker): Maker = {
      dbMaker
        .transactionDisable()
        .executorEnable()
        .cacheSoftRefEnable()
        .asyncWriteEnable()
        .asyncWriteFlushDelay(3000)
    }
  }

  def apply(): MapDbFile = {
    ProxyStoreDbProducer()
  }

  def close(): Unit = {
    ProxyStoreDbProducer.close()
  }
}
