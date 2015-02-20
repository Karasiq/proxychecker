package com.karasiq.proxychecker.providers.default

import com.karasiq.geoip.GeoipResolver
import com.karasiq.proxychecker.providers.GeoipResolverProvider
import com.typesafe.config.ConfigFactory

trait DefaultGeoipResolverProvider extends GeoipResolverProvider {
  private val (dbType, dbPath) = {
    val cfg = ConfigFactory.load().getConfig("geoip")
    (cfg.getString("type"), cfg.getString("path"))
  }

  override final val geoipResolver: GeoipResolver = dbType match {
    case "auto" ⇒
      GeoipResolver.read(dbPath)

    case theType ⇒
      GeoipResolver.read(GeoipResolver.typeOf(theType), dbPath)
  }
}
