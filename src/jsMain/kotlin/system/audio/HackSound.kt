package system.audio

import agent.Faction
import config.OscillatorType
import org.khronos.webgl.set
import system.display.fx.HackFx
import util.Util
import util.data.Pos
import kotlin.math.PI
import kotlin.math.min

/**
 * Hack / glyph sound, synced to the [system.display.HackFx] collar spin:
 *  - a **centrifuge whoosh** (lowpassed white noise whose cutoff + loudness track the spin → a
 *    "sssSSSsss" that brightens + swells to a peak mid-spin, then dulls + fades — no tonal siren), plus
 *  - **resonator clicks**: a short ping each 1/8 turn the collar passes a *filled* slot, pitched by that
 *    reso's level (higher level = deeper, on the shared scale). Spin DIRECTION reverses the click order
 *    and spin SPEED sets the tempo → a little melody that differs per faction + portal.
 *
 * Keyed by **portal id**: re-hacking before the spin finishes (e.g. RES interrupts ENL, flipping the
 * spin) stops the old voice and starts a fresh one. Lives outside [SoundUtil] (at its size limit).
 */
object HackSound {
    private const val MAX_CLICKS = 64 // safety cap (long high-level glyphs turn many times)
    private const val EIGHTH = PI / 4.0 // one reso slot = 1/8 turn

    private class Voice(val gain: dynamic, val oscs: MutableList<dynamic>)

    private val active = mutableMapOf<String, Voice>()

    /** Normal hack for [id] (portal); [dur] s = the spin's wall-clock length. [slots] = octant→reso level
     *  (0 = empty), [faction] sets the click order/direction. */
    fun hack(id: String, pos: Pos, dur: Double, faction: Faction, slots: IntArray) {
        if (SoundUtil.isMuted()) return
        play(id, pos, dur, faction, slots, glyph = false, level = 0)
    }

    /** Glyph hack: a sharper whoosh (it spins faster) + a glassy completion chime, plus the clicks. */
    fun glyph(id: String, pos: Pos, level: Int, dur: Double, faction: Faction, slots: IntArray) {
        if (SoundUtil.isMuted()) return
        play(id, pos, dur, faction, slots, glyph = true, level = level)
    }

    /** Stop [id]'s current voice with a quick fade (a re-hack interrupted it before it played out). */
    fun stop(id: String) {
        val v = active.remove(id) ?: return
        val n = SoundUtil.now()
        v.gain.gain.cancelScheduledValues(n)
        v.gain.gain.setTargetAtTime(SoundUtil.EPS, n, 0.02) // ~60 ms fade, no click
        v.oscs.forEach { runCatching { it.stop(n + 0.12) } }
    }

    @Suppress("LongParameterList") // the full hack/glyph sound parameters
    private fun play(id: String, pos: Pos, dur: Double, faction: Faction, slots: IntArray, glyph: Boolean, level: Int) {
        Mixer.current = Mixer.Group.PORTAL // hack/glyph → the Portal mixer channel
        stop(id)
        val ctx = SoundUtil.audioCtx
        val n = SoundUtil.now()
        val mid = n + dur * 0.5
        val panner = SoundUtil.createPanner(pos)
        val peak = if (glyph) 0.08 else 0.06 // white noise is perceptually loud — keep it well down
        val highCut = if (glyph) 4800.0 else 3200.0

        // Looped white-noise whoosh through a lowpass whose cutoff (and the gain) track the spin.
        val len = (ctx.sampleRate * 0.5).toInt().coerceAtLeast(1)
        val buffer = ctx.createBuffer(1, len, ctx.sampleRate)
        val data = buffer.getChannelData(0)
        var i = 0
        while (i < len) {
            data[i] = (Util.random() * 2.0 - 1.0).toFloat()
            i++
        }
        val src = ctx.createBufferSource()
        src.buffer = buffer
        src.asDynamic().loop = true
        val lowpass = ctx.createBiquadFilter()
        lowpass.type = "lowpass"
        lowpass.frequency.setValueAtTime(420.0, n)
        lowpass.frequency.linearRampToValueAtTime(highCut, mid) // brightens with spin speed (sss → SSS)
        lowpass.frequency.linearRampToValueAtTime(420.0, n + dur)
        lowpass.asDynamic().Q.value = 1.2
        // Bell envelope: quiet at the ends, loud in the middle (the "sss → SSS → sss"), silent at start/end.
        val gainNode = ctx.createGain()
        gainNode.gain.setValueAtTime(SoundUtil.EPS, n)
        gainNode.gain.linearRampToValueAtTime(peak * 0.12, n + dur * 0.25) // ease in from silence (soft sss)
        gainNode.gain.linearRampToValueAtTime(peak, mid) // swell to the peak (SSS)
        gainNode.gain.linearRampToValueAtTime(peak * 0.12, n + dur * 0.75) // back down (soft sss)
        gainNode.gain.exponentialRampToValueAtTime(SoundUtil.EPS, n + dur) // fade fully out
        src.connect(lowpass)
        lowpass.connect(gainNode)
        gainNode.connect(panner)
        panner.connect(Mixer.currentBus())
        src.start()
        src.stop(n + dur + 0.05)

        val oscs = mutableListOf<dynamic>(src)
        scheduleClicks(oscs, panner, n, dur, faction, slots, glyph)
        if (glyph) oscs.add(chime(panner, level, n, dur))
        active[id] = Voice(gainNode, oscs)
    }

