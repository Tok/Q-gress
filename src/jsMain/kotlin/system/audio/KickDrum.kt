package system.audio

import config.OscillatorType
import config.Sim
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set
import util.Rng
import util.data.Pos
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp

/**
 * A deep, hard **TR-909-style kick** for the detonations — the punch under the XMP / Ultra-Strike blasts.
 * A sine whose pitch drops fast from the start freq (the "thwack" body) plus a tight high-passed click
 * (the hard beater attack), with a bit of [reverb send][sendToReverb] for space. Split out of [Sound]
 * (size); reuses its lazy audio graph (context, master gain, panner, oscillator/gain helpers).
 *
 * Part of the wider move toward a **303 / 808 / 909** sound-design palette (see PLAN).
 */
object KickDrum {
    private const val PITCH_DROP = 0.28 // kick pitch ends at this × start (the fast "thwack" envelope)
    private const val PITCH_S = 0.05 // …over this long (fast = punchy)
    private const val MIN_HZ = 30.0 // don't let the pitch drop below this
    private const val ATTACK_S = 0.004 // near-instant attack = hard
    private const val CLICK_AMP_FRAC = 0.5 // click loudness vs the body
    private const val CLICK_S = 0.012 // click length
    private const val CLICK_TAU = 0.002 // click decay time-constant
    private const val CLICK_HP_HZ = 1400.0 // click high-pass (keeps just the beater snap)
    private const val REVERB_SEND = 0.3 // how much of the kick body feeds the reverb (a bit of space)

    // --- Live tuning (AUDIO tab → Instruments → Explosion kick) ----------------------------------------
    // Multipliers on the per-blast base values (so XMP stays deeper than US, but both scale together) + a
    // waveshaper drive. All default-neutral so the kick is unchanged until dialled. Persisted via [InstrumentPrefs].
    var pitchMult = 1.0
        private set
    var decayMult = 1.0
        private set
    var clickMult = 1.0
        private set
    var drive = 0.0
        private set

    fun setPitchMult(v: Double) {
        pitchMult = v.coerceIn(0.4, 2.5)
    }
    fun setDecayMult(v: Double) {
        decayMult = v.coerceIn(0.3, 3.0)
    }
    fun setClickMult(v: Double) {
        clickMult = v.coerceIn(0.0, 3.0)
    }
    fun setDrive(v: Double) {
        drive = v.coerceIn(0.0, 1.0)
    }

    fun resetTuning() {
        pitchMult = 1.0
        decayMult = 1.0
        clickMult = 1.0
        drive = 0.0
    }

    /** Audition the explosion kick (AUDIO tab "Test" button): an XMP-style deep kick at the play-area centre. */
    fun test() {
        Mixer.current = Mixer.Group.WEAPONS
        play(Pos(Sim.width / 2, Sim.height / 2), 48.0, 1.0, 1.0, 0.05)
    }

    /**
     * Deep hard kick at sim [pos]: pitch drops fast from [startHz], peak [amp], body length [decay] s.
     * [clickFrac] sizes the beater click (the sharp transient) — small/0 for a deep boom (XMP), bigger
     * for a tight punch (Ultra-Strike). The live tuning ([pitchMult]/[decayMult]/[clickMult]/[drive]) scales these.
     */
    fun play(pos: Pos, startHz: Double, amp: Double, decay: Double, clickFrac: Double = CLICK_AMP_FRAC) {
        val ctx = Sound.audioCtx
        val n = Sound.now()
        val sHz = startHz * pitchMult
        val dec = decay * decayMult
        val osc = Sound.createStaticOscillator(OscillatorType.SINE, sHz)
        osc.frequency.setValueAtTime(sHz, n)
        osc.frequency.exponentialRampToValueAtTime(maxOf(sHz * PITCH_DROP, MIN_HZ), n + PITCH_S)
        val g = ctx.createGain()
        g.gain.setValueAtTime(Sound.EPS, n)
        g.gain.linearRampToValueAtTime(amp, n + ATTACK_S)
        g.gain.exponentialRampToValueAtTime(Sound.EPS, n + dec)
        val pan = Sound.createPanner(pos)
        // Optional drive: osc → waveshaper → pan (harder, dirtier kick); bypassed at drive 0.
        if (drive > 0.0) {
            val ws = ctx.asDynamic().createWaveShaper()
            ws.curve = driveCurve(drive)
            osc.connect(ws)
            ws.connect(pan)
        } else {
            osc.connect(pan)
        }
        pan.connect(g)
        g.connect(Mixer.currentBus())
        sendToReverb(g, amp * REVERB_SEND)
        osc.start()
        osc.stop(n + dec)
        if (clickFrac * clickMult > 0.0) playClick(pos, amp * clickFrac * clickMult) // the hard transient on top
    }

    private fun driveCurve(amount: Double): Float32Array {
        val k = amount * 60.0
        val n = 256
        val curve = Float32Array(n)
        val deg = PI / 180.0
        for (i in 0 until n) {
            val x = i.toDouble() * 2.0 / n - 1.0
            curve[i] = ((3.0 + k) * x * 20.0 * deg / (PI + k * abs(x))).toFloat()
        }
        return curve
    }

    /** Tap [node]'s output into the master reverb send bus at [amount] (explosion-only space; see [AudioFx]). */
    fun sendToReverb(node: dynamic, amount: Double) {
        val bus = AudioFx.reverbSend() ?: return
        val g = Sound.audioCtx.createGain()
        g.gain.value = amount
        node.connect(g)
        g.connect(bus)
    }

    // A short high-passed noise tick — the beater "click" that makes the kick read hard/punchy.
    private fun playClick(pos: Pos, amplitude: Double) {
        val ctx = Sound.audioCtx
        val sr = ctx.sampleRate
        val len = (CLICK_S * sr).toInt().coerceAtLeast(1)
        val buffer = ctx.createBuffer(1, len, sr)
        val data = buffer.getChannelData(0)
        var i = 0
        while (i < len) {
            data[i] = ((Rng.random() * 2.0 - 1.0) * exp(-(i.toDouble() / sr) / CLICK_TAU)).toFloat()
            i++
        }
        val source = ctx.createBufferSource()
        source.buffer = buffer
        val highpass = ctx.createBiquadFilter()
        highpass.type = "highpass"
        highpass.frequency.setValueAtTime(CLICK_HP_HZ, Sound.now())
        val gainNode = Sound.createStaticGain(amplitude)
        val panNode = Sound.createPanner(pos)
        source.connect(highpass)
        highpass.connect(gainNode)
        gainNode.connect(panNode)
        panNode.connect(Mixer.currentBus())
        source.start()
        source.stop(Sound.now() + CLICK_S)
    }
}
