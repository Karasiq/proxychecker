package com.karasiq.geoip

import java.io.{Closeable, File}
import java.net.InetAddress
import java.nio.file.Paths
import java.util.Locale

import com.maxmind.geoip2.exception.AddressNotFoundException
import com.maxmind.geoip2.model._
import com.maxmind.geoip2.record.{City, Country}
import com.maxmind.geoip2.{DatabaseReader, GeoIp2Provider}

import scala.language.implicitConversions
import scala.util.control.Exception

case class GeoipResult(code: String = "", country: String = "", city: String = "", organization: String = "")

object GeoipResult {
  private def countryFrom(r: AbstractResponse): Option[Country] = r match {
    case c: CountryResponse if c.getCountry.getName != null ⇒
      Some(c.getCountry)

    case c: CityResponse if c.getCountry.getName != null ⇒
      Some(c.getCountry)

    case c: InsightsResponse if c.getCountry.getName != null ⇒
      Some(c.getCountry)

    case _ ⇒
      None
  }

  private def cityFrom(r: AbstractResponse): Option[City] = r match {
    case c: CityResponse if c.getCity.getName != null ⇒
      Some(c.getCity)

    case c: InsightsResponse if c.getCity.getName != null ⇒
      Some(c.getCity)

    case _ ⇒
      None
  }

  private def organizationFrom(r: AbstractResponse): Option[String] = r match {
    case c: IspResponse if c.getOrganization != null ⇒
      Some(c.getOrganization)

    case _ ⇒
      None
  }

  def apply(r: AbstractResponse): GeoipResult = {
    val country = countryFrom(r)
    val city = cityFrom(r)
    val organization = organizationFrom(r)
    GeoipResult(country.fold("")(_.getIsoCode), country.fold("")(_.getName), city.fold("")(_.getName), organization.getOrElse(""))
  }
}

object GeoipResolver {
  implicit def stringToInetAddress(s: String): InetAddress = InetAddress.getByName(s)

  private def apply(provider: InetAddress ⇒ AbstractResponse)(ip: InetAddress): GeoipResult = {
    Exception.catching(classOf[AddressNotFoundException]).either(provider(ip)).fold(_ ⇒ GeoipResult(), GeoipResult.apply)
  }

  def apply(db: GeoIp2Provider, dbType: Type): InetAddress ⇒ GeoipResult = dbType match {
    case City ⇒
      apply(ip ⇒ db.city(ip))

    case Country ⇒
      apply(ip ⇒ db.country(ip))

    case ISP if db.isInstanceOf[DatabaseReader] ⇒
      apply(ip ⇒ db.asInstanceOf[DatabaseReader].isp(ip))

    case _ ⇒
      (_: InetAddress) ⇒ GeoipResult()
  }

  sealed trait Type
  case object City extends Type
  case object Country extends Type
  case object ISP extends Type
  
  def typeOf(file: String): Type = {
    val name = Paths.get(file).getFileName.toString.toLowerCase // File name

    name match { // Type of db
      case f if f.contains("city") ⇒
        GeoipResolver.City

      case f if f.contains("isp") ⇒
        GeoipResolver.ISP

      case _ ⇒
        GeoipResolver.Country
    }
  }

  def read(f: String): GeoipResolver = new GeoipFileResolver(f, typeOf(f))

  def read(t: Type, f: String): GeoipResolver = new GeoipFileResolver(f, t)
}

trait GeoipResolver {
  def apply(ip: InetAddress): GeoipResult
}

private sealed class GeoipFileResolver(file: String, dbType: GeoipResolver.Type) extends GeoipResolver with Closeable {
  import scala.collection.JavaConversions._

  private def getSystemLocale: String = {
    Locale.getDefault.getLanguage
  }

  protected final val db = new DatabaseReader.Builder(new File(file)).locales(Seq(getSystemLocale, "en")).build()

  override def close(): Unit = db.close()

  override final def apply(ip: InetAddress): GeoipResult = GeoipResolver(db, dbType)(ip)
}
