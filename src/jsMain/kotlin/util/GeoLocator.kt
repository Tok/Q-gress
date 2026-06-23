package util

import kotlinx.browser.window

/**
 * Best-effort browser geolocation that **never prompts**: it resolves the player's position only when
 * location permission was *already* granted (checked first via the Permissions API). The title uses
 * this to open on the player's home when it's available without a permission popup; callers fall back
 * (to a random location) otherwise. Onboarding's explicit "Home" button still prompts on purpose —
 * that's a deliberate user action, not the title's silent best-effort.
 */
object GeoLocator {
    private const val TIMEOUT_MS = 2500 // give up fast so the title isn't held up → caller falls back
    private const val MAX_AGE_MS = 6 * 60 * 60 * 1000 // accept a cached fix up to ~6h old (no GPS spin-up)

    /**
     * Calls [onLocated] with (lng, lat) **iff** geolocation permission is already granted and a fix
     * arrives quickly; otherwise calls [onNone]. Exactly one of the callbacks fires.
     */
    fun homeIfPermitted(onLocated: (Double, Double) -> Unit, onNone: () -> Unit) {
        val hasApis = js("typeof navigator !== 'undefined' && !!navigator.geolocation && !!navigator.permissions")
            .unsafeCast<Boolean>()
        if (!hasApis) {
            onNone()
            return
        }
        val query: dynamic = js("({ name: 'geolocation' })")
        window.asDynamic().navigator.permissions.query(query).then(
            { status: dynamic -> if (status.state == "granted") requestPosition(onLocated, onNone) else onNone() },
            { _: dynamic -> onNone() }, // query rejected (older browsers) → don't risk a prompt, fall back
        )
    }

    private fun requestPosition(onLocated: (Double, Double) -> Unit, onNone: () -> Unit) {
        val opts: dynamic = js("({})")
        opts.timeout = TIMEOUT_MS
        opts.maximumAge = MAX_AGE_MS // prefer a cached fix → fast, and won't prompt (permission already granted)
        window.asDynamic().navigator.geolocation.getCurrentPosition(
            { pos: dynamic -> onLocated(pos.coords.longitude as Double, pos.coords.latitude as Double) },
            { _: dynamic -> onNone() },
        )
    }
}