    /** A short ping each 1/8 turn the collar passes a filled slot, pitched by that slot's reso level. */
    @Suppress("LongParameterList") // click scheduling needs the spin + slot context
    private fun scheduleClicks(
        oscs: MutableList<dynamic>,
        panner: dynamic,
        n: Double,
        dur: Double,
        faction: Faction,
        slots: IntArray,
        glyph: Boolean,
    ) {
        val total = HackFx.spinRadians(dur, glyph) // total radians turned
        val dir = if (HackFx.spinSign(faction) >= 0.0) 1 else -1
        val steps = min((total / EIGHTH).toInt(), MAX_CLICKS)
        var j = 1
        while (j <= steps) {
            val slotIdx = ((-dir * j) % 8 + 8) % 8 // which slot reaches the pickup at this 1/8 turn (dir flips order)
            val lvl = slots.getOrElse(slotIdx) { 0 }
            if (lvl > 0) {
                val u = invSmoother((j * EIGHTH) / total) // when the spin reaches j·45° (eased, so tempo tracks speed)
                oscs.add(click(panner, SoundUtil.noteFor(lvl, 3), n + u * dur))
            }
            j++
        }
    }

    private fun click(panner: dynamic, freq: Double, t: Double): dynamic {
        val ctx = SoundUtil.audioCtx
        val osc = ctx.createOscillator()
        osc.type = OscillatorType.TRIANGLE
        osc.frequency.setValueAtTime(freq, t)
        val g = ctx.createGain()
        g.gain.setValueAtTime(SoundUtil.EPS, t)
        g.gain.linearRampToValueAtTime(0.07, t + 0.004) // sharp transient
        g.gain.exponentialRampToValueAtTime(SoundUtil.EPS, t + 0.06)
        osc.connect(g)
        g.connect(panner)
        panner.connect(Mixer.currentBus())
        osc.asDynamic().start(t) // typed start() takes no arg; schedule at t
        osc.stop(t + 0.08)
        return osc
    }

    // Invert smootherstep s(u)=6u⁵−15u⁴+10u³ for u in [0,1] (bisection) so a click lands when the eased
    // spin angle reaches a slot — clicks therefore bunch up at peak speed and thin out at the ends.
    private fun invSmoother(targetFrac: Double): Double {
        val target = targetFrac.coerceIn(0.0, 1.0)
        var lo = 0.0
        var hi = 1.0
        repeat(22) {
            val m = (lo + hi) / 2.0
            val s = m * m * m * (m * (m * 6.0 - 15.0) + 10.0)
            if (s < target) lo = m else hi = m
        }
        return (lo + hi) / 2.0
    }

    /** A glassy chime ringing up as a glyph completes (the "stronger / skill" flourish). */
    private fun chime(panner: dynamic, level: Int, n: Double, dur: Double): dynamic {
        val ctx = SoundUtil.audioCtx
        val ring = ctx.createOscillator()
        ring.type = OscillatorType.SINE
        val rt = n + dur * 0.7
        val note = SoundUtil.noteFor(level, 4)
        ring.frequency.setValueAtTime(note, rt)
        ring.frequency.exponentialRampToValueAtTime(note * 1.5, n + dur)
        val ringGain = ctx.createGain()
        ringGain.gain.setValueAtTime(SoundUtil.EPS, n)
        ringGain.gain.setValueAtTime(SoundUtil.EPS, rt)
        ringGain.gain.linearRampToValueAtTime(0.13, rt + 0.05)
        ringGain.gain.exponentialRampToValueAtTime(SoundUtil.EPS, n + dur + 0.3)
        ring.connect(ringGain)
        ringGain.connect(panner)
        panner.connect(Mixer.currentBus())
        ring.start()
        ring.stop(n + dur + 0.3)
        return ring
    }
}
