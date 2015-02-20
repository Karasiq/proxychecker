package com.karasiq.proxychecker.test.providers

import java.net.InetAddress

import com.karasiq.geoip.{GeoipResolver, GeoipResult}
import com.karasiq.proxychecker.providers.GeoipResolverProvider

trait TestGeoipResolverProvider extends GeoipResolverProvider {
   override final val geoipResolver: GeoipResolver = new GeoipResolver {
     override def apply(ip: InetAddress): GeoipResult = GeoipResult("TE", "Test Country", "Test City")
   }
 }
