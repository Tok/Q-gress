import kotlinx.browser.window
import org.w3c.dom.Window
import util.HtmlUtil

private fun win(): Window? = if (jsTypeOf(window) == "undefined") null else window

fun main(args: Array<String>) {
    win()?.onload = {
        HtmlUtil.load()
    }
}
