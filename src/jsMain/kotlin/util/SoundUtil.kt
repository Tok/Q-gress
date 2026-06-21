package util

import World
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
import kotlin.math.exp
import kotlin.math.sqrt

object SoundUtil {
    const val DEFAULT_VOLUME = 1.0
    private const val EPS = 0.0001 // exponentialRamp can't target 0

    // 3D-audio tuning (sim-space is real metres). The play area is hundreds of metres across, so a
    // small reference distance + gentle rolloff makes near/far audibly differ at gameplay zoom while
    // distant events still carry. The listener (camera) sits well above, so Z mostly adds elevation.
    private const val SOUND_Z = 1.6 // head height in metres
    private const val REF_DISTANCE = 45.0
    private const val MAX_DISTANCE = 6000.0
    private const val ROLLOFF = 0.8
    private const val PANNING_MODEL = "HRTF" // front/back + elevation cues (vs cheaper "equalpower")

    private val audioCtx = AudioContext()
    private val listener = audioCtx.listener

    // Single master gain all sounds route through; controls overall volume.
    private val masterGain: GainNode = audioCtx.createGain().also {
        it.gain.value = 0.0
        it.connect(audioCtx.destination)
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
        masterGain.gain.setTargetAtTime(volume, now(), 0.01)
    }

    private fun isMuted() = masterVolume <= 0.0

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
        playNoiseCrack(pos, amplitude, heaviness)
        playThud(pos, amplitude, heaviness)
        repeat((9 + heaviness * 17).toInt()) { playTinkle(pos, amplitude) }
    }

    /** XMP burst: a low sine "boom" sweeping down + a high-passed noise "fwoom", sized by level (1..8). */
    fun playXmpSound(pos: Pos, level: Int) {
        if (isMuted()) return
        val amp = 0.5 + level * 0.06
        // Deep boom: a sine sweeping well down, with a longer decay.
        val dur = 0.5 + level * 0.04
        val osc = createExponentialRampOscillator(OscillatorType.SINE, 120.0 + level * 6.0, 26.0, dur)
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime(amp, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur)
        connectVoice(osc, createPanner(pos), gainNode, n + dur)
        // Low-passed noise "blast" (a rumble that whoomphs down), not the bright hi-hat-like crack.
        playNoiseBlast(pos, amp * 0.9, 0.45 + level * 0.05)
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

    /** Hack: a short centrifuge whir that spins up then eases off — matches the collar animation. */
    fun playHackingSound(pos: Pos) {
        if (isMuted()) return
        val dur = 0.5
        val n = now()
        val panner = createPanner(pos)
        val osc = audioCtx.createOscillator()
        osc.type = OscillatorType.TRIANGLE
        osc.frequency.setValueAtTime(180.0, n)
        osc.frequency.exponentialRampToValueAtTime(560.0, n + dur * 0.5) // spin up
        osc.frequency.exponentialRampToValueAtTime(300.0, n + dur) // ease off
        val gainNode = audioCtx.createGain()
        gainNode.gain.setValueAtTime(EPS, n)
        gainNode.gain.linearRampToValueAtTime(0.16, n + 0.04)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur)
        osc.connect(gainNode)
        gainNode.connect(panner)
        panner.connect(masterGain)
        osc.start()
        osc.stop(n + dur)
    }

    /** Glyph hack: a deeper, longer whir + a glassy resonant chime — reads as stronger than a hack. */
    fun playGlyphingSound(pos: Pos) {
        if (isMuted()) return
        val dur = 0.95
        val n = now()
        val panner = createPanner(pos)
        // Deeper centrifuge whir (sawtooth softened through a lowpass).
        val osc = audioCtx.createOscillator()
        osc.type = OscillatorType.SAW
        osc.frequency.setValueAtTime(150.0, n)
        osc.frequency.exponentialRampToValueAtTime(680.0, n + dur * 0.55)
        osc.frequency.exponentialRampToValueAtTime(280.0, n + dur)
        val lowpass = audioCtx.createBiquadFilter()
        lowpass.type = "lowpass"
        lowpass.frequency.setValueAtTime(1300.0, n)
        val gainNode = audioCtx.createGain()
        gainNode.gain.setValueAtTime(EPS, n)
        gainNode.gain.linearRampToValueAtTime(0.24, n + 0.05)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur)
        osc.connect(lowpass)
        lowpass.connect(gainNode)
        gainNode.connect(panner)
        panner.connect(masterGain)
        osc.start()
        osc.stop(n + dur)
        // A glassy chime rings up as it completes (the "stronger / skill" flourish).
        val ring = audioCtx.createOscillator()
        ring.type = OscillatorType.SINE
        val rt = n + dur * 0.45
        ring.frequency.setValueAtTime(1040.0, rt)
        ring.frequency.exponentialRampToValueAtTime(1640.0, n + dur)
        val ringGain = audioCtx.createGain()
        ringGain.gain.setValueAtTime(EPS, n) // silent until the chime onset at rt
        ringGain.gain.setValueAtTime(EPS, rt)
        ringGain.gain.linearRampToValueAtTime(0.13, rt + 0.05)
        ringGain.gain.exponentialRampToValueAtTime(EPS, n + dur + 0.25)
        ring.connect(ringGain)
        ringGain.connect(panner)
        ring.start()
        ring.stop(n + dur + 0.25)
    }

    /** Portal gained a level: a quick rising note. */
    fun playUpgradeSound(pos: Pos) {
        if (isMuted()) return
        val dur = 0.18
        val osc = createExponentialRampOscillator(OscillatorType.SINE, 520.0, 880.0, dur)
        playSound(osc, createPanner(pos), 0.08, dur)
    }

    /** Portal lost a level: a quick falling note. */
    fun playDowngradeSound(pos: Pos) {
        if (isMuted()) return
        val dur = 0.2
        val osc = createExponentialRampOscillator(OscillatorType.SINE, 520.0, 300.0, dur)
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
        val freq = 160.0 - (level.level * 5)
        val osc = createStaticOscillator(OscillatorType.SQUARE, freq)
        val gain = (0.04 + (level.level * 0.006))
        val duration = 0.005 + (0.001 * level.level)
        playSound(osc, createPanner(pos), gain, duration)
    }

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
        val midAnchor = Pos((field.primaryAnchor.x() + field.secondaryAnchor.x()) / 2, (field.primaryAnchor.y() + field.secondaryAnchor.y()) / 2)
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

    private fun now() = audioCtx.currentTime.toDouble()

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
    private fun createPanner(pos: Pos): PannerNode {
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
