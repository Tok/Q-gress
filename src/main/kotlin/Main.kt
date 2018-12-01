import org.w3c.dom.Window
import util.HtmlUtil
import kotlin.browser.window

private fun win(): Window? = if (jsTypeOf(window) == "undefined") null else window

fun main(args: Array<String>) {
    win()?.onload = {
        HtmlUtil.load()
    }
}
