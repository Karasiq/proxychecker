package com.karasiq.proxychecker.providers.default

import com.karasiq.proxychecker.providers.ProxyCheckerServicesProvider

/**
  * Default components provider for ProxyCheckerService
  */
trait DefaultProxyCheckerServicesProvider extends ProxyCheckerServicesProvider with DefaultActorSystemProvider with DefaultProxyStoreProvider with DefaultProxyCheckerMeasurerEventBusProvider with DefaultProxyCheckerMeasurerActorProvider with DefaultGeoipResolverProvider with DefaultProxyListParserProvider with DefaultWebServiceCacheProvider with DefaultUpdateWatcherProvider with DefaultProxyListSchedulerProvider
