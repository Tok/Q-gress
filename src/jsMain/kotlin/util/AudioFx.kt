package util

import kotlin.math.pow

/**
 * The master audio FX bus, sitting between [SoundUtil]'s mix and its safety limiter. The signal runs
 * `input → high-pass → low-pass (with resonance) → compressor → fxOut`, with parallel **reverb** and
 * **delay/echo** sends summed back into `fxOut`, and an [analyser] tap for the AUDIO tab's scope/spectrum.
 * A master **ADSR** ([envAttack]/[envReleaseMult], wired into [SoundUtil]'s shared one-shot voice) reshapes
 * most SFX. All node values are mirrored in plain vars so they can be read back (sliders, the TUNING LAB
 * export) and re-applied after [build] / after [util.AudioPrefs] loads.
 *
 * Web Audio nodes are reached via `dynamic`; the setters ramp smoothly. Defaults are all "neutral" (open
 * filter, no resonance, dry reverb/delay, no compression, instant attack / unstretched tail) so a fresh
 * install sounds exactly as before until something is dialled.
 */
object AudioFx {
    const val LOWPASS_OPEN_HZ = 22000.0 // wide open (no audible low-pass)
    const val HIGHPASS_OPEN_HZ = 20.0 // wide open (no audible high-pass)
    const val MIN_Q = 0.7 // Butterworth — no resonant peak
    const val MAX_Q = 24.0 // strongly resonant
    const val MAX_DELAY_S = 1.0 // delay line length
    private const val ENV_EPS = 0.0001 // non-zero floor for exponential gain ramps (can't ramp to 0)
    private const val REVERB_SECONDS = 2.2
    private const val REVERB_DECAY = 2.6 // higher = faster tail decay
    private const val REVERB_RETURN = 0.9 // shared convolver return level (both the global mix + the per-voice send)

    private var ctx: dynamic = null
    private var highpass: dynamic = null
    private var lowpass: dynamic = null
    private var compressor: dynamic = null
    private var reverbWet: dynamic = null // global send (master mix → reverb); 0 = dry
    private var sendBus: dynamic = null // explicit per-voice send: loud one-shots (explosions) route extra reverb here
    private var delay: dynamic = null
    private var delayFeedback: dynamic = null
    private var delayWet: dynamic = null
    private var analyserNode: dynamic = null

    // --- Canonical state (mirrors the live nodes; the read-back + persistence source of truth) ---------
    var lowpassHz = LOWPASS_OPEN_HZ
        private set
    var lowpassQ = MIN_Q
        private set
    var reverbMix = 0.0
        private set
    var delayTimeS = 0.25
        private set
    var delayFeedback01 = 0.3
        private set
    var delayMix = 0.0
        private set
    var compressAmount = 0.0
        private set // 0 = bypass, 1 = heavy
    var envAttackS = 0.0
        private set // master one-shot attack ramp (0 = instant, as before)
    var envDecayS = 0.0
        private set // exported for sound-default tuning (not wired to one-shots)
    var envSustain = 1.0
        private set // exported for sound-default tuning (not wired to one-shots)
    var envReleaseMult = 1.0
        private set // stretches the one-shot tail (1.0 = unchanged)

    /** Wire the master FX between [input] and [output] on audio context [audioCtx]. Call once. */
    fun build(audioCtx: dynamic, input: dynamic, output: dynamic) {
        ctx = audioCtx
        val hp = audioCtx.createBiquadFilter()
        hp.type = "highpass"
        hp.frequency.value = HIGHPASS_OPEN_HZ
        val lp = audioCtx.createBiquadFilter()
        lp.type = "lowpass"
        lp.frequency.value = LOWPASS_OPEN_HZ
        val comp = audioCtx.createDynamicsCompressor()
        val fxOut = audioCtx.createGain()
        input.connect(hp)
        hp.connect(lp)
        lp.connect(comp)
        comp.connect(fxOut) // dry (compressed) path
        // Reverb send (off the filtered signal) → convolver → return → fxOut.
        val conv = audioCtx.createConvolver()
        conv.buffer = impulse(audioCtx)
        val wet = audioCtx.createGain()
        wet.gain.value = 0.0
        lp.connect(wet)
        wet.connect(conv)
        val send = audioCtx.createGain() // per-voice send (explosions), independent of the global mix
        send.gain.value = 1.0
        send.connect(conv)
        val ret = audioCtx.createGain()
        ret.gain.value = REVERB_RETURN
        conv.connect(ret)
        ret.connect(fxOut)
        // Delay/echo send (off the filtered signal): delay → feedback loop → wet → fxOut.
        val dl = audioCtx.createDelay(MAX_DELAY_S)
        val fb = audioCtx.createGain()
        val dWet = audioCtx.createGain()
        dWet.gain.value = 0.0
        lp.connect(dl)
        dl.connect(fb)
        fb.connect(dl) // regenerating feedback
        dl.connect(dWet)
        dWet.connect(fxOut)
        // Analyser tap (pass-through): fxOut → analyser → output.
        val an = audioCtx.createAnalyser()
        an.fftSize = 2048
        an.smoothingTimeConstant = 0.78
        fxOut.connect(an)
        an.connect(output)
        highpass = hp
        lowpass = lp
        compressor = comp
        reverbWet = wet
        sendBus = send
        delay = dl
        delayFeedback = fb
        delayWet = dWet
        analyserNode = an
        applyAll() // push any pre-build (persisted) values onto the fresh nodes
    }

