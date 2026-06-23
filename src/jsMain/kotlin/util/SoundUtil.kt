package util

import World
import agent.Faction
import agent.NonFaction
import config.Config
import config.Constants
import config.Dim
import config.OscillatorType
import config.Sim
import external.sound.AudioContext
import external.sound.AudioNode
import external.sound.GainNode
import external.sound.OscillatorNode
import external.sound.PannerNode
import external.sound.StereoPannerNode
import items.level.XmpLevel
import org.khronos.webgl.set
import portal.Field
import portal.Link
import system.Checkpoint
import system.display.Scene3D
import util.data.Pos
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

object SoundUtil {
    const val DEFAULT_VOLUME = 1.0
    internal const val EPS = 0.0001 // exponentialRamp can't target 0
    private const val SHATTER_MIX = 0.8 // glass-shatter loudness vs the rest of the mix

    // The shared musical scale (level → note, major/minor on the lead) lives in [Scale].
    internal fun noteFor(level: Int, octaveUp: Int = 0) = Scale.noteFor(level, octaveUp)

    // 3D-audio tuning (sim-space is real metres). The play area is hundreds of metres across, so a
    // small reference distance + gentle rolloff makes near/far audibly differ at gameplay zoom while
    // distant events still carry. The listener (camera) sits well above, so Z mostly adds elevation.
    private const val SOUND_Z = 1.6 // head height in metres

    // The listener rides the camera, which sits hundreds–thousands of metres from the action at gameplay
    // zoom, so a small ref distance + steep rolloff crushed everything to near-silence. A large ref
    // distance + gentle rolloff keeps distant events audible while near/far still differ.
    private const val REF_DISTANCE = 300.0
    private const val MAX_DISTANCE = 12000.0
    private const val ROLLOFF = 0.5
    private const val PANNING_MODEL = "HRTF" // front/back + elevation cues (vs cheaper "equalpower")
    private const val MASTER_BOOST = 2.6 // lift the whole bus (3D attenuation made it quiet); limiter guards clipping
    private const val DEEP_THUMP_HZ = 48.0 // fixed deep sub-kick under the XMP explosion (adds weight)
    private const val MUFFLE_CLOSED_HZ = 600.0 // muffled: distant/underwater (title behind onboarding)

    internal val audioCtx = AudioContext()
    private val listener = audioCtx.listener

    // Master limiter on the way to the speakers: lets us boost the bus without harsh clipping when many
    // voices stack. (DynamicsCompressor configured as a brick-wall-ish limiter.)
    private val limiter: dynamic = run {
        val c = audioCtx.asDynamic().createDynamicsCompressor()
        c.threshold.value = -6.0
        c.knee.value = 6.0
        c.ratio.value = 12.0
        c.attack.value = 0.003
        c.release.value = 0.12
        c.connect(audioCtx.destination)
        c
    }

    // Single master gain all sounds route through; controls overall volume (× MASTER_BOOST into the
    // limiter). The master FX bus (low/high-pass + reverb send — see AudioFx) sits between it + the limiter.
    internal val masterGain: GainNode = audioCtx.createGain().also {
        it.gain.value = 0.0
        AudioFx.build(audioCtx.asDynamic(), it.asDynamic(), limiter)
    }

    /** Muffle (lowpass) the whole mix, or open it back up — used to push the title audio behind onboarding. */
    fun setMuffled(on: Boolean) {
        AudioFx.setLowpass(if (on) MUFFLE_CLOSED_HZ else AudioFx.LOWPASS_OPEN_HZ)
    }

    // Master volume in 0..1. Starts muted; enabled on the first user gesture
    // (browser autoplay policy needs one). The volume slider drives it too.
    private var masterVolume = 0.0

    /** Resume the audio context and turn sound on. Idempotent; call on a user gesture. */
    fun enableAudio() {
        if (audioCtx.state != "running") audioCtx.resume()
        if (masterVolume <= 0.0) setMasterVolume(DEFAULT_VOLUME)
    }

    fun setMasterVolume(volume: Double) {
        if (audioCtx.state != "running") audioCtx.resume()
        masterVolume = volume
        masterGain.gain.setTargetAtTime(volume * MASTER_BOOST, now(), 0.01)
    }

    internal fun isMuted() = masterVolume <= 0.0
    private var preMuteVolume = 0.0

