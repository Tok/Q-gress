package util

import kotlin.math.pow

/**
 * The master audio FX bus, sitting between [SoundUtil]'s mix and its limiter: a tunable **low-pass**
 * (also the onboarding "muffle"), a tunable **high-pass**, and a **reverb** send (a convolver fed a
 * generated decaying-noise impulse). Split out of [SoundUtil] (size) and exposed so the `#audio` demo
 * can tune the filters/reverb live while we dial the SFX in.
 *
 * All nodes are Web Audio API objects reached via `dynamic`. [build] wires `input → filters → output`
 * (dry) plus a parallel `→ reverb → wet → output`; the setters ramp smoothly.
 */
object AudioFx {
    const val LOWPASS_OPEN_HZ = 22000.0 // wide open (no audible low-pass)
    const val HIGHPASS_OPEN_HZ = 20.0 // wide open (no audible high-pass)
    private const val REVERB_SECONDS = 2.2
    private const val REVERB_DECAY = 2.6 // higher = faster tail decay

    private var ctx: dynamic = null
    private var lowpass: dynamic = null
    private var highpass: dynamic = null
    private var reverbWet: dynamic = null

    /** Wire the master FX between [input] and [output] on audio context [audioCtx]. Call once. */
    fun build(audioCtx: dynamic, input: dynamic, output: dynamic) {
        ctx = audioCtx
        val hp = audioCtx.createBiquadFilter()
        hp.type = "highpass"
        hp.frequency.value = HIGHPASS_OPEN_HZ
        val lp = audioCtx.createBiquadFilter()
        lp.type = "lowpass"
        lp.frequency.value = LOWPASS_OPEN_HZ
        input.connect(hp)
        hp.connect(lp)
        lp.connect(output) // dry path
        val conv = audioCtx.createConvolver()
        conv.buffer = impulse(audioCtx)
        val wet = audioCtx.createGain()
        wet.gain.value = 0.0 // dry by default; the demo (or future tuning) raises it
        lp.connect(conv)
        conv.connect(wet)
        wet.connect(output) // wet (reverb) path
        highpass = hp
        lowpass = lp
        reverbWet = wet
    }

    fun setLowpass(hz: Double) = ramp(lowpass?.frequency, hz)
    fun setHighpass(hz: Double) = ramp(highpass?.frequency, hz)
    fun setReverbMix(wet: Double) = ramp(reverbWet?.gain, wet)

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
