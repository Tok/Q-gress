package system.audio

import config.OscillatorType
import external.sound.OscillatorNode
import items.level.XmpLevel
import org.khronos.webgl.set
import util.Rng
import util.data.Pos
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Detonation + impact voices, split out of [Sound] (the audio hub): the XMP / Ultra-Strike blasts and their
 * layered explosion, the procedural glass-shatter, the title-screen thunder, and the mod knock-out "plop".
 * Calls back into [Sound] for the shared node factory + master bus — the same pattern the other voices
 * ([KickDrum], [SteamSound], [HackSound]) already use.
 */
object BlastSound {
    private const val SHATTER_MIX = 0.8 // glass-shatter loudness vs the rest of the mix

    // 909-style kick under the detonations (deep, hard, punchy; see [KickDrum]). The Ultra-Strike now
    // carries the old medium-deep XMP boom; the XMP itself goes much deeper + bigger — a huge, distant blast.
    private const val US_KICK_HZ = 85.0 // Ultra-Strike kick (the old XMP boom)
    private const val US_KICK_DECAY = 0.5
    private const val US_KICK_CLICK = 0.18 // a soft beater click
    private const val XMP_KICK_HZ = 48.0 // XMP kick — very deep (sub-bass "whump" of a huge explosion)
    private const val XMP_KICK_DECAY = 1.0 // …and long (the boom rolls out)
    private const val XMP_KICK_CLICK = 0.05 // …with almost no click (a distant blast has no sharp transient)
    private const val BLAST_REVERB_SEND = 0.22 // reverb send for the explosion rumble tail

    /**
     * Procedural glass-shatter (ported from qlippostasis GlassShatterSound): a bright high-passed
     * noise "crack" + a low sine "thud" + a scatter of damped high-sine "tinkles". [heaviness]
     * 0≈one small object, 1≈big; randomised so no two shatters sound the same.
     */
    fun playGlassShatterSound(pos: Pos, heaviness: Double = 0.3, amplitude: Double = 0.7) {
        Mixer.current = Mixer.Group.PORTAL
        if (Sound.isMuted()) return
        val amp = amplitude * SHATTER_MIX // dialed back to sit under the XMP explosion in the mix
        playNoiseCrack(pos, amp, heaviness)
        playThud(pos, amp, heaviness)
        repeat((9 + heaviness * 17).toInt()) { playTinkle(pos, amp) }
    }

    /**
     * Procedural thunder clap for the title-screen lightning (ported from qlippostasis
     * ThunderSynth.TeslaBolt): white noise through a cascaded 2-pole low-pass whose cutoff sweeps from
     * a bright crack down to a low rumble, shaped by a fast-attack / exp-decay envelope + a tanh
     * waveshaper. [pan] = bolt position (−1..1), [decayMult] scales length + depth.
     */
    fun playThunderSound(pan: Double, decayMult: Double = 1.0) {
        if (Sound.isMuted()) return
        val sr = Sound.audioCtx.sampleRate
        val duration = (0.25 * decayMult).coerceIn(0.08, 1.5)
        val len = (duration * sr).toInt().coerceAtLeast(1)
        val buffer = Sound.audioCtx.createBuffer(1, len, sr)
        val data = buffer.getChannelData(0)
        val freqMult = 0.8 + Rng.random() * 0.6
        var blp1 = 0.0
        var blp2 = 0.0
        var i = 0
        while (i < len) {
            val t = i.toDouble() / sr
            val env = if (t < 0.001) t / 0.001 else exp(-t * 18.0 / decayMult)
            val noise = Rng.random() * 2.0 - 1.0
            val sweep = (t / (0.05 * decayMult)).coerceIn(0.0, 1.0)
            val cutoff = 8000.0 * freqMult + (400.0 - 8000.0 * freqMult) * sweep // bright crack → low rumble
            val g = sin(PI * (cutoff / sr).coerceIn(0.001, 0.499))
            blp1 += g * (noise - blp1)
            blp2 += g * (blp1 - blp2)
            var filtered = blp2
            if (t > 0.01 && Rng.random() < 0.04) filtered += Rng.random() * 1.2 - 0.6 // occasional crackle
            val shaped = tanh(filtered * env * 0.8 * 5.0)
            data[i] = (shaped * 0.92).coerceIn(-1.0, 1.0).toFloat()
            i++
        }
        val source = Sound.audioCtx.createBufferSource()
        source.buffer = buffer
        val gainNode = Sound.createStaticGain(1.0) // bolts are loud — basically max
        val panNode = Sound.createStaticPan(pan.coerceIn(-1.0, 1.0))
        source.connect(gainNode)
        gainNode.connect(panNode)
        panNode.connect(Mixer.currentBus())
        source.start()
        source.stop(Sound.now() + duration)
    }

