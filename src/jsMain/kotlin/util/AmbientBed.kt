package util

import World
import config.Sim
import org.khronos.webgl.set
import portal.Field
import util.data.Pos
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * The **field hum** — a generative atmospheric drone (sub-bass / rumble / noise / band-passed drone through a
 * slow LFO-swept resonant low-pass, ported in spirit from Spectral Plinko's "black hole ambient") that the
 * control **fields emit**. [updateFromFields] (called each frame) drives its **volume from the % of the play
 * area covered by fields** (capped at 100% — layered fields can exceed it) and **pans** it (3D) toward the
 * area-weighted field centroid. **No fields → silent.** Routes into the **Ambient** mixer channel ([Mixer]);
 * the AUDIO-tab Ambient sub-tab tunes enable / level / cutoff (persisted via [AmbientPrefs]).
 */
object AmbientBed {
    private const val SUB_HZ = 42.0
    private const val RUMBLE_HZ = 63.0
    private const val DRONE_HZ = 126.0
    private const val LFO_HZ = 0.08 // very slow cutoff drift
    private const val LFO_DEPTH_HZ = 180.0
    private const val RAMP_S = 0.6 // gain glide so coverage changes aren't clicky
    private const val FULL_AT = 0.5 // field coverage at which the hum reaches full volume (clamps above)
    private const val BOOST = 5.0 // the hum is a prominent layer — lift it well above the raw coverage gain

    var enabled = true
        private set
    var level = 0.6
        private set // master scale on the coverage-driven volume
    var cutoffHz = 320.0
        private set
    var distance = 0.4
        private set // distance falloff amount: 0 = flat (no falloff), 1 = silent when the fields sit at the edge

    private var built = false
    private var master: dynamic = null
    private var panner: dynamic = null
    private var filter: dynamic = null

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value && built) ramp(0.0)
    }

    fun setLevel(v: Double) {
        level = v.coerceIn(0.0, 1.0)
    }

    fun setCutoff(hz: Double) {
        cutoffHz = hz.coerceIn(80.0, 2000.0)
        filter?.frequency?.asDynamic()?.setTargetAtTime(cutoffHz, SoundUtil.audioCtx.asDynamic().currentTime, 0.2)
    }

    fun setDistance(v: Double) {
        distance = v.coerceIn(0.0, 1.0)
    }

    fun resetTuning() {
        enabled = true
        level = 0.6
        cutoffHz = 320.0
        distance = 0.4
        if (built) setCutoff(cutoffHz)
    }

    /** Drive the field hum from the live control fields. Call each frame. */
    fun updateFromFields() {
        if (!enabled) {
            if (built) ramp(0.0)
            return
        }
        val fields = World.allFields()
        var totalArea = 0.0
        var cx = 0.0
        var cy = 0.0
        var healthSum = 0.0
        fields.forEach { f ->
            val a = f.calculateArea().toDouble()
            totalArea += a
            val c = centroid(f)
            cx += c.x * a
            cy += c.y * a
            healthSum += fieldHealth(f) * a
        }
        if (totalArea <= 0.0) { // no fields → silent
            if (built) ramp(0.0)
            return
        }
        if (!built) build()
        val coverage = totalArea / playAreaMu() // layered fields can push this past 1 — that's fine, vol clamps
        val vol = (coverage / FULL_AT).coerceIn(0.0, 1.0) // 50% coverage → full volume
        val ccx = cx / totalArea
        val ccy = cy / totalArea
        // Distance falloff: quieter as the field centroid sits further from the play-area centre (tunable amount).
        val dx = ccx - Sim.width / 2.0
        val dy = ccy - Sim.height / 2.0
        val normDist = (sqrt(dx * dx + dy * dy) / Sim.fieldRadius()).coerceIn(0.0, 1.0)
        val distGain = (1.0 - distance * normDist).coerceIn(0.0, 1.0)
        ramp(level * vol * distGain * BOOST)
        // Stereo-pan toward the field centroid's horizontal position.
        val panX = ((ccx - Sim.width / 2.0) / (Sim.width / 2.0)).coerceIn(-1.0, 1.0)
        panner?.pan?.asDynamic()?.setTargetAtTime(panX, SoundUtil.audioCtx.asDynamic().currentTime, 0.2)
        // Field health (mean of each field's 3 portals, area-weighted) shifts the timbre: healthy fields hum
        // brighter/fuller (the base cutoff opens up); weak/decaying fields read duller.
        val health = (healthSum / totalArea).coerceIn(0.0, 1.0)
        filter?.frequency?.asDynamic()?.setTargetAtTime(cutoffHz * (0.55 + 0.9 * health), SoundUtil.audioCtx.asDynamic().currentTime, 0.25)
    }

    // A field's health = the mean resonator health of its three portals (0..1).
    private fun fieldHealth(f: Field): Double =
        (f.origin.calcHealth() + f.primaryAnchor.calcHealth() + f.secondaryAnchor.calcHealth()) / 300.0

    private fun ramp(target: Double) {
        master?.gain?.asDynamic()?.setTargetAtTime(target, SoundUtil.audioCtx.asDynamic().currentTime, RAMP_S)
    }

    private fun centroid(f: Field): Pos = Pos(
        (f.origin.location.x + f.primaryAnchor.location.x + f.secondaryAnchor.location.x) / 3,
        (f.origin.location.y + f.primaryAnchor.location.y + f.secondaryAnchor.location.y) / 3,
    )

    // The play area in MU units (Field.calculateArea() is px²/100), for the coverage ratio.
    private fun playAreaMu(): Double {
        val r = Sim.fieldRadius()
        val px2 = if (Sim.roundField) PI * r * r else Sim.width.toDouble() * Sim.height
        return (px2 / 100.0).coerceAtLeast(1.0)
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
        val pan = ctx.createStereoPanner() // L/R only (no distance attenuation) → loud + steady; aimed at the field centroid
        lp.connect(gain)
        gain.connect(pan)
        pan.connect(Mixer.bus(Mixer.Group.AMBIENT))
        tone(ctx, "sine", SUB_HZ, 0.34, lp)
        tone(ctx, "triangle", RUMBLE_HZ, 0.22, lp)
        tone(ctx, "sawtooth", DRONE_HZ, 0.10, lp, bandpass = 200.0)
        noise(ctx, lp)
        val lfo = ctx.createOscillator()
        lfo.type = "sine"
        lfo.frequency.value = LFO_HZ
        val lg = ctx.createGain()
        lg.gain.value = LFO_DEPTH_HZ
        lfo.connect(lg)
        lg.connect(lp.frequency)
        lfo.start()
        master = gain
        panner = pan
        filter = lp
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
