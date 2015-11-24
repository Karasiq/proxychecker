import Notifications.Layout
import com.greencatsoft.angularjs._
import com.greencatsoft.angularjs.core._
import org.scalajs
import org.scalajs.dom.console
import org.scalajs.dom.raw.{CloseEvent, Event, MessageEvent, WebSocket}
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.Any.{fromBoolean, fromFunction1, fromString, wrapArray}
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import scala.scalajs.js.UndefOr.undefOr2ops
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.{URIUtils, UndefOr}
import scala.util.{Failure, Success}

@js.native
trait ProxyTableScope extends Scope {
  var location: Location = js.native

  // Model
  var proxies: js.Array[ProxyServiceEntry] = js.native

  var proxyLists: js.Array[String] = js.native

  var currentList: String = js.native

  // Filters
  var showCountry: String = js.native
  var countryFilter: js.Dynamic = js.native

  var statusFilter: js.Dynamic = js.native

  // Form data
  var countryInfo: GeoipResult = js.native

  var proxyAddTemp: Int = js.native
  var proxyText: String = js.native

  var newListName: String = js.native
  var newListInMemory: Boolean = js.native

  var modifyListName: String = js.native
  var modifyListSources: String = js.native
}

@JSExport
@injectable("proxyTableCtrl")
class ProxyTableController extends Controller[ProxyTableScope] {
  @inject
  var location: Location = _

  @inject
  var service: ProxyService = _

  @inject
  var scope: ProxyTableScope = _

  private def handleError(t: Throwable): Unit = {
    val text = s"Error has occurred: $t"
    Notifications.error(text, Layout.topRight, 3000)
    console.error(text)
  }

  private var webSocket: Option[WebSocket] = None

  private def openWebSocket(): Unit = {
    def processEvent(event: MessageEvent) = {
      val proxy = read[ProxyServiceEntry](event.data.toString)
      // console.info("WebSocket push: " + proxy)
      scope.$apply {
        val p = scope.proxies.find(_.address == proxy.address)
        if (p.nonEmpty) {
          p.get.alive = proxy.latency != -1
          p.get.latency = proxy.latency
          p.get.lastCheck = proxy.lastCheck
          p.get.protocol = proxy.protocol
        } else refreshProxyList()
      }
    }

    val ws = new scalajs.dom.WebSocket("ws://" + scalajs.dom.window.location.host)
    ws.onmessage = (x: MessageEvent) ⇒ processEvent(x)
    ws.onopen = (x: Event) ⇒ {
      webSocket = Some(ws)
      ws.send(s"list=${scope.currentList}")
      console.info("WebSocket opened")
    }
    ws.onclose = (x: CloseEvent) ⇒ {
      webSocket = None
      val errorMessage = s"WebSocket was closed: ${x.reason}"
      console.warn(errorMessage)
      Notifications.error(errorMessage, Layout.topRight, 3000)
      org.scalajs.dom.setTimeout(() ⇒ openWebSocket(), 5000)
    }
  }

  @JSExport
  def openTxt(): Unit = {
    val alive = location.path() match {
      case "/live" ⇒ Some(true)
      case "/dead" ⇒ Some(false)
      case _ ⇒ None
    }
    val country = Some(scope.showCountry).filter(_.nonEmpty)
    val list = scope.currentList

    val url = "/proxylist.txt?" + Seq(alive.map("alive=" + _), country.map("country=" + _), Some("list=" + URIUtils.encodeURIComponent(list))).flatten.mkString("&")

    scalajs.dom.window.open(url, "_blank")
  }

  override def initialize(): Unit = {
    super.initialize()
  
    scope.countryFilter = literal()
    scope.countryInfo = GeoipResult()
    scope.newListInMemory = false
    scope.proxies = js.Array[ProxyServiceEntry]()
    scope.proxyLists = js.Array[String]()
    scope.location = location
    scope.statusFilter = literal()
    scope.showCountry = ""
    scope.currentList = getListCookie()

    scope.$watch("location.path()", (path: UndefOr[String]) ⇒
      scope.statusFilter = path.toOption match {
        case Some("/live") ⇒ literal(alive = true)
        case Some("/dead") ⇒ literal(alive = false)
        case _ ⇒ literal()
      }
    )

    scope.$watch("showCountry", (country: UndefOr[String]) ⇒ {
      scope.countryFilter = country.toOption match {
        case Some(countryCode: String) if countryCode.nonEmpty ⇒ literal(geoip=literal(code=countryCode))
        case _ ⇒ literal()
      }
    })

    refreshLists()
    refreshProxyList()
    org.scalajs.dom.setInterval(() ⇒ scope.$apply {
      refreshProxyList()
      refreshLists()
    }, 15000)
    openWebSocket()
  }

