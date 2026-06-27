package util

import kotlinx.browser.localStorage

/**
 * Persists the [AmbientBed] state (on / level / cutoff) across reloads, mirroring [VolumePrefs] / [AudioPrefs].
 * [load] runs at startup (restores the bed, incl. re-starting it if it was on); [save] on any Ambient change;
 * shown + reset by the TUNING LAB.
 */
object AmbientPrefs {
    private const val KEY = "qgress.ambient"

    fun load() {
        if (HtmlUtil.isNotRunningInBrowser()) return
        runCatching {
            val o = JSON.parse<dynamic>(localStorage.getItem(KEY) ?: return)
            val level = o.level
            val cutoff = o.cutoff
            if (level != null) AmbientBed.setLevel(level.unsafeCast<Double>())
            if (cutoff != null) AmbientBed.setCutoff(cutoff.unsafeCast<Double>())
            if (o.enabled == false) AmbientBed.setEnabled(false) // default on (the field hum is automatic)
        }
    }

    fun save() {
        if (HtmlUtil.isNotRunningInBrowser()) return
        runCatching { localStorage.setItem(KEY, JSON.stringify(json())) }
    }

    /** The ambient-bed state as a plain object — persisted by [save] and shown by the TUNING LAB. */
    fun json(): dynamic {
        val o: dynamic = js("({})")
        o.enabled = AmbientBed.enabled
        o.level = AmbientBed.level
        o.cutoff = AmbientBed.cutoffHz
        return o
    }
}
