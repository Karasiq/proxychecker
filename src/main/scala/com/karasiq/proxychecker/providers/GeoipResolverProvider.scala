package com.karasiq.proxychecker.providers

import com.karasiq.geoip.GeoipResolver

trait GeoipResolverProvider {
  def geoipResolver: GeoipResolver
}