    /** Toggle mute, remembering the prior level. Returns the new volume (0 when muted). */
    fun toggleMute(): Double {
        if (isMuted()) {
            setMasterVolume(if (preMuteVolume > 0.0) preMuteVolume else DEFAULT_VOLUME)
        } else {
            preMuteVolume = masterVolume
            setMasterVolume(0.0)
        }
        return masterVolume
    }

    fun masterVolume() = masterVolume

    /**
     * Place the Web Audio listener at the camera (sim-space metres) — called every frame from
     * [Scene3D]. Forward/up arrive un-normalised and are normalised here; a degenerate/pre-first
     * frame keeps the last orientation.
     */
    fun updateListener(eye: DoubleArray, forward: DoubleArray, up: DoubleArray) {
        val fl = sqrt(forward[0] * forward[0] + forward[1] * forward[1] + forward[2] * forward[2])
        val ul = sqrt(up[0] * up[0] + up[1] * up[1] + up[2] * up[2])
        if (fl < EPS || ul < EPS) return
        listener.positionX.value = eye[0]
        listener.positionY.value = eye[1]
        listener.positionZ.value = eye[2]
        listener.forwardX.value = forward[0] / fl
        listener.forwardY.value = forward[1] / fl
        listener.forwardZ.value = forward[2] / fl
        listener.upX.value = up[0] / ul
        listener.upY.value = up[1] / ul
        listener.upZ.value = up[2] / ul
    }

    fun playNoiseGenSound() {
        if (!Config.isPlayInitialSound || isMuted()) return
        val freq = 330
        val osc = createNoiseOscillator(freq)
        playSound(osc, createNoisePan(), 0.15, 13.0)
    }

    fun playOffScreenLocationCreationSound() {
        val center = Pos(Sim.width / 2, Sim.height / 2)
        return playPortalCreationSound(center, 0.5)
    }

