package util

import org.khronos.webgl.set

/**
 * A generative **ambient bed** — a slow, dark atmospheric layer under the sim (ported in spirit from Spectral
 * Plinko's "black hole ambient"): a sub-bass sine, a rumble triangle, lowpassed noise, and a band-passed
 * sawtooth drone, summed through a resonant low-pass that a slow LFO sweeps, into the **Ambient** mixer
 * channel ([Mixer]). Built once (lazily, browser-only); [setOn] gates the master gain so the oscillators just
 * idle when off. Level/cutoff/on-state persist via [AmbientPrefs] and feed the TUNING LAB + global reset.
 */
object AmbientBed {
    private const val SUB_HZ = 42.0
    private const val RUMBLE_HZ = 63.0
    private const val DRONE_HZ = 126.0
    private const val LFO_HZ = 0.08 // very slow cutoff drift
    private const val LFO_DEPTH_HZ = 180.0
    private const val RAMP_S = 1.5 // fade in/out so toggling isn't a click

    var on = false
        private set
    var level = 0.5
        private set
    var cutoffHz = 320.0
        private set

    private var built = false
    private var master: dynamic = null // master gain → Ambient bus
    private var filter: dynamic = null
    private var lfoGain: dynamic = null

    fun setOn(value: Boolean) {
        on = value
        if (value) build()
        rampMaster()
    }

    fun setLevel(v: Double) {
        level = v.coerceIn(0.0, 1.0)
        rampMaster()
    }

    fun setCutoff(hz: Double) {
        cutoffHz = hz.coerceIn(80.0, 2000.0)
        filter?.frequency?.asDynamic()?.setTargetAtTime(cutoffHz, SoundUtil.audioCtx.asDynamic().currentTime, 0.2)
    }

    fun resetTuning() {
        setOn(false)
        level = 0.5
        cutoffHz = 320.0
        if (built) setCutoff(cutoffHz)
        rampMaster()
    }

    private fun rampMaster() {
        val m = master ?: return
        val target = if (on) level * 0.5 else 0.0 // ×0.5 headroom: the bed sits well under the SFX
        m.gain.asDynamic().setTargetAtTime(target, SoundUtil.audioCtx.asDynamic().currentTime, RAMP_S)
    }

    private fun build() {
        if (built || HtmlUtil.isNotRunningInBrowser()) return
        built = true
        val ctx = SoundUtil.audioCtx.asDynamic()
        val lp = ctx.createBiquadFilter()
        lp.type = "lowpass"
        lp.frequency.value = cutoffHz
        lp.Q.value = 6.0
        val gain = ctx.createGain()
        gain.gain.value = 0.0
        lp.connect(gain)
        gain.connect(Mixer.bus(Mixer.Group.AMBIENT))
        tone(ctx, "sine", SUB_HZ, 0.34, lp)
        tone(ctx, "triangle", RUMBLE_HZ, 0.22, lp)
        tone(ctx, "sawtooth", DRONE_HZ, 0.10, lp, bandpass = 200.0)
        noise(ctx, lp)
        // Slow LFO sweeping the cutoff for a breathing, evolving texture.
        val lfo = ctx.createOscillator()
        lfo.type = "sine"
        lfo.frequency.value = LFO_HZ
        val lg = ctx.createGain()
        lg.gain.value = LFO_DEPTH_HZ
        lfo.connect(lg)
        lg.connect(lp.frequency)
        lfo.start()
        master = gain
        filter = lp
        lfoGain = lg
    }

    private fun tone(ctx: dynamic, type: String, hz: Double, gainVal: Double, dest: dynamic, bandpass: Double = 0.0) {
        val osc = ctx.createOscillator()
        osc.type = type
        osc.frequency.value = hz
        val g = ctx.createGain()
        g.gain.value = gainVal
        if (bandpass > 0.0) {
            val bp = ctx.createBiquadFilter()
            bp.type = "bandpass"
            bp.frequency.value = bandpass
            bp.Q.value = 8.0
            osc.connect(bp)
            bp.connect(g)
        } else {
            osc.connect(g)
        }
        g.connect(dest)
        osc.start()
    }

    private fun noise(ctx: dynamic, dest: dynamic) {
        val sr = ctx.sampleRate as Double
        val len = (sr * 4.0).toInt().coerceAtLeast(1) // 4 s loop
        val buf = ctx.createBuffer(1, len, sr)
        val data = buf.getChannelData(0)
        var i = 0
        while (i < len) {
            data[i] = ((Util.random() * 2.0 - 1.0) * 0.4).toFloat()
            i++
        }
        val src = ctx.createBufferSource()
        src.buffer = buf
        src.loop = true
        val lp = ctx.createBiquadFilter()
        lp.type = "lowpass"
        lp.frequency.value = 420.0
        val g = ctx.createGain()
        g.gain.value = 0.12
        src.connect(lp)
        lp.connect(g)
        g.connect(dest)
        src.start()
    }
}
