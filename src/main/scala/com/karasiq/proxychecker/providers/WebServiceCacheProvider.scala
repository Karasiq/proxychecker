package com.karasiq.proxychecker.providers

import com.karasiq.proxychecker.webservice.WebServiceCache

trait WebServiceCacheProvider {
  def webServiceCache: WebServiceCache
}
