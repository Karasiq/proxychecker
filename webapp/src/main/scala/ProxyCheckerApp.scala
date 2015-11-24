import com.greencatsoft.angularjs.Angular

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

@JSExport
object ProxyCheckerApp extends JSApp {
  override def main(): Unit = {
    val module = Angular.module("proxyChecker", Seq("angularUtils.directives.dirPagination"))
    module.controller[ProxyTableController]
    module.factory[ProxyServiceFactory]
  }
}
