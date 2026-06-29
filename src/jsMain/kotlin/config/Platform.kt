package config

import kotlinx.browser.window
import system.ui.Bootstrap

// JS shell: answer the platform facts from Bootstrap + the live window (the imperative edge the pure
// `config` core can't reach). See the commonMain `expect object Platform` for the contract.
actual object Platform {
    actual fun isBrowser(): Boolean = Bootstrap.isRunningInBrowser()
    actual fun isLocal(): Boolean = Bootstrap.isLocal()
    actual fun windowWidth(fallback: Int): Int = if (isBrowser()) window.innerWidth else fallback
    actual fun windowHeight(fallback: Int): Int = if (isBrowser()) window.innerHeight else fallback
}
