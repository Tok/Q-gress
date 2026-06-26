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
    /**
     * Force the headless ([NoOpEffects]) sink even in the browser. An in-browser headless eval (the TRAIN tab
     * trainer / the leaderboard) runs many [ai.SimRunner] matches, each of which calls [reset] — so a one-off
     * [install] wouldn't stick. Setting this makes [default]/[reset] yield [NoOpEffects] for the duration, so
     * those matches fire no stray 3D/sound FX; clear it (then [reset]) to restore the live renderer.
     */
    var headless: Boolean = false

    private fun default(): Effects = if (headless || HtmlUtil.isNotRunningInBrowser()) NoOpEffects else BrowserEffects

    var sink: Effects = default()
        private set

    fun install(effects: Effects) {
        sink = effects
    }

    fun reset() {
        sink = default()
    }
}
