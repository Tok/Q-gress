package util

import kotlinx.browser.localStorage

/**
 * Shared `localStorage` plumbing for the settings stores ([AudioPrefs], [AmbientPrefs], [GameplayPrefs],
 * [InstrumentPrefs], [MixerPrefs]) — each used to repeat the same browser-guard + `runCatching` + JSON
 * dance. [VolumePrefs] stays bespoke (two scalar keys, not one JSON blob). All no-op safely when headless
 * or when storage is unavailable.
 */
object Prefs {
    /** Read + parse the JSON blob at [key], or null when headless / absent / unparseable. */
    fun read(key: String): dynamic? {
        if (HtmlUtil.isNotRunningInBrowser()) return null
        return runCatching {
            val raw = localStorage.getItem(key) ?: return null
            JSON.parse<dynamic>(raw)
        }.getOrNull()
    }

    /** Serialize [json] and store it under [key] (no-op headless / on failure). */
    fun save(key: String, json: () -> dynamic) {
        if (HtmlUtil.isNotRunningInBrowser()) return
        runCatching { localStorage.setItem(key, JSON.stringify(json())) }
    }

    /** Call [set] with [v] coerced to Double when [v] is present (a loaded field) — the common "apply if set". */
    fun apply(v: dynamic, set: (Double) -> Unit) {
        if (v != null) set(v.unsafeCast<Double>())
    }
}