    /**
     * XMP burst: the existing synthetic boom (now pitched to the scale note — level 8 lowest) + noise
     * blast, with a layered "proper" explosion on top tuned to the mushroom animation (see
     * [playXmpExplosion]). Used by the demo + title; the in-game volley uses the lighter overload below.
     */
    fun playXmpSound(pos: Pos, level: Int) {
        Mixer.current = Mixer.Group.WEAPONS
        if (Sound.isMuted()) return
        val amp = 0.6 + level * 0.06
        val note = Sound.noteFor(level) // 65–131 Hz; level 8 is the lowest
        // (1) Synthetic boom: a sine at the scale note sweeping deep down, long decay (the body of the blast).
        val dur = 0.7 + level * 0.05
        val osc = Sound.createExponentialRampOscillator(OscillatorType.SINE, note, note * 0.3, dur)
        val gainNode = Sound.audioCtx.createGain()
        val n = Sound.now()
        gainNode.gain.setValueAtTime(amp, n)
        gainNode.gain.exponentialRampToValueAtTime(Sound.EPS, n + dur)
        Sound.connectVoice(osc, Sound.createPanner(pos), gainNode, n + dur)
        // Low-passed noise "blast" (a rumble that whoomphs down), not the bright hi-hat-like crack.
        playNoiseBlast(pos, amp * 0.85, 0.6 + level * 0.06)
        // (2) The HUGE, distant explosion on top, riding the fireball's life — deep kick + long muffled rumble.
        playXmpExplosion(pos, amp, note, 1.9 + level * 0.12, deep = true)
    }

    /** The in-game volley blip: a tiny on-scale square pip per XMP fired (level 8 lowest). */
    fun playXmpSound(level: XmpLevel, pos: Pos) {
        Mixer.current = Mixer.Group.WEAPONS
        if (Sound.isMuted()) return
        val freq = Sound.noteFor(level.level, octaveUp = 3) // on-scale volley blip (level 8 lowest)
        val osc = Sound.createStaticOscillator(OscillatorType.SQUARE, freq)
        val gain = (0.04 + (level.level * 0.006))
        val duration = 0.005 + (0.001 * level.level)
        Sound.playSound(osc, Sound.createPanner(pos), gain, duration)
    }

    /**
     * Ultra-strike: a **punchy, medium-deep boom** — the character the XMP used to have (the XMP itself is
     * now a far bigger, deeper, more distant blast). A deep 909 kick + sub + a tighter, brighter rumble.
     */
    fun playUltraStrike(pos: Pos) {
        Mixer.current = Mixer.Group.WEAPONS
        if (Sound.isMuted()) return
        playXmpExplosion(pos, 0.95, Sound.noteFor(8), 0.95, deep = false) // deepest scale note; short tight tail
    }

    /** Subtle "plop" when an XMP/Ultra-Strike knocks a mod/shield out of a slot (portal survives). */
    fun playKnockOutSound(pos: Pos) {
        Mixer.current = Mixer.Group.PORTAL
        if (Sound.isMuted()) return
        val n = Sound.now()
        val osc = Sound.createExponentialRampOscillator(OscillatorType.SINE, 430.0, 130.0, 0.11) // quick downward bloop
        val gainNode = Sound.audioCtx.createGain()
        gainNode.gain.setValueAtTime(Sound.EPS, n)
        gainNode.gain.linearRampToValueAtTime(0.11, n + 0.006) // soft pluck
        gainNode.gain.exponentialRampToValueAtTime(Sound.EPS, n + 0.12)
        Sound.connectVoice(osc, Sound.createPanner(pos), gainNode, n + 0.12)
    }

