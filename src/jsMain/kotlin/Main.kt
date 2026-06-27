import kotlinx.browser.window
import org.w3c.dom.Window
import system.ui.Bootstrap
import util.VersionCheck

private fun win(): Window? = if (jsTypeOf(window) == "undefined") null else window

fun main(args: Array<String>) {
    win()?.onload = {
        Bootstrap.load()
        VersionCheck.start() // offer a reload when a newer build is deployed (no hard-refresh needed)
    }
}
