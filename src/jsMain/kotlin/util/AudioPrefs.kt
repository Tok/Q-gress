package util

import kotlinx.browser.localStorage

/**
 * Persists the master [AudioFx] tuning (filter, resonance, reverb, delay, compressor, master ADSR) across
 * reloads, mirroring [VolumePrefs]. One JSON blob under [KEY]. [load] runs at startup **before** the audio
 * graph builds — it sets the [AudioFx] canonical vars (the live nodes pick them up in `AudioFx.build` →
 * `applyAll`); [save] is called by the AUDIO tab after any change and by the TUNING LAB reset.
 */
object AudioPrefs {
    private const val KEY = "qgress.audiofx"

    fun load() {
        if (HtmlUtil.isNotRunningInBrowser()) return
        runCatching {
            val raw = localStorage.getItem(KEY) ?: return
            val o = JSON.parse<dynamic>(raw)
            apply(o.lowpassHz) { AudioFx.setLowpass(it) }
            apply(o.lowpassQ) { AudioFx.setLowpassQ(it) }
            apply(o.reverbMix) { AudioFx.setReverbMix(it) }
            apply(o.delayTimeS) { AudioFx.setDelayTime(it) }
            apply(o.delayFeedback) { AudioFx.setDelayFeedback(it) }
            apply(o.delayMix) { AudioFx.setDelayMix(it) }
            apply(o.compressAmount) { AudioFx.setCompress(it) }
            apply(o.envAttackS) { AudioFx.setEnvAttack(it) }
            apply(o.envDecayS) { AudioFx.setEnvDecay(it) }
            apply(o.envSustain) { AudioFx.setEnvSustain(it) }
            apply(o.envReleaseMult) { AudioFx.setEnvRelease(it) }
        }
    }

    fun save() {
        if (HtmlUtil.isNotRunningInBrowser()) return
        runCatching {
            val o: dynamic = js("({})")
            o.lowpassHz = AudioFx.lowpassHz
            o.lowpassQ = AudioFx.lowpassQ
            o.reverbMix = AudioFx.reverbMix
            o.delayTimeS = AudioFx.delayTimeS
            o.delayFeedback = AudioFx.delayFeedback01
            o.delayMix = AudioFx.delayMix
            o.compressAmount = AudioFx.compressAmount
            o.envAttackS = AudioFx.envAttackS
            o.envDecayS = AudioFx.envDecayS
            o.envSustain = AudioFx.envSustain
            o.envReleaseMult = AudioFx.envReleaseMult
            localStorage.setItem(KEY, JSON.stringify(o))
        }
    }

    private fun apply(v: dynamic, set: (Double) -> Unit) {
        if (v != null) set(v.unsafeCast<Double>())
    }
}
