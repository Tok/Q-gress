package util

import World
import agent.NonFaction
import config.Constants
import config.Dim
import config.OscillatorType
import external.sound.AudioContext
import external.sound.GainNode
import external.sound.OscillatorNode
import external.sound.StereoPannerNode
import items.level.XmpLevel
import org.w3c.dom.HTMLInputElement
import portal.Field
import portal.Link
import system.Checkpoint
import util.data.Coords
import kotlin.browser.document

object SoundUtil {
    private val audioCtx = AudioContext()

    private fun isMuted(): Boolean {
        val soundCheckbox = document.getElementById(HtmlUtil.SOUND_CHECKBOX_ID) as HTMLInputElement
        return !soundCheckbox.checked
    }

    private fun volume() = if (isMuted()) 0.0 else 0.4 //TODO create volume slider

    fun playNoiseGenSound() {
        if (isMuted()) return
        val freq = 330
        val osc = createNoiseOscillator(freq)
        playSound(osc, createNoisePan(), 0.15, 13.0)
    }

    fun playOffScreenLocationCreationSound() {
        val center = Coords(Dim.width / 2, Dim.height / 2)
        return playPortalCreationSound(center, 0.5)
    }

    fun playPortalCreationSound(pos: Coords, gain: Double = 1.0) {
        if (isMuted()) return
        val duration = 0.5
        val pan = pos.x.toDouble() / Dim.width
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 120.0, 0.0, duration)
        playSound(oscNode, createStaticPan(pan), gain, duration)
    }

    fun playPortalRemovalSound(pos: Coords) {
        if (isMuted()) return
        val duration = 0.5
        val pan = pos.x.toDouble() / Dim.width
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 60.0, 120.0, duration)
        playSound(oscNode, createStaticPan(pan), 1.0, duration)
    }

    fun playCheckpointSound(@Suppress("UNUSED_PARAMETER") checkpoint: Checkpoint) {
        if (isMuted()) return
        val duration = 0.05
        val pan = 0.5
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 440.0, 440.0, duration)
        playSound(oscNode, createStaticPan(pan), 0.5, duration)
    }

    fun playNpcCreationSound(npc: NonFaction) {
        if (isMuted()) return
        val duration = 0.02
        val pan = npc.pos.xx() / Dim.width
        val offset = -(npc.size.offset * 120.0)
        val start = 660.0
        val end = 660.0 + offset
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, start, end, duration)
        playSound(oscNode, createStaticPan(pan), 0.2, duration)
    }

    fun playHackingSound(pos: Coords) {
        if (isMuted()) return
        val freq = 500.0
        val osc = createStaticOscillator(OscillatorType.SINE, freq)
        val pan = pos.xx() / Dim.width
        val gain = 0.04
        val duration = 0.02
        playSound(osc, createStaticPan(pan), gain, duration)
    }

    fun playXmpSound(level: XmpLevel, pos: Coords) {
        if (isMuted()) return
        val freq = 160.0 - (level.level * 5)
        val osc = createStaticOscillator(OscillatorType.SQUARE, freq)
        val pan = pos.xx() / Dim.width
        val gain = (0.04 + (level.level * 0.006))
        val duration = 0.005 + (0.001 * level.level)
        playSound(osc, createStaticPan(pan), gain, duration)
    }

    fun playDeploySound(pos: Coords, distanceToPortal: Int) {
        if (isMuted()) return
        val ratio = distanceToPortal / Dim.maxDeploymentRange
        val gain = 0.10
        val duration = 0.2
        val minFreq = 250.0
        val baseFreq = -250.0
        val startFreq = minFreq + (baseFreq * ratio)
        val endFreq = minFreq + (baseFreq * ratio * 2)
        val pan = pos.x.toDouble() / Dim.width
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, startFreq, endFreq, duration)
        playSound(oscNode, createStaticPan(pan), gain, duration)
    }

    fun playLinkingSound(link: Link) {
        if (isMuted()) return
        val ratio = link.getLine().calcLength() / World.diagonalLength()
        val gain = 0.30
        val duration = 0.04 + (0.16 * ratio)
        val minFreq = 500.0 * ratio
        val baseFreq = 500.0
        val startFreq = minFreq + (baseFreq * ratio)
        val endFreq = minFreq + (baseFreq * ratio * 2)
        val startPan = link.getLine().from.x.toDouble() / Dim.width
        val endPan = link.getLine().to.x.toDouble() / Dim.width
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, startFreq, endFreq, duration)
        val panNode = createLinearRampPan(startPan, endPan, duration)
        playSound(oscNode, panNode, gain, duration)
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
        val startPan = field.origin.x() / Dim.width
        val endPan = 0.5 * (field.primaryAnchor.x() + field.secondaryAnchor.x()) / Dim.width
        val oscNode = createExponentialRampOscillator(OscillatorType.TRIANGLE, startFreq, endFreq, duration)
        val panNode = createLinearRampPan(startPan, endPan, duration)
        playSound(oscNode, panNode, gain, duration)
    }

    private fun playSound(oscNode: OscillatorNode, panNode: StereoPannerNode, gain: Double, duration: Double) {
        val gainNode = createStaticGain(gain)
        oscNode.connect(panNode)
        panNode.connect(gainNode)
        gainNode.connect(audioCtx.destination)
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
            val freq = Util.randomInt(maxFreq - (maxFreq * it / max)).toDouble()
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
            val pan = Util.randomInt(Dim.width).toDouble() / Dim.width
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
        node.gain.setTargetAtTime(gain * volume(), now(), 0.0)
        return node
    }
}