    /**
     * Layered detonation that rides the mushroom animation: an initial broadband crack (the snap), a
     * chest-punch sub at the note, a deep 909 kick, and a long lowpassed rumble tail whose brightness +
     * amplitude decay over [life] (the smoke cooling + dissipating), so the sound rises + fades with it.
     *
     * [deep] = the XMP's HUGE, distant blast: a very deep + long kick, a softer/darker snap, and a more
     * muffled, longer, more reverberant rumble (a giant explosion heard from afar). `false` = the tighter,
     * brighter, medium-deep boom now used by the Ultra-Strike.
     */
    private fun playXmpExplosion(pos: Pos, amplitude: Double, note: Double, life: Double, deep: Boolean) {
        val n = Sound.now()
        // (a) detonation snap — a distant huge blast has little high-end, so it's softer/darker when deep.
        playNoiseCrack(pos, amplitude * (if (deep) 0.22 else 0.55), if (deep) 0.85 else 0.5)
        // (b) sub thump at the note, dropping an octave (longer + heavier for the big one)
        val subDecay = if (deep) 0.7 else 0.4
        decayVoice(
            Sound.createExponentialRampOscillator(OscillatorType.SINE, note, note * 0.5, subDecay),
            pos,
            amplitude * (if (deep) 1.7 else 1.2),
            subDecay,
        )
        // (b2) deep, hard 909 kick at the blast front — the punch/whump that lands the detonation with weight.
        if (deep) {
            KickDrum.play(pos, XMP_KICK_HZ, amplitude * 2.2, XMP_KICK_DECAY, XMP_KICK_CLICK)
        } else {
            KickDrum.play(pos, US_KICK_HZ, amplitude * 1.9, US_KICK_DECAY, US_KICK_CLICK)
        }
        // (c) long rumble tail — fast attack, brightness + level fall over the fireball's life
        val sr = Sound.audioCtx.sampleRate
        val len = (life * sr).toInt().coerceAtLeast(1)
        val buffer = Sound.audioCtx.createBuffer(1, len, sr)
        val data = buffer.getChannelData(0)
        var i = 0
        while (i < len) {
            data[i] = (Rng.random() * 2.0 - 1.0).toFloat()
            i++
        }
        val source = Sound.audioCtx.createBufferSource()
        source.buffer = buffer
        val lowpass = Sound.audioCtx.createBiquadFilter()
        lowpass.type = "lowpass"
        lowpass.frequency.setValueAtTime(if (deep) 600.0 else 1500.0, n) // distant huge blast → muffled, no bright crack
        lowpass.frequency.exponentialRampToValueAtTime(if (deep) 38.0 else 70.0, n + life) // → deep cooling rumble
        val rumbleGain = Sound.audioCtx.createGain()
        rumbleGain.gain.setValueAtTime(Sound.EPS, n)
        rumbleGain.gain.exponentialRampToValueAtTime(amplitude * 1.3, n + 0.03) // fast attack
        rumbleGain.gain.exponentialRampToValueAtTime(amplitude * 0.35, n + life * 0.4)
        rumbleGain.gain.exponentialRampToValueAtTime(Sound.EPS, n + life) // long smoke tail
        val panNode = Sound.createPanner(pos)
        source.connect(lowpass)
        lowpass.connect(rumbleGain)
        rumbleGain.connect(panNode)
        panNode.connect(Mixer.currentBus())
        KickDrum.sendToReverb(rumbleGain, amplitude * (if (deep) 0.45 else BLAST_REVERB_SEND)) // more space on the huge blast
        source.start()
        source.stop(n + life)
    }

