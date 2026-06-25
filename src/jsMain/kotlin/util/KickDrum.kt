package util

import config.OscillatorType
import org.khronos.webgl.set
import util.data.Pos
import kotlin.math.exp

/**
 * A deep, hard **TR-909-style kick** for the detonations — the punch under the XMP / Ultra-Strike blasts.
 * A sine whose pitch drops fast from the start freq (the "thwack" body) plus a tight high-passed click
 * (the hard beater attack), with a bit of [reverb send][sendToReverb] for space. Split out of [SoundUtil]
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

    /** Deep hard kick at sim [pos]: pitch drops fast from [startHz], peak [amp], body length [decay] s. */
    fun play(pos: Pos, startHz: Double, amp: Double, decay: Double) {
        val ctx = SoundUtil.audioCtx
        val n = SoundUtil.now()
        val osc = SoundUtil.createStaticOscillator(OscillatorType.SINE, startHz)
        osc.frequency.setValueAtTime(startHz, n)
        osc.frequency.exponentialRampToValueAtTime(maxOf(startHz * PITCH_DROP, MIN_HZ), n + PITCH_S)
        val g = ctx.createGain()
        g.gain.setValueAtTime(SoundUtil.EPS, n)
        g.gain.linearRampToValueAtTime(amp, n + ATTACK_S)
        g.gain.exponentialRampToValueAtTime(SoundUtil.EPS, n + decay)
        val pan = SoundUtil.createPanner(pos)
        osc.connect(pan)
        pan.connect(g)
        g.connect(SoundUtil.masterGain)
        sendToReverb(g, amp * REVERB_SEND)
        osc.start()
        osc.stop(n + decay)
        playClick(pos, amp * CLICK_AMP_FRAC) // the hard transient on top of the body
    }

    /** Tap [node]'s output into the master reverb send bus at [amount] (explosion-only space; see [AudioFx]). */
    fun sendToReverb(node: dynamic, amount: Double) {
        val bus = AudioFx.reverbSend() ?: return
        val g = SoundUtil.audioCtx.createGain()
        g.gain.value = amount
        node.connect(g)
        g.connect(bus)
    }

    // A short high-passed noise tick — the beater "click" that makes the kick read hard/punchy.
    private fun playClick(pos: Pos, amplitude: Double) {
        val ctx = SoundUtil.audioCtx
        val sr = ctx.sampleRate
        val len = (CLICK_S * sr).toInt().coerceAtLeast(1)
        val buffer = ctx.createBuffer(1, len, sr)
        val data = buffer.getChannelData(0)
        var i = 0
        while (i < len) {
            data[i] = ((Util.random() * 2.0 - 1.0) * exp(-(i.toDouble() / sr) / CLICK_TAU)).toFloat()
            i++
        }
        val source = ctx.createBufferSource()
        source.buffer = buffer
        val highpass = ctx.createBiquadFilter()
        highpass.type = "highpass"
        highpass.frequency.setValueAtTime(CLICK_HP_HZ, SoundUtil.now())
        val gainNode = SoundUtil.createStaticGain(amplitude)
        val panNode = SoundUtil.createPanner(pos)
        source.connect(highpass)
        highpass.connect(gainNode)
        gainNode.connect(panNode)
        panNode.connect(SoundUtil.masterGain)
        source.start()
        source.stop(SoundUtil.now() + CLICK_S)
    }
}