  @JSExport
  def getGeoipString(geoip: GeoipResult): String = {
    Vector(geoip.country, geoip.city).filterNot(_.isEmpty).mkString(", ")
  }

  @JSExport
  def getGeoipFlagSrc(geoip: GeoipResult): String = {
    if (geoip.code.nonEmpty) s"/flag/${geoip.code.toLowerCase}.png" else ""
  }

  @JSExport
  def proxyRowClass(proxy: ProxyServiceEntry): String = {
    if (proxy.alive) "success" else "danger"
  }

  @JSExport
  def listRowClass(list: String): String = {
    if (list == scope.currentList) "success" else ""
  }

  private val deleteDblClick = new DoubleClickHandler {
    override def onClick(args: Any*): Unit = {
      Notifications.confirmation("Delete dead proxies from list?", Layout.topRight) {
        after(service.deleteAll(alive = Some(false), olderThan = Some(30))) { _ ⇒
          refreshProxyList()
          Notifications.success("Dead proxies successfully deleted", Layout.topRight)
        }
      }
    }

    override def onDoubleClick(args: Any*): Unit = {
      Notifications.confirmation("Delete all proxies from list?", Layout.topRight) {
        after(service.deleteAll()) { _ ⇒
          refreshProxyList()
          Notifications.warning("Proxy list cleared", Layout.topRight)
        }
      }
    }
  }

  @JSExport
  def deleteDead(): Unit = deleteDblClick.click()

  @JSExport
  def deleteAll(): Unit = deleteDblClick.doubleClick()

  @JSExport
  def changeLiveFilter(): Unit = {
    location.path(location.path() match {
      case "/live" ⇒ "/dead"
      case "/dead" ⇒ ""
      case _ ⇒ "/live"
    })
  }

  @JSExport
  def rescan(address: String): Unit = {
    after(service.rescanProxy(address)) { _ ⇒
      refreshProxyList()
      Notifications.info(s"Rescanning proxy: $address", Layout.topLeft, 300)
    }
  }

  private val rescanDblClick = new DoubleClickHandler {
    override def onClick(args: Any*): Unit = {
      after(service.rescanDead()) { _ ⇒
        refreshProxyList()
        Notifications.info("Rescanning dead proxies", Layout.topRight)
      }
    }

    override def onDoubleClick(args: Any*): Unit = {
      after(service.rescanAll()) { _ ⇒
        refreshProxyList()
        Notifications.info("Rescanning all proxies", Layout.topRight)
      }
    }
  }

  @JSExport
  def rescanDead(): Unit = rescanDblClick.click()

  @JSExport
  def rescanAll(): Unit = rescanDblClick.doubleClick()

  private def after[T](future: Future[T])(onSuccess: T ⇒ Unit): Unit = {
    future onComplete {
      case Success(v) ⇒
        onSuccess(v.asInstanceOf[T])

      case Failure(t) ⇒ 
        handleError(t)
    }
  }

  @JSExport
  def refreshProxyList(): Unit = {
    after(service.getProxyList) { proxies ⇒
      scope.proxies = proxies.toJSArray
    }
  }

  @JSExport
  def addProxy(): Unit = {
    val text: String = scope.proxyText.trim
    val temp: Boolean = scope.proxyAddTemp == 1
    if (text.nonEmpty) addProxy(text, temp, scope)
  }

  private def addProxy(text: String, temp: Boolean, scope: ProxyTableScope): Unit = {
    after(service.addProxies(text, temp)) { _ ⇒
      refreshProxyList()
      scope.proxyText = ""
      Notifications.success("Proxies added to list", Layout.topRight)
    }
  }

  @JSExport
  def copyProxy(address: String, list: String): Unit = {
    console.info(s"Copying proxy $address to list [$list]")
    after(service.addProxies(address, temp = false, Some(list))) { _ ⇒
      Notifications.success(s"Successfully copied $address to $list", Layout.topLeft, 300)
    }
  }

