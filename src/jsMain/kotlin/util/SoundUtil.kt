package util

import World
import agent.NonFaction
import config.Config
import config.Constants
import config.Dim
import config.OscillatorType
import config.Sim
import external.sound.AudioContext
import external.sound.GainNode
import external.sound.OscillatorNode
import external.sound.StereoPannerNode
import items.level.XmpLevel
import org.khronos.webgl.set
import portal.Field
import portal.Link
import system.Checkpoint
import util.data.Pos
import kotlin.math.exp

object SoundUtil {
    const val DEFAULT_VOLUME = 0.4
    private const val EPS = 0.0001 // exponentialRamp can't target 0
    private val audioCtx = AudioContext()

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
        val pan = pos.x / Sim.width
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 120.0, 0.0, duration)
        playSound(oscNode, createStaticPan(pan), gain, duration)
    }

    fun playPortalRemovalSound(pos: Pos) {
        if (isMuted()) return
        val duration = 0.5
        val pan = pos.x / Sim.width
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 60.0, 120.0, duration)
        playSound(oscNode, createStaticPan(pan), 1.0, duration)
    }

    /**
     * Procedural glass-shatter (ported from qlippostasis GlassShatterSound): a bright high-passed
     * noise "crack" + a low sine "thud" + a scatter of damped high-sine "tinkles". [heaviness]
     * 0≈one small object, 1≈big; randomised so no two shatters sound the same.
     */
    fun playGlassShatterSound(pos: Pos, heaviness: Double = 0.3, amplitude: Double = 0.7) {
        if (isMuted()) return
        val pan = pos.x / Sim.width
        playNoiseCrack(pan, amplitude, heaviness)
        playThud(pan, amplitude, heaviness)
        repeat((9 + heaviness * 17).toInt()) { playTinkle(pan, amplitude) }
    }

    /** XMP burst: a low sine "boom" sweeping down + a high-passed noise "fwoom", sized by level (1..8). */
    fun playXmpSound(pos: Pos, level: Int) {
        if (isMuted()) return
        val pan = pos.x / Sim.width
        val amp = 0.5 + level * 0.06
        // Deep boom: a sine sweeping well down, with a longer decay.
        val dur = 0.5 + level * 0.04
        val osc = createExponentialRampOscillator(OscillatorType.SINE, 120.0 + level * 6.0, 26.0, dur)
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime(amp, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur)
        connectVoice(osc, createStaticPan(pan), gainNode, n + dur)
        // Low-passed noise "blast" (a rumble that whoomphs down), not the bright hi-hat-like crack.
        playNoiseBlast(pan, amp * 0.9, 0.45 + level * 0.05)
    }

    private fun playNoiseBlast(pan: Double, amplitude: Double, dur: Double) {
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
        val panNode = createStaticPan(pan)
        source.connect(lowpass)
        lowpass.connect(gainNode)
        gainNode.connect(panNode)
        panNode.connect(masterGain)
        source.start()
        source.stop(n + dur)
    }

    private fun playNoiseCrack(pan: Double, amplitude: Double, heaviness: Double) {
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
        val panNode = createStaticPan(pan)
        source.connect(highpass)
        highpass.connect(gainNode)
        gainNode.connect(panNode)
        panNode.connect(masterGain)
        source.start()
        source.stop(now() + dur)
    }

    private fun playThud(pan: Double, amplitude: Double, heaviness: Double) {
        val tau = 0.07 + Util.random() * 0.07 + heaviness * 0.06
        val osc = createStaticOscillator(OscillatorType.SINE, 85.0 + Util.random() * 80.0)
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime((0.6 + heaviness * 0.9) * amplitude, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + tau * 5.0)
        connectVoice(osc, createStaticPan(pan), gainNode, n + tau * 5.0)
    }

    private fun playTinkle(pan: Double, amplitude: Double) {
        val tau = 0.02 + Util.random() * 0.1
        val osc = createStaticOscillator(OscillatorType.SINE, 2200.0 + Util.random() * 6800.0)
        val gainNode = audioCtx.createGain()
        val n = now() + Util.random() * 0.5
        gainNode.gain.setValueAtTime(EPS, now())
        gainNode.gain.setValueAtTime((0.12 + Util.random() * 0.33) * amplitude, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + tau * 5.0)
        connectVoice(osc, createStaticPan(pan), gainNode, n + tau * 5.0)
    }

    private fun connectVoice(osc: OscillatorNode, panNode: StereoPannerNode, gainNode: GainNode, stopTime: Double) {
        osc.connect(panNode)
        panNode.connect(gainNode)
        gainNode.connect(masterGain)
        osc.start()
        osc.stop(stopTime)
    }

    fun playCheckpointSound(@Suppress("UNUSED_PARAMETER") checkpoint: Checkpoint) {
        if (isMuted()) return
        val duration = 0.05
        val pan = 0.5
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
        val pan = 0.5
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 220.0, 220.0, duration)
        playSound(oscNode, createStaticPan(pan), 0.5, duration)
    }

    fun playNpcCreationSound(npc: NonFaction) {
        if (isMuted()) return
        val duration = 0.02
        val pan = npc.pos.x / Sim.width
        val offset = -(npc.size.offset * 120.0)
        val start = 660.0
        val end = 660.0 + offset
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, start, end, duration)
        playSound(oscNode, createStaticPan(pan), 0.2, duration)
    }

    fun playHackingSound(pos: Pos) {
        if (isMuted()) return
        val freq = 500.0
        val osc = createStaticOscillator(OscillatorType.SINE, freq)
        val pan = pos.x / Sim.width
        val gain = 0.04
        val duration = 0.02
        playSound(osc, createStaticPan(pan), gain, duration)
    }

    fun playGlyphingSound(pos: Pos) {
        if (isMuted()) return
        val freq = 400.0
        val osc = createStaticOscillator(OscillatorType.SINE, freq)
        val pan = pos.x / Sim.width
        val gain = 0.04
        val duration = 0.06
        playSound(osc, createStaticPan(pan), gain, duration)
    }

    fun playXmpSound(level: XmpLevel, pos: Pos) {
        if (isMuted()) return
        val freq = 160.0 - (level.level * 5)
        val osc = createStaticOscillator(OscillatorType.SQUARE, freq)
        val pan = pos.x / Sim.width
        val gain = (0.04 + (level.level * 0.006))
        val duration = 0.005 + (0.001 * level.level)
        playSound(osc, createStaticPan(pan), gain, duration)
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
        val pan = pos.x / Sim.width
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, startFreq, endFreq, duration)
        playSound(oscNode, createStaticPan(pan), gain, duration)
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
        val startPan = link.getLine().from.x / Sim.width
        val endPan = link.getLine().to.x / Sim.width
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, startFreq, endFreq, duration)
        val panNode = createLinearRampPan(startPan, endPan, duration)
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
        connectVoice(osc, createStaticPan(0.5), gainNode, n + dur)
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
        val startPan = field.origin.x() / Sim.width
        val endPan = 0.5 * (field.primaryAnchor.x() + field.secondaryAnchor.x()) / Sim.width
        val oscNode = createExponentialRampOscillator(OscillatorType.TRIANGLE, startFreq, endFreq, duration)
        val panNode = createLinearRampPan(startPan, endPan, duration)
        playSound(oscNode, panNode, gain, duration)
    }

    private fun playSound(oscNode: OscillatorNode, panNode: StereoPannerNode, gain: Double, duration: Double) {
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
            val pan = Util.random()
            val tc = timeConstant * it
            node.pan.setTargetAtTime(pan, n + tc, timeConstant)
        }

        return node
    }

    private fun createLinearRampPan(startPan: Double, endPan: Double, duration: Double): StereoPannerNode {
        val node = createStaticPan(startPan)
        node.pan.linearRampToValueAtTime(endPan, now() + duration)
        return node
    }

    private fun createStaticGain(gain: Double): GainNode {
        val node = audioCtx.createGain()
        node.gain.setTargetAtTime(gain, now(), 0.0)
        return node
    }
}
