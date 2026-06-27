package util

import kotlinx.browser.localStorage

/**
 * Persists the per-instrument tuning (currently the explosion [KickDrum]) across reloads, mirroring
 * [VolumePrefs] / [AudioPrefs]. [load] runs at startup; [save] is called when an Instruments knob changes; the
 * TUNING LAB shows + resets the values.
 */
object InstrumentPrefs {
    private const val KEY = "qgress.instruments"

    fun load() {
        if (HtmlUtil.isNotRunningInBrowser()) return
        runCatching {
            val o = JSON.parse<dynamic>(localStorage.getItem(KEY) ?: return).kick ?: return
            apply(o.pitch) { KickDrum.setPitchMult(it) }
            apply(o.decay) { KickDrum.setDecayMult(it) }
            apply(o.click) { KickDrum.setClickMult(it) }
            apply(o.drive) { KickDrum.setDrive(it) }
        }
    }

    fun save() {
        if (HtmlUtil.isNotRunningInBrowser()) return
        runCatching { localStorage.setItem(KEY, JSON.stringify(json())) }
    }

    /** The instrument tuning as a plain object — persisted by [save] and shown by the TUNING LAB. */
    fun json(): dynamic {
        val kick: dynamic = js("({})")
        kick.pitch = KickDrum.pitchMult
        kick.decay = KickDrum.decayMult
        kick.click = KickDrum.clickMult
        kick.drive = KickDrum.drive
        val o: dynamic = js("({})")
        o.kick = kick
        return o
    }

    private fun apply(v: dynamic, set: (Double) -> Unit) {
        if (v != null) set(v.unsafeCast<Double>())
    }
}