  @JSExport
  def removeProxy(address: String): Unit = {
    console.info(s"Removing proxy: $address")
    after(service.removeProxy(address)) { _ ⇒
      scope.proxies = proxies(scope).filter(_.address != address).toJSArray // Fast delete
      Notifications.warning(s"Proxy deleted: $address", Layout.topLeft, 300)
    }
  }

  @JSExport
  def showCountryInfo(geoip: GeoipResult): Unit = {
    assert(geoip.country.nonEmpty, "Invalid country")
    scope.countryInfo = geoip
    
    // Bootstrap fix
    js.Dynamic.global.$("#country-info-dialog h3 abbr").attr("data-original-title", s"ISO code: ${scope.countryInfo.code}")
    
    // Wikipedia frame
    val lang = js.eval("navigator.language || navigator.userLanguage").toString.take(2).toLowerCase
    
    val article = if (lang == "ru") {
        scope.countryInfo.country
            .replaceAll("о-ва", "острова")
            .replaceAll("о-в", "остров")
    } else scope.countryInfo.country
    
    js.Dynamic.global.$("#country-info-wikipedia-frame").html("<a class=\"embedly-card\" href=\"https://en.wikipedia.org/wiki/" + lang + ":" + article + "\">" + article + " - Wikipedia</a><script async src=\"//cdn.embedly.com/widgets/platform.js\" charset=\"UTF-8\"></script>")
    
    // Show dialog
    js.Dynamic.global.$("#country-info-dialog").modal(literal(show = true))
  }

  @JSExport
  def countryShow(geoip: GeoipResult): Unit = {
    scope.showCountry = if (scope.showCountry == geoip.code) "" else geoip.code
    console.info(scope.showCountry)
  }

  @JSExport
  def countryDelete(geoip: GeoipResult): Unit = {
    Notifications.confirmation(s"Delete all proxies from ${geoip.country}?", Layout.topCenter) {
      after(service.deleteAll(country = Some(geoip.code))) { _ ⇒
        scope.showCountry = ""
        js.Dynamic.global.$("#country-info-dialog").modal("hide")
        refreshProxyList()
        Notifications.warning(s"Deleted all proxies from ${geoip.country}", Layout.topRight)
      }
    }
  }

  @JSExport
  def refreshLists(): Unit = {
    after(service.getProxyListsNames) { lists ⇒
      scope.proxyLists = lists.toJSArray
      if (scope.currentList.nonEmpty && !lists.contains(scope.currentList)) changeList("")
    }
  }

  @JSExport
  def addList(): Unit = {
    val name = scope.newListName
    assert(name.nonEmpty, "List name cannot be empty")
    console.info(s"Creating list: $name")

    after(service.createList(name, scope.newListInMemory)) { _ ⇒
      changeList(name)
      scope.newListName = ""
      refreshLists()
      Notifications.success(s"List created: $name", Layout.topRight)
    }
  }

  @JSExport
  def modifyListSourcesDlg(listName: String): Unit = {
    after(service.getProxyListSources(listName)) { sources ⇒
      scope.modifyListName = listName
      scope.modifyListSources = sources.mkString("\n")
      js.Dynamic.global.$("#list-sources-dialog").modal(literal(show = true))
    }
  }

  @JSExport
  def modifyListSources(): Unit = {
    after(service.setProxyListSources(scope.modifyListName, scope.modifyListSources.trim)) { _ ⇒
      scalajs.dom.setTimeout(() ⇒ refreshProxyList(), 3000)
    }
  }

  @JSExport
  def removeList(name: String): Unit = {
    console.info(s"Deleting list: $name")
    after(service.deleteList(name)) { _ ⇒
      changeList("")
      refreshLists()
    }
  }

  private def getListCookie(): String =
    scalajs.dom.document.cookie.replaceAll("(?:(?:^|.*;\\s*)list\\s*\\=\\s*([^;]*).*$)|^.*$", "$1")

  private def setListCookie(name: String): Unit = {
    import org.scalajs.dom.document
    document.cookie = s"list=$name"
  }

  @JSExport
  def changeList(name: String): Unit = {
    if (scope.currentList != name) {
      console.info(s"List changed: $name")
      scope.currentList = name
      setListCookie(name)
      webSocket.foreach(_.send(s"list=$name"))
    }
    refreshProxyList()
  }


  private def proxies(scope: ProxyTableScope): Seq[ProxyServiceEntry] = scope.proxies
}
