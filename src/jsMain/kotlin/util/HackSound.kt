package util

import config.OscillatorType
import util.data.Pos
import kotlin.math.min

/**
 * Hack / glyph **centrifuge-whir** sounds — a "sssSSSSsss" that swells in pitch + loudness to a peak
 * mid-spin then eases off, synced to the collar in [system.display.HackFx] spinning up and slowing to
 * a stop. Each whir is keyed by **portal id**: if the same portal is re-hacked before its spin finishes
 * (e.g. RES interrupts an ENL hack, flipping the spin), the previous whir is stopped and a fresh one
 * starts — so the audio always tracks the current spin. Lives outside [SoundUtil] (which is at its size
 * limit) and reuses its audio graph via a few internal members.
 */
object HackSound {
    private class Voice(val osc: dynamic, val gain: dynamic, val chime: dynamic)

    private val active = mutableMapOf<String, Voice>()

    /** Normal hack whir for [id] (portal), [dur] s = the spin's wall-clock length. */
    fun hack(id: String, pos: Pos, level: Int, dur: Double) {
        if (SoundUtil.isMuted()) return
        stop(id)
        active[id] = whir(pos, SoundUtil.noteFor(level, 2), 0.9, 3.0, 0.16, dur, OscillatorType.TRIANGLE, null, null)
    }

    /** Glyph hack: heavier whir (deeper rest, sweeps higher — the collar spins faster) + a completion chime. */
    fun glyph(id: String, pos: Pos, level: Int, dur: Double) {
        if (SoundUtil.isMuted()) return
        stop(id)
        val chime = chime(pos, level, dur)
        active[id] = whir(pos, SoundUtil.noteFor(level, 2), 0.7, 4.0, 0.22, dur, OscillatorType.SAW, 1500.0, chime)
    }

    /** Stop [id]'s current whir with a quick fade (a re-hack interrupted it before it played out). */
    fun stop(id: String) {
        val v = active.remove(id) ?: return
        val n = SoundUtil.now()
        v.gain.gain.cancelScheduledValues(n)
        v.gain.gain.setTargetAtTime(SoundUtil.EPS, n, 0.02) // ~60 ms fade, no click
        runCatching { v.osc.stop(n + 0.12) }
        if (v.chime != null) runCatching { v.chime.stop(n + 0.12) }
    }

    @Suppress("LongParameterList") // a parametric whir voice
    private fun whir(
        pos: Pos,
        base: Double,
        lowMul: Double,
        highMul: Double,
        peakGain: Double,
        dur: Double,
        type: String,
        lowpassHz: Double?,
        chime: dynamic,
    ): Voice {
        val ctx = SoundUtil.audioCtx
        val n = SoundUtil.now()
        val mid = n + dur * 0.5
        val panner = SoundUtil.createPanner(pos)
        val osc = ctx.createOscillator()
        osc.type = type
        osc.frequency.setValueAtTime(base * lowMul, n)
        osc.frequency.exponentialRampToValueAtTime(base * highMul, mid) // spins up → pitch rises
        osc.frequency.exponentialRampToValueAtTime(base * lowMul, n + dur) // slows to a stop → pitch falls
        val gainNode = ctx.createGain()
        gainNode.gain.setValueAtTime(SoundUtil.EPS, n)
        gainNode.gain.linearRampToValueAtTime(peakGain * 0.45, n + min(0.12, dur * 0.15)) // attack in
        gainNode.gain.linearRampToValueAtTime(peakGain, mid) // loudest at peak spin
        gainNode.gain.linearRampToValueAtTime(peakGain * 0.4, n + dur * 0.9)
        gainNode.gain.exponentialRampToValueAtTime(SoundUtil.EPS, n + dur)
        if (lowpassHz != null) {
            val lowpass = ctx.createBiquadFilter()
            lowpass.type = "lowpass"
            lowpass.frequency.setValueAtTime(lowpassHz, n)
            osc.connect(lowpass)
            lowpass.connect(gainNode)
        } else {
            osc.connect(gainNode)
        }
        gainNode.connect(panner)
        panner.connect(SoundUtil.masterGain)
        osc.start()
        osc.stop(n + dur + 0.05)
        return Voice(osc, gainNode, chime)
    }

    /** A glassy chime ringing up as a glyph completes (the "stronger / skill" flourish). */
    private fun chime(pos: Pos, level: Int, dur: Double): dynamic {
        val ctx = SoundUtil.audioCtx
        val n = SoundUtil.now()
        val panner = SoundUtil.createPanner(pos)
        val ring = ctx.createOscillator()
        ring.type = OscillatorType.SINE
        val rt = n + dur * 0.7 // rings near completion
        val note = SoundUtil.noteFor(level, 4) // bright on-scale flourish
        ring.frequency.setValueAtTime(note, rt)
        ring.frequency.exponentialRampToValueAtTime(note * 1.5, n + dur)
        val ringGain = ctx.createGain()
        ringGain.gain.setValueAtTime(SoundUtil.EPS, n)
        ringGain.gain.setValueAtTime(SoundUtil.EPS, rt)
        ringGain.gain.linearRampToValueAtTime(0.13, rt + 0.05)
        ringGain.gain.exponentialRampToValueAtTime(SoundUtil.EPS, n + dur + 0.3)
        ring.connect(ringGain)
        ringGain.connect(panner)
        panner.connect(SoundUtil.masterGain)
        ring.start()
        ring.stop(n + dur + 0.3)
        return ring
    }
}