    private fun playNoiseBlast(pos: Pos, amplitude: Double, dur: Double) {
        val sr = Sound.audioCtx.sampleRate
        val tau = dur * 0.4
        val len = (dur * sr).toInt().coerceAtLeast(1)
        val buffer = Sound.audioCtx.createBuffer(1, len, sr)
        val data = buffer.getChannelData(0)
        var i = 0
        while (i < len) {
            data[i] = ((Rng.random() * 2.0 - 1.0) * exp(-(i.toDouble() / sr) / tau)).toFloat()
            i++
        }
        val source = Sound.audioCtx.createBufferSource()
        source.buffer = buffer
        val lowpass = Sound.audioCtx.createBiquadFilter()
        lowpass.type = "lowpass"
        val n = Sound.now()
        lowpass.frequency.setValueAtTime(700.0, n)
        lowpass.frequency.exponentialRampToValueAtTime(110.0, n + dur) // brightness falls → a rumble
        val gainNode = Sound.createStaticGain(amplitude)
        val panNode = Sound.createPanner(pos)
        source.connect(lowpass)
        lowpass.connect(gainNode)
        gainNode.connect(panNode)
        panNode.connect(Mixer.currentBus())
        source.start()
        source.stop(n + dur)
    }

    private fun playNoiseCrack(pos: Pos, amplitude: Double, heaviness: Double) {
        val sr = Sound.audioCtx.sampleRate
        val tau = 0.06 + Rng.random() * 0.06 + heaviness * 0.06
        val dur = tau * 5.0
        val len = (dur * sr).toInt().coerceAtLeast(1)
        val buffer = Sound.audioCtx.createBuffer(1, len, sr)
        val data = buffer.getChannelData(0)
        var i = 0
        while (i < len) {
            data[i] = ((Rng.random() * 2.0 - 1.0) * exp(-(i.toDouble() / sr) / tau)).toFloat()
            i++
        }
        val source = Sound.audioCtx.createBufferSource()
        source.buffer = buffer
        val highpass = Sound.audioCtx.createBiquadFilter()
        highpass.type = "highpass"
        highpass.frequency.setValueAtTime(2000.0, Sound.now())
        val gainNode = Sound.createStaticGain(amplitude * 0.8)
        val panNode = Sound.createPanner(pos)
        source.connect(highpass)
        highpass.connect(gainNode)
        gainNode.connect(panNode)
        panNode.connect(Mixer.currentBus())
        source.start()
        source.stop(Sound.now() + dur)
    }

    private fun playThud(pos: Pos, amplitude: Double, heaviness: Double) {
        val tau = 0.07 + Rng.random() * 0.07 + heaviness * 0.06
        val osc = Sound.createStaticOscillator(OscillatorType.SINE, 85.0 + Rng.random() * 80.0)
        val gainNode = Sound.audioCtx.createGain()
        val n = Sound.now()
        gainNode.gain.setValueAtTime((0.6 + heaviness * 0.9) * amplitude, n)
        gainNode.gain.exponentialRampToValueAtTime(Sound.EPS, n + tau * 5.0)
        Sound.connectVoice(osc, Sound.createPanner(pos), gainNode, n + tau * 5.0)
    }

    private fun playTinkle(pos: Pos, amplitude: Double) {
        val tau = 0.02 + Rng.random() * 0.1
        val osc = Sound.createStaticOscillator(OscillatorType.SINE, 2200.0 + Rng.random() * 6800.0)
        val gainNode = Sound.audioCtx.createGain()
        val n = Sound.now() + Rng.random() * 0.5
        gainNode.gain.setValueAtTime(Sound.EPS, Sound.now())
        gainNode.gain.setValueAtTime((0.12 + Rng.random() * 0.33) * amplitude, n)
        gainNode.gain.exponentialRampToValueAtTime(Sound.EPS, n + tau * 5.0)
        Sound.connectVoice(osc, Sound.createPanner(pos), gainNode, n + tau * 5.0)
    }

    /** Play [osc] at [pos] as the common one-shot voice. The master ADSR ([AudioFx.shapeDecay]) shapes the
     *  gain (attack ramp + release-stretched decay); default state reproduces the old peak→silence one-shot. */
    private fun decayVoice(osc: OscillatorNode, pos: Pos, peak: Double, dur: Double) {
        val n = Sound.now()
        val g = Sound.audioCtx.createGain()
        val total = AudioFx.shapeDecay(g.gain, n, peak, dur)
        Sound.connectVoice(osc, Sound.createPanner(pos), g, n + total)
    }
}
