package system.effect

import util.HtmlUtil

/**
 * The live [Effects] sink — the install point for the imperative-shell boundary. Self-selects
 * [BrowserEffects] in the browser / [NoOpEffects] headless at first reference (assigning the sink
 * *object* invokes no renderer method, so no three.js geometry is forced). A harness installs a fake
 * via [install]; [reset] restores the default (e.g. between headless matches / tests). Mirrors
 * [ai.FactionPolicies].
 */
object Fx {
    private fun default(): Effects = if (HtmlUtil.isRunningInBrowser()) BrowserEffects else NoOpEffects

    var sink: Effects = default()
        private set

    fun install(effects: Effects) {
        sink = effects
    }

    fun reset() {
        sink = default()
    }
}
