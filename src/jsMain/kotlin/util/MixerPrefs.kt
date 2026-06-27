package util

import kotlinx.browser.localStorage
import util.Mixer.Group

/**
 * Persists the per-role mixer (volume + mute for each [Group]) across reloads, mirroring [VolumePrefs] /
 * [AudioPrefs]. [load] runs at startup (the values seed the lazy gain buses when they're first built); [save]
 * is called whenever a Mixer channel changes.
 */
object MixerPrefs {
    private const val KEY = "qgress.mixer"

    fun load() {
        if (HtmlUtil.isNotRunningInBrowser()) return
        runCatching {
            val raw = localStorage.getItem(KEY) ?: return
            val o = JSON.parse<dynamic>(raw)
            Group.values().forEach { g ->
                val entry = o[g.name] ?: return@forEach
                val v = entry.v
                val m = entry.m
                if (v != null) Mixer.setVolume(g, v.unsafeCast<Double>())
                if (m != null) Mixer.setMuted(g, m.unsafeCast<Boolean>())
            }
        }
    }

    fun save() {
        if (HtmlUtil.isNotRunningInBrowser()) return
        runCatching { localStorage.setItem(KEY, JSON.stringify(json())) }
    }

    /** The mixer state as a plain object — persisted by [save] and shown by the TUNING LAB. */
    fun json(): dynamic {
        val o: dynamic = js("({})")
        Group.values().forEach { g ->
            val entry: dynamic = js("({})")
            entry.v = Mixer.volume(g)
            entry.m = Mixer.isMuted(g)
            o[g.name] = entry
        }
        return o
    }
}
