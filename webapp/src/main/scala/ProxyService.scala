import com.greencatsoft.angularjs._
import com.greencatsoft.angularjs.core.HttpService
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.{JSExport, JSExportAll}


@JSExportAll
case class GeoipResult(var code: String = "", var country: String = "", var city: String = "")

@JSExportAll
case class ProxyServiceEntry(var address: String, var protocol: String, var alive: Boolean, var latency: Int, var lastCheck: String, var geoip: GeoipResult)

@injectable("proxyService")
class ProxyService(http: HttpService) extends Service {
  require(http != null, "Missing argument 'http'.")

  import scala.scalajs.js.URIUtils.encodeURIComponent

  @JSExport
  def deleteAll(alive: Option[Boolean] = None, country: Option[String] = None, newerThan: Option[Int] = None, olderThan: Option[Int] = None): Future[js.Any] = {
    val query = Seq(alive.map("alive=" + _), country.map("country=" + encodeURIComponent(_)), newerThan.map("newerThan=" + _), olderThan.map("olderThan=" + _))
      .flatten.mkString("&")

    http.delete[js.Any]("/proxy?" + query)
  }

  @JSExport
  def createList(name: String, inMemory: Boolean): Future[js.Any] = {
    http.put[js.Any]("/list?list=" + encodeURIComponent(name) + "&inmemory=" + inMemory)
  }

  @JSExport
  def deleteList(name: String): Future[js.Any] = {
    http.delete[js.Any]("/list?list=" + encodeURIComponent(name))
  }

  @JSExport
  def getProxyListsNames: Future[Set[String]] = {
    val future = http.get[js.Any]("/lists.json")
    future
      .map(JSON.stringify(_))
      .map(read[Set[String]])
  }

  @JSExport
  def setProxyListSources(list: String, sources: String): Future[js.Any] = {
    assert(list.nonEmpty, "Invalid list name")
    http.post[js.Any](s"/sources?list=$list", sources)
  }

  @JSExport
  def getProxyListSources(list: String): Future[Set[String]] = {
    val future = http.get[js.Any](s"/sources.json?list=$list")
    future
      .map(JSON.stringify(_))
      .map(read[Set[String]])
  }

  @JSExport
  def getProxyList: Future[Seq[ProxyServiceEntry]] = {
    val future = http.get[js.Any]("/proxylist.json")
    future
      .map(JSON.stringify(_))
      .map(read[Seq[ProxyServiceEntry]])
  }

  @JSExport
  def rescanAll(): Future[js.Any] = {
    http.post[js.Any]("/rescan")
  }

  @JSExport
  def rescanDead(): Future[js.Any] = {
    http.post[js.Any]("/rescan?alive=false&olderThan=30")
  }

  @JSExport
  def rescanProxy(address: String): Future[js.Any] = {
    http.post[js.Any]("/rescan?address=" + encodeURIComponent(address))
  }

  @JSExport
  def addProxies(text: String, temp: Boolean, list: Option[String] = None): Future[js.Any] = {
    http.post[js.Any](s"/proxylist?temp=$temp" + list.fold("")("&list=" + _), text)
  }

  @JSExport
  def removeProxy(address: String): Future[js.Any] = {
    http.delete[js.Any]("/proxy?address=" + encodeURIComponent(address))
  }
}

@injectable("proxyService")
class ProxyServiceFactory extends Factory[ProxyService] {
  @inject
  var http: HttpService = _

  override def apply(): ProxyService = new ProxyService(http)
}