    fun playPortalCreationSound(pos: Pos, gain: Double = 1.0) {
        if (isMuted()) return
        val duration = 0.5
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 120.0, 0.0, duration)
        playSound(oscNode, createPanner(pos), gain, duration)
    }

    fun playPortalRemovalSound(pos: Pos) {
        if (isMuted()) return
        val duration = 0.5
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 60.0, 120.0, duration)
        playSound(oscNode, createPanner(pos), 1.0, duration)
    }

    /**
     * Procedural glass-shatter (ported from qlippostasis GlassShatterSound): a bright high-passed
     * noise "crack" + a low sine "thud" + a scatter of damped high-sine "tinkles". [heaviness]
     * 0≈one small object, 1≈big; randomised so no two shatters sound the same.
     */
    fun playGlassShatterSound(pos: Pos, heaviness: Double = 0.3, amplitude: Double = 0.7) {
        if (isMuted()) return
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
        if (isMuted()) return
        val sr = audioCtx.sampleRate
        val duration = (0.25 * decayMult).coerceIn(0.08, 1.5)
        val len = (duration * sr).toInt().coerceAtLeast(1)
        val buffer = audioCtx.createBuffer(1, len, sr)
        val data = buffer.getChannelData(0)
        val freqMult = 0.8 + Util.random() * 0.6
        var blp1 = 0.0
        var blp2 = 0.0
        var i = 0
        while (i < len) {
            val t = i.toDouble() / sr
            val env = if (t < 0.001) t / 0.001 else exp(-t * 18.0 / decayMult)
            val noise = Util.random() * 2.0 - 1.0
            val sweep = (t / (0.05 * decayMult)).coerceIn(0.0, 1.0)
            val cutoff = 8000.0 * freqMult + (400.0 - 8000.0 * freqMult) * sweep // bright crack → low rumble
            val g = sin(PI * (cutoff / sr).coerceIn(0.001, 0.499))
            blp1 += g * (noise - blp1)
            blp2 += g * (blp1 - blp2)
            var filtered = blp2
            if (t > 0.01 && Util.random() < 0.04) filtered += Util.random() * 1.2 - 0.6 // occasional crackle
            val shaped = tanh(filtered * env * 0.8 * 5.0)
            data[i] = (shaped * 0.92).coerceIn(-1.0, 1.0).toFloat()
            i++
        }
        val source = audioCtx.createBufferSource()
        source.buffer = buffer
        val gainNode = createStaticGain(1.0) // bolts are loud — basically max
        val panNode = createStaticPan(pan.coerceIn(-1.0, 1.0))
        source.connect(gainNode)
        gainNode.connect(panNode)
        panNode.connect(masterGain)
        source.start()
        source.stop(now() + duration)
    }

    /**
     * XMP burst: the existing synthetic boom (now pitched to the scale note — level 8 lowest) + noise
     * blast, with a layered "proper" explosion on top tuned to the mushroom animation (see
     * [playXmpExplosion]). Used by the demo + title; the in-game volley uses the lighter overload below.
     */
    fun playXmpSound(pos: Pos, level: Int) {
        if (isMuted()) return
        val amp = 0.5 + level * 0.06
        val note = noteFor(level) // 65–131 Hz; level 8 is the lowest
        // (1) Synthetic boom: a sine at the scale note sweeping down, with a longer decay.
        val dur = 0.5 + level * 0.04
        val osc = createExponentialRampOscillator(OscillatorType.SINE, note, note * 0.4, dur)
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime(amp, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur)
        connectVoice(osc, createPanner(pos), gainNode, n + dur)
        // Low-passed noise "blast" (a rumble that whoomphs down), not the bright hi-hat-like crack.
        playNoiseBlast(pos, amp * 0.9, 0.45 + level * 0.05)
        // (2) Proper explosion on top, tuned to the fireball's life (≈ XmpBurst LIFE_BASE + level·0.1).
        playXmpExplosion(pos, amp, note, 1.4 + level * 0.1)
    }

    /** A short rising 3-note jingle when one of the player's agents levels up (level 5→3→1 on the scale). */
    fun playLevelUp(pos: Pos) {
        if (isMuted()) return
        val n = now()
        val osc = createStaticOscillator(OscillatorType.TRIANGLE, noteFor(5, octaveUp = 2))
        osc.frequency.setValueAtTime(noteFor(5, octaveUp = 2), n)
        osc.frequency.setValueAtTime(noteFor(3, octaveUp = 2), n + 0.09)
        osc.frequency.setValueAtTime(noteFor(1, octaveUp = 2), n + 0.18)
        val g = audioCtx.createGain()
        g.gain.setValueAtTime(0.32, n)
        g.gain.setValueAtTime(0.32, n + 0.26)
        g.gain.exponentialRampToValueAtTime(EPS, n + 0.4)
        connectVoice(osc, createPanner(pos), g, n + 0.4)
    }

    /**
     * Ultra-strike: a short, sharp, **punchy** hit — distinct from the XMP's long boom. A tight crack +
     * a quick high "pew" body + a snappy sub punch, all in ~0.12s (no long rumble tail).
     */
    fun playUltraStrike(pos: Pos) {
        if (isMuted()) return
        playNoiseCrack(pos, 0.9, 0.05) // sharp, short crack
        decayVoice(createExponentialRampOscillator(OscillatorType.SQUARE, 440.0, 90.0, 0.1), pos, 0.5, 0.1) // high→low "pew"
        decayVoice(createExponentialRampOscillator(OscillatorType.SINE, 150.0, 60.0, 0.12), pos, 0.85, 0.12) // tight sub punch
    }

    /**
     * Layered detonation that rides the mushroom animation: an initial broadband crack (the snap), a
     * chest-punch sub at the note, and a long lowpassed rumble tail whose brightness falls + amplitude
     * decays over [life] (the smoke cooling + dissipating), so the sound rises and fades with the visual.
     */
    private fun playXmpExplosion(pos: Pos, amplitude: Double, note: Double, life: Double) {
        val n = now()
        playNoiseCrack(pos, amplitude * 0.7, 0.5) // (a) detonation snap (tamed — the blast read too bright/high)
        // (b) sub thump at the note, dropping an octave, quick decay
        decayVoice(createExponentialRampOscillator(OscillatorType.SINE, note, note * 0.5, 0.4), pos, amplitude * 1.2, 0.4)
        // (b2) deep body thump — a fixed, loud low sub-kick UNDER the noise so the blast lands with weight.
        decayVoice(createExponentialRampOscillator(OscillatorType.SINE, DEEP_THUMP_HZ, DEEP_THUMP_HZ * 0.6, 0.3), pos, amplitude * 1.9, 0.3)
        // (c) long rumble tail — fast attack, brightness + level fall over the fireball's life
        val sr = audioCtx.sampleRate
        val len = (life * sr).toInt().coerceAtLeast(1)
        val buffer = audioCtx.createBuffer(1, len, sr)
        val data = buffer.getChannelData(0)
        var i = 0
        while (i < len) {
            data[i] = (Util.random() * 2.0 - 1.0).toFloat()
            i++
        }
        val source = audioCtx.createBufferSource()
        source.buffer = buffer
        val lowpass = audioCtx.createBiquadFilter()
        lowpass.type = "lowpass"
        lowpass.frequency.setValueAtTime(1500.0, n)
        lowpass.frequency.exponentialRampToValueAtTime(70.0, n + life) // bright blast → deep cooling rumble
        val rumbleGain = audioCtx.createGain()
        rumbleGain.gain.setValueAtTime(EPS, n)
        rumbleGain.gain.exponentialRampToValueAtTime(amplitude * 1.3, n + 0.03) // fast attack
        rumbleGain.gain.exponentialRampToValueAtTime(amplitude * 0.35, n + life * 0.4)
        rumbleGain.gain.exponentialRampToValueAtTime(EPS, n + life) // long smoke tail
        val panNode = createPanner(pos)
        source.connect(lowpass)
        lowpass.connect(rumbleGain)
        rumbleGain.connect(panNode)
        panNode.connect(masterGain)
        source.start()
        source.stop(n + life)
    }

    private fun playNoiseBlast(pos: Pos, amplitude: Double, dur: Double) {
        val sr = audioCtx.sampleRate
        val tau = dur * 0.4
        val len = (dur * sr).toInt().coerceAtLeast(1)
        val buffer = audioCtx.createBuffer(1, len, sr)
        val data = buffer.getChannelData(0)
        var i = 0
        while (i < len) {
            data[i] = ((Util.random() * 2.0 - 1.0) * exp(-(i.toDouble() / sr) / tau)).toFloat()
            i++
        }
        val source = audioCtx.createBufferSource()
        source.buffer = buffer
        val lowpass = audioCtx.createBiquadFilter()
        lowpass.type = "lowpass"
        val n = now()
        lowpass.frequency.setValueAtTime(700.0, n)
        lowpass.frequency.exponentialRampToValueAtTime(110.0, n + dur) // brightness falls → a rumble
        val gainNode = createStaticGain(amplitude)
        val panNode = createPanner(pos)
        source.connect(lowpass)
        lowpass.connect(gainNode)
        gainNode.connect(panNode)
        panNode.connect(masterGain)
        source.start()
        source.stop(n + dur)
    }

    private fun playNoiseCrack(pos: Pos, amplitude: Double, heaviness: Double) {
        val sr = audioCtx.sampleRate
        val tau = 0.06 + Util.random() * 0.06 + heaviness * 0.06
        val dur = tau * 5.0
        val len = (dur * sr).toInt().coerceAtLeast(1)
        val buffer = audioCtx.createBuffer(1, len, sr)
        val data = buffer.getChannelData(0)
        var i = 0
        while (i < len) {
            data[i] = ((Util.random() * 2.0 - 1.0) * exp(-(i.toDouble() / sr) / tau)).toFloat()
            i++
        }
        val source = audioCtx.createBufferSource()
        source.buffer = buffer
        val highpass = audioCtx.createBiquadFilter()
        highpass.type = "highpass"
        highpass.frequency.setValueAtTime(2000.0, now())
        val gainNode = createStaticGain(amplitude * 0.8)
        val panNode = createPanner(pos)
        source.connect(highpass)
        highpass.connect(gainNode)
        gainNode.connect(panNode)
        panNode.connect(masterGain)
        source.start()
        source.stop(now() + dur)
    }

    private fun playThud(pos: Pos, amplitude: Double, heaviness: Double) {
        val tau = 0.07 + Util.random() * 0.07 + heaviness * 0.06
        val osc = createStaticOscillator(OscillatorType.SINE, 85.0 + Util.random() * 80.0)
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime((0.6 + heaviness * 0.9) * amplitude, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + tau * 5.0)
        connectVoice(osc, createPanner(pos), gainNode, n + tau * 5.0)
    }

    private fun playTinkle(pos: Pos, amplitude: Double) {
        val tau = 0.02 + Util.random() * 0.1
        val osc = createStaticOscillator(OscillatorType.SINE, 2200.0 + Util.random() * 6800.0)
        val gainNode = audioCtx.createGain()
        val n = now() + Util.random() * 0.5
        gainNode.gain.setValueAtTime(EPS, now())
        gainNode.gain.setValueAtTime((0.12 + Util.random() * 0.33) * amplitude, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + tau * 5.0)
        connectVoice(osc, createPanner(pos), gainNode, n + tau * 5.0)
    }

    private fun connectVoice(osc: OscillatorNode, panNode: AudioNode, gainNode: GainNode, stopTime: Double) {
        osc.connect(panNode)
        panNode.connect(gainNode)
        gainNode.connect(masterGain)
        osc.start()
        osc.stop(stopTime)
    }

    /** Play [osc] at [pos] with a peak→silence exponential decay over [dur] (the common one-shot voice). */
    private fun decayVoice(osc: OscillatorNode, pos: Pos, peak: Double, dur: Double) {
        val n = now()
        val g = audioCtx.createGain()
        g.gain.setValueAtTime(peak, n)
        g.gain.exponentialRampToValueAtTime(EPS, n + dur)
        connectVoice(osc, createPanner(pos), g, n + dur)
    }

    fun playCheckpointSound(@Suppress("UNUSED_PARAMETER") checkpoint: Checkpoint) {
        if (isMuted()) return
        val duration = 0.05
        val pan = 0.0 // centre
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 440.0, 440.0, duration)
        playSound(oscNode, createStaticPan(pan), 0.5, duration)
    }

    fun playFailSound() {
        if (isMuted()) return
        val freq = 220.0
        val osc = createStaticOscillator(OscillatorType.SINE, freq)
        playSound(osc, createNoisePan(), 0.1, 0.5)
    }

    fun playCycleSound() {
        if (isMuted()) return
        val duration = 0.01
        val pan = 0.0 // centre
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 220.0, 220.0, duration)
        playSound(oscNode, createStaticPan(pan), 0.5, duration)
    }

    /** A marble "tok": a short triangle with a quick downward pitch drop — the NPC dropping in. */
    fun playNpcCreationSound(npc: NonFaction) {
        if (isMuted()) return
        val duration = 0.05
        val sizePitch = 1.0 - npc.size.offset * 0.18 // smaller NPC → higher-pitched marble
        val start = 1150.0 * sizePitch
        val end = 480.0 * sizePitch // fast downward chirp = a marble tap
        val oscNode = createExponentialRampOscillator(OscillatorType.TRIANGLE, start, end, duration)
        playSound(oscNode, createPanner(npc.pos), 0.22, duration)
    }

    /** Portal gained a level: a quick note rising up to the NEW [level]'s note on the shared scale. */
    fun playUpgradeSound(pos: Pos, level: Int) {
        if (isMuted()) return
        val dur = 0.18
        val target = noteFor(level, octaveUp = 3)
        val osc = createExponentialRampOscillator(OscillatorType.SINE, target * 0.67, target, dur)
        playSound(osc, createPanner(pos), 0.08, dur)
    }

    /** Portal lost a level: a quick note falling down to the NEW [level]'s note on the shared scale. */
    fun playDowngradeSound(pos: Pos, level: Int) {
        if (isMuted()) return
        val dur = 0.2
        val target = noteFor(level, octaveUp = 3)
        val osc = createExponentialRampOscillator(OscillatorType.SINE, target * 1.5, target, dur)
        playSound(osc, createPanner(pos), 0.08, dur)
    }

    /** Portal neutralized (lost its owner): a short descending "power-down" sweep. */
    fun playNeutralizeSound(pos: Pos) {
        if (isMuted()) return
        val dur = 0.5
        val osc = createExponentialRampOscillator(OscillatorType.SAW, 440.0, 90.0, dur)
        val n = now()
        val gainNode = audioCtx.createGain()
        gainNode.gain.setValueAtTime(0.12, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur)
        connectVoice(osc, createPanner(pos), gainNode, n + dur)
    }

    fun playXmpSound(level: XmpLevel, pos: Pos) {
        if (isMuted()) return
        val freq = noteFor(level.level, octaveUp = 3) // on-scale volley blip (level 8 lowest)
        val osc = createStaticOscillator(OscillatorType.SQUARE, freq)
        val gain = (0.04 + (level.level * 0.006))
        val duration = 0.005 + (0.001 * level.level)
        playSound(osc, createPanner(pos), gain, duration)
    }

    /** A short bell-like "ding" as a single resonator drops into its slot (on the shared scale). */
    fun playResoDeploySound(pos: Pos, level: Int) {
        if (isMuted()) return
        val freq = noteFor(level, octaveUp = 3) // bright + on-key (level 8 = lowest)
        val osc = createStaticOscillator(OscillatorType.SINE, freq)
        val gainNode = audioCtx.createGain()
        val n = now()
        val dur = 0.22
        gainNode.gain.setValueAtTime(EPS, n)
        gainNode.gain.exponentialRampToValueAtTime(0.16, n + 0.005) // fast attack → the "ding"
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur) // quick bell decay
        connectVoice(osc, createPanner(pos), gainNode, n + dur)
    }

    /** A metallic "clunk" when a mod (shield / heat sink) is slotted into a portal. */
    fun playModDeploySound(pos: Pos, level: Int) {
        if (isMuted()) return
        val osc = createStaticOscillator(OscillatorType.SQUARE, noteFor(level, octaveUp = 2))
        val gainNode = audioCtx.createGain()
        val n = now()
        val dur = 0.16
        gainNode.gain.setValueAtTime(0.10, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur)
        connectVoice(osc, createPanner(pos), gainNode, n + dur)
    }

    /** A bright shimmering "shing" as a shield powers up on the portal. */
    fun playShieldDeploySound(pos: Pos, level: Int) {
        if (isMuted()) return
        val base = noteFor(level, octaveUp = 3)
        val osc = createLinearRampOscillator(OscillatorType.TRIANGLE, base, base * 1.5, 0.35) // rises into place
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime(EPS, n)
        gainNode.gain.exponentialRampToValueAtTime(0.13, n + 0.02)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + 0.4) // gentle ring-out
        connectVoice(osc, createPanner(pos), gainNode, n + 0.4)
    }

    /** A descending "power-down" as a shield collapses / is stripped off. */
    fun playShieldRemoveSound(pos: Pos, level: Int) {
        if (isMuted()) return
        val base = noteFor(level, octaveUp = 3)
        val osc = createLinearRampOscillator(OscillatorType.TRIANGLE, base * 1.5, base * 0.5, 0.35) // falls away
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime(0.12, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + 0.35)
        connectVoice(osc, createPanner(pos), gainNode, n + 0.35)
    }

    /** A glitchy faction-pitched sweep when a virus (ADA / JARVIS) flips a portal. */
    fun playVirusSound(pos: Pos, faction: Faction) {
        if (isMuted()) return
        val base = if (faction == Faction.ENL) 180.0 else 140.0
        val osc = createLinearRampOscillator(OscillatorType.SQUARE, base, base * 4.0, VIRUS_DUR)
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime(0.14, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + VIRUS_DUR)
        connectVoice(osc, createPanner(pos), gainNode, n + VIRUS_DUR)
    }

    private const val VIRUS_DUR = 0.5

    fun playDeploySound(pos: Pos, distanceToPortal: Int) {
        if (isMuted()) return
        val ratio = distanceToPortal / Dim.maxDeploymentRange
        val gain = 0.10
        val duration = 0.2
        val minFreq = 250.0
        val baseFreq = -250.0
        val startFreq = minFreq + (baseFreq * ratio)
        val endFreq = minFreq + (baseFreq * ratio * 2)
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, startFreq, endFreq, duration)
        playSound(oscNode, createPanner(pos), gain, duration)
    }

    fun playLinkingSound(link: Link) {
        if (isMuted()) return
        val ratio = link.getLine().length() / World.diagonalLength()
        val gain = 0.30
        val duration = 0.04 + (0.16 * ratio)
        val minFreq = 500.0 * ratio
        val baseFreq = 500.0
        val startFreq = minFreq + (baseFreq * ratio)
        val endFreq = minFreq + (baseFreq * ratio * 2)
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, startFreq, endFreq, duration)
        val panNode = createPannerRamp(link.getLine().from, link.getLine().to, duration)
        playSound(oscNode, panNode, gain, duration)
    }

    /** Field collapse (teardown): a short downward sweep that decays away. */
    fun playFieldDownSound() {
        if (isMuted()) return
        val dur = 0.5
        val osc = createExponentialRampOscillator(OscillatorType.TRIANGLE, 110.0, 28.0, dur)
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime(0.35, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur)
        connectVoice(osc, createStaticPan(0.0), gainNode, n + dur)
    }

    fun playFieldingSound(field: Field) {
        if (isMuted()) return
        val areaRatio = field.calculateArea() / World.totalArea()
        val gain = 0.4
        val minDuration = 1.0 / Constants.phi
        val maxDuration = 1.0
        val diff = maxDuration - minDuration
        val additionalDuration = diff * areaRatio
        val duration = minDuration + additionalDuration
        val minFreq = 70.0
        val baseFreq = 20.0
        val startFreq = minFreq + (baseFreq * areaRatio)
        val endFreq = startFreq * 2.0
        val midAnchor = Pos(
            (field.primaryAnchor.x() + field.secondaryAnchor.x()) / 2,
            (field.primaryAnchor.y() + field.secondaryAnchor.y()) / 2,
        )
        val oscNode = createExponentialRampOscillator(OscillatorType.TRIANGLE, startFreq, endFreq, duration)
        val panNode = createPannerRamp(field.origin.location, midAnchor, duration)
        playSound(oscNode, panNode, gain, duration)
    }

    private fun playSound(oscNode: OscillatorNode, panNode: AudioNode, gain: Double, duration: Double) {
        val gainNode = createStaticGain(gain)
        oscNode.connect(panNode)
        panNode.connect(gainNode)
        gainNode.connect(masterGain)
        oscNode.start()
        oscNode.stop(now() + duration)
    }

    internal fun now() = audioCtx.currentTime.toDouble()

    private fun createStaticOscillator(type: String, freq: Double): OscillatorNode {
        val node = audioCtx.createOscillator()
        node.type = type
        node.frequency.setTargetAtTime(freq, now(), 0.0)
        return node
    }

    private fun createNoiseOscillator(maxFreq: Int): OscillatorNode {
        val node = audioCtx.createOscillator()
        node.type = OscillatorType.SQUARE
        val n = now()
        val timeConstant = 0.01
        val max = 1000
        (0..max).forEach {
            val freq = Util.random() * (maxFreq - (maxFreq * it / max))
            val tc = timeConstant * it
            node.frequency.setTargetAtTime(freq, n + tc, timeConstant)
        }
        return node
    }

    private fun createLinearRampOscillator(type: String, startFreq: Double, endFreq: Double, duration: Double): OscillatorNode {
        val node = createStaticOscillator(type, startFreq)
        node.frequency.linearRampToValueAtTime(endFreq, now() + duration)
        return node
    }

    private fun createExponentialRampOscillator(type: String, startFreq: Double, endFreq: Double, duration: Double): OscillatorNode {
        val node = createStaticOscillator(type, startFreq)
        node.frequency.exponentialRampToValueAtTime(endFreq, now() + duration)
        return node
    }

    private fun createStaticPan(pan: Double): StereoPannerNode {
        val node = audioCtx.createStereoPanner()
        node.pan.setTargetAtTime(pan, now(), 0.0)
        return node
    }

    private fun createNoisePan(): StereoPannerNode {
        val node = audioCtx.createStereoPanner()
        val timeConstant = 0.01
        val max = 1000
        val n = now()
        (0..max).forEach {
            val pan = Util.random() * 2.0 - 1.0 // full −1…+1 stereo field
            val tc = timeConstant * it
            node.pan.setTargetAtTime(pan, n + tc, timeConstant)
        }

        return node
    }

    /** A positional source at sim [pos] (metres), spatialized relative to the camera listener. */
    internal fun createPanner(pos: Pos): PannerNode {
        val node = audioCtx.createPanner()
        node.panningModel = PANNING_MODEL
        node.distanceModel = "inverse"
        node.refDistance = REF_DISTANCE
        node.maxDistance = MAX_DISTANCE
        node.rolloffFactor = ROLLOFF
        node.positionX.value = Scene3D.sceneX(pos)
        node.positionY.value = Scene3D.sceneY(pos)
        node.positionZ.value = SOUND_Z
        return node
    }

    /** A panner that travels from [from] to [to] over [duration] — e.g. a link / field sweep. */
    private fun createPannerRamp(from: Pos, to: Pos, duration: Double): PannerNode {
        val node = createPanner(from)
        val end = now() + duration
        node.positionX.setValueAtTime(Scene3D.sceneX(from), now())
        node.positionY.setValueAtTime(Scene3D.sceneY(from), now())
        node.positionX.linearRampToValueAtTime(Scene3D.sceneX(to), end)
        node.positionY.linearRampToValueAtTime(Scene3D.sceneY(to), end)
        return node
    }

    private fun createStaticGain(gain: Double): GainNode {
        val node = audioCtx.createGain()
        node.gain.setTargetAtTime(gain, now(), 0.0)
        return node
    }
}
