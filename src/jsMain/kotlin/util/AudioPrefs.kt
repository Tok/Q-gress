package util

/**
 * Persists the master [AudioFx] tuning (filter, resonance, reverb, delay, compressor, master ADSR) across
 * reloads, mirroring [VolumePrefs]. One JSON blob under [KEY]. [load] runs at startup **before** the audio
 * graph builds — it sets the [AudioFx] canonical vars (the live nodes pick them up in `AudioFx.build` →
 * `applyAll`); [save] is called by the AUDIO tab after any change and by the TUNING LAB reset.
 */
object AudioPrefs {
    private const val KEY = "qgress.audiofx"

    fun load() {
        val o = Prefs.read(KEY) ?: return
        applyFx(o)
        applyEnv(o)
    }

    private fun applyFx(o: dynamic) {
        Prefs.apply(o.lowpassHz) { AudioFx.setLowpass(it) }
        Prefs.apply(o.lowpassQ) { AudioFx.setLowpassQ(it) }
        Prefs.apply(o.distortion) { AudioFx.setDistortion(it) }
        Prefs.apply(o.lfoRateHz) { AudioFx.setLfoRate(it) }
        Prefs.apply(o.lfoDepth) { AudioFx.setLfoDepth(it) }
        Prefs.apply(o.reverbMix) { AudioFx.setReverbMix(it) }
        Prefs.apply(o.delayTimeS) { AudioFx.setDelayTime(it) }
        Prefs.apply(o.delayFeedback) { AudioFx.setDelayFeedback(it) }
        Prefs.apply(o.delayMix) { AudioFx.setDelayMix(it) }
        Prefs.apply(o.compressAmount) { AudioFx.setCompress(it) }
    }

    private fun applyEnv(o: dynamic) {
        Prefs.apply(o.envAttackS) { AudioFx.setEnvAttack(it) }
        Prefs.apply(o.envDecayS) { AudioFx.setEnvDecay(it) }
        Prefs.apply(o.envSustain) { AudioFx.setEnvSustain(it) }
        Prefs.apply(o.envReleaseMult) { AudioFx.setEnvRelease(it) }
    }

    fun save() = Prefs.save(KEY, ::json)

    /** The current FX/envelope state as a plain object — persisted by [save] and shown by the TUNING LAB. */
    fun json(): dynamic {
        val o: dynamic = js("({})")
        o.lowpassHz = AudioFx.lowpassHz
        o.lowpassQ = AudioFx.lowpassQ
        o.distortion = AudioFx.distortionAmount
        o.lfoRateHz = AudioFx.lfoRateHz
        o.lfoDepth = AudioFx.lfoDepth
        o.reverbMix = AudioFx.reverbMix
        o.delayTimeS = AudioFx.delayTimeS
        o.delayFeedback = AudioFx.delayFeedback01
        o.delayMix = AudioFx.delayMix
        o.compressAmount = AudioFx.compressAmount
        o.envAttackS = AudioFx.envAttackS
        o.envDecayS = AudioFx.envDecayS
        o.envSustain = AudioFx.envSustain
        o.envReleaseMult = AudioFx.envReleaseMult
        return o
    }
}
