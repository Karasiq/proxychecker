import org.scalajs

abstract class DoubleClickHandler {
  def onClick(args: Any*): Unit
  def onDoubleClick(args: Any*): Unit

  private var isDblClick = false

  final def click(args: Any*) = {
    scalajs.dom.setTimeout(() ⇒ if (!isDblClick) onClick(args), 300)
  }

  final def doubleClick(args: Any*) = {
    isDblClick = true
    scalajs.dom.setTimeout(() ⇒ isDblClick = false, 400)
    onDoubleClick(args)
  }
}
