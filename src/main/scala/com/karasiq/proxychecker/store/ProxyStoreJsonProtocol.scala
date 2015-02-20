package com.karasiq.proxychecker.store

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import java.util.Locale
import java.util.concurrent.TimeUnit

import com.karasiq.geoip.GeoipResult
import spray.json._

import scala.concurrent.duration.Duration

object ProxyStoreJsonProtocol extends DefaultJsonProtocol {
  implicit def geoipResultFormat: JsonFormat[GeoipResult] = jsonFormat(GeoipResult.apply, "code", "country", "city", "organization")

  implicit object InstantFormat extends JsonFormat[Instant] {
    private val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy EE HH:mm:ss", Locale.getDefault)
      .withZone(ZoneId.systemDefault())

    override def write(obj: Instant): JsValue = {
      JsString(formatter.format(obj))
    }

    override def read(json: JsValue): Instant = {
      Instant.from(formatter.parse(json.convertTo[String]))
    }
  }

  implicit object ProxyStoreFormat extends JsonFormat[ProxyStoreEntry] {
    @inline
    private def latencyFrom(alive: Boolean, latency: BigDecimal) = {
      if (alive) Duration(latency.longValue(), TimeUnit.MILLISECONDS) else Duration.Inf
    }

    override def write(obj: ProxyStoreEntry): JsValue = {
      JsObject(
        "address" → JsString(obj.address),
        "protocol" → JsString(obj.protocol),
        "alive" → JsBoolean(obj.isAlive),
        "latency" → JsNumber(if (obj.speed.isFinite()) obj.speed.toMillis else -1),
        "lastCheck" → obj.lastCheck.toJson,
        "geoip" → obj.geoip.toJson
      )
    }

    override def read(json: JsValue): ProxyStoreEntry = json.asJsObject.getFields("address", "protocol", "alive", "latency", "lastCheck", "geoip") match {
      case Seq(JsString(address), JsString(protocol), JsBoolean(alive), JsNumber(latency), lastCheck: JsValue, geoip: JsValue) ⇒
        ProxyStoreEntry(address, protocol, latencyFrom(alive, latency), geoip.convertTo[GeoipResult], lastCheck.convertTo[Instant])

      case _ ⇒
        throw new DeserializationException("ProxyStoreEntry expected")
    }
  }
}