    /** The per-voice reverb send bus (connect a source/gain here for explosion-only space); null pre-build. */
    fun reverbSend(): dynamic = sendBus

    /** The analyser node tapping the master FX output (for the AUDIO tab's scope/spectrum); null pre-build. */
    fun analyser(): dynamic = analyserNode

    fun setLowpass(hz: Double) {
        lowpassHz = hz
        ramp(lowpass?.frequency, hz)
    }

    fun setLowpassQ(q: Double) {
        lowpassQ = q
        ramp(lowpass?.Q, q)
    }

    fun setHighpass(hz: Double) = ramp(highpass?.frequency, hz)

    /** Transient onboarding muffle: clamp the low-pass node down to [closedHz], or restore the player's stored
     *  cutoff — WITHOUT touching the canonical [lowpassHz] (so un-muffling returns to whatever they'd dialled). */
    fun muffle(on: Boolean, closedHz: Double) = ramp(lowpass?.frequency, if (on) closedHz else lowpassHz)

    fun setReverbMix(wet: Double) {
        reverbMix = wet
        ramp(reverbWet?.gain, wet)
    }

    fun setDelayTime(seconds: Double) {
        delayTimeS = seconds.coerceIn(0.0, MAX_DELAY_S)
        ramp(delay?.delayTime, delayTimeS)
    }

    fun setDelayFeedback(amount: Double) {
        delayFeedback01 = amount.coerceIn(0.0, 0.95) // < 1 so it can't self-oscillate to clipping
        ramp(delayFeedback?.gain, delayFeedback01)
    }

    fun setDelayMix(wet: Double) {
        delayMix = wet.coerceIn(0.0, 1.0)
        ramp(delayWet?.gain, delayMix)
    }

    /** Master compressor amount: 0 = bypass (no gain reduction), 1 = heavy. Maps to threshold + ratio. */
    fun setCompress(amount: Double) {
        compressAmount = amount.coerceIn(0.0, 1.0)
        val c = compressor ?: return
        c.threshold.value = -amount * 45.0 // 0 dB (off) → −45 dB (heavy)
        c.ratio.value = 1.0 + amount * 11.0 // 1:1 (off) → 12:1
        c.knee.value = 24.0
        c.attack.value = 0.005
        c.release.value = 0.18
    }

    /**
     * Apply the master ADSR to a one-shot voice's [gain] param: an optional attack ramp up to [peak], then an
     * exponential decay to silence. The release multiplier stretches the tail. Returns the total voice length
     * (so the caller can stop the oscillator). Default state (attack 0, release ×1) reproduces the old
     * peak→silence one-shot exactly. Centralised here so [SoundUtil.decayVoice] stays tiny.
     */
    fun shapeDecay(gain: dynamic, now: Double, peak: Double, dur: Double): Double {
        val total = dur * envReleaseMult
        val attack = envAttackS.coerceAtMost(total * 0.5)
        if (attack > 0.0) {
            gain.setValueAtTime(ENV_EPS, now)
            gain.linearRampToValueAtTime(peak, now + attack)
        } else {
            gain.setValueAtTime(peak, now)
        }
        gain.exponentialRampToValueAtTime(ENV_EPS, now + total)
        return total
    }

    // Master ADSR (wired into SoundUtil.decayVoice via shapeDecay): attack + release shape most one-shot SFX.
    fun setEnvAttack(seconds: Double) {
        envAttackS = seconds.coerceIn(0.0, 0.5)
    }

    fun setEnvDecay(seconds: Double) {
        envDecayS = seconds.coerceIn(0.0, 0.5)
    }

    fun setEnvSustain(level: Double) {
        envSustain = level.coerceIn(0.0, 1.0)
    }

    fun setEnvRelease(multiplier: Double) {
        envReleaseMult = multiplier.coerceIn(0.2, 3.0)
    }

    /** Re-push every canonical value onto the live nodes (after [build] or an [util.AudioPrefs] load). */
    fun applyAll() {
        setLowpass(lowpassHz)
        setLowpassQ(lowpassQ)
        setReverbMix(reverbMix)
        setDelayTime(delayTimeS)
        setDelayFeedback(delayFeedback01)
        setDelayMix(delayMix)
        setCompress(compressAmount)
    }

    /** Restore every FX + envelope value to its neutral default (the TUNING LAB "reset"). */
    fun resetToDefaults() {
        setLowpass(LOWPASS_OPEN_HZ)
        setLowpassQ(MIN_Q)
        setReverbMix(0.0)
        setDelayTime(0.25)
        setDelayFeedback(0.3)
        setDelayMix(0.0)
        setCompress(0.0)
        setEnvAttack(0.0)
        setEnvDecay(0.0)
        setEnvSustain(1.0)
        setEnvRelease(1.0)
    }

    private fun ramp(param: dynamic, target: Double) {
        val c = ctx ?: return
        param?.setTargetAtTime(target, c.currentTime, 0.12)
    }

    // A simple algorithmic reverb impulse: stereo decaying noise (the standard convolver IR trick).
    private fun impulse(audioCtx: dynamic): dynamic {
        val rate = audioCtx.sampleRate as Double
        val len = (rate * REVERB_SECONDS).toInt().coerceAtLeast(1)
        val buf = audioCtx.createBuffer(2, len, rate)
        for (ch in 0..1) {
            val data = buf.getChannelData(ch)
            var i = 0
            while (i < len) {
                data[i] = ((Util.random() * 2.0 - 1.0) * (1.0 - i.toDouble() / len).pow(REVERB_DECAY)).toFloat()
                i++
            }
        }
        return buf
    }
}
