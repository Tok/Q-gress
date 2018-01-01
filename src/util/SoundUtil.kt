package util

import World
import config.Constants
import config.Dimensions
import config.OscillatorType
import external.sound.AudioContext
import external.sound.GainNode
import external.sound.OscillatorNode
import external.sound.StereoPannerNode
import items.level.XmpLevel
import portal.Field
import portal.Link
import util.data.Coords

object SoundUtil {
    val audioCtx = AudioContext()

    fun playPortalCreationSound(pos: Coords) {
        val duration = 0.5
        val pan = pos.x.toDouble() / World.can.width
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 120.0, 0.0, duration)
        val panNode = createStaticPan(pan)
        playSound(oscNode, panNode, 0.50, duration)
    }

    fun playPortalRemovalSound(pos: Coords) {
        val duration = 0.5
        val pan = pos.x.toDouble() / World.can.width
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 60.0, 120.0, duration)
        val panNode = createStaticPan(pan)
        playSound(oscNode, panNode, 0.50, duration)
    }

    fun playHackingSound(pos: Coords) {
        val freq = 500.0
        val osc = createStaticOscillator(OscillatorType.SINE, freq)
        val pan = pos.xx() / World.can.width
        val panNode = createStaticPan(pan)
        val gain = 0.02
        val duration = 0.02
        playSound(osc, panNode, gain, duration)
    }

    fun playXmpSound(level: XmpLevel, pos: Coords) {
        val freq = 160.0 - (level.level * 5)
        val osc = createStaticOscillator(OscillatorType.SQUARE, freq)
        val pan = pos.xx() / World.can.width
        val panNode = createStaticPan(pan)
        val gain = 0.02 + (level.level * 0.003)
        val duration = 0.005 + (0.001 * level.level)
        playSound(osc, panNode, gain, duration)
    }

    fun playDeploySound(pos: Coords, distanceToPortal: Int) {
        val ratio = distanceToPortal / Dimensions.maxDeploymentRange
        val gain = 0.05
        val duration = 0.2
        val minFreq = 250.0
        val baseFreq = -250.0
        val startFreq = minFreq + (baseFreq * ratio)
        val endFreq = minFreq + (baseFreq * ratio * 2)
        val pan = pos.x.toDouble() / World.can.width
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, startFreq, endFreq, duration)
        val panNode = createStaticPan(pan)
        playSound(oscNode, panNode, gain, duration)
    }

    fun playLinkingSound(link: Link) {
        val ratio = link.getLine().calcLength() / World.diagonalLength()
        val gain = 0.15
        val duration = 0.04 + (0.16 * ratio)
        val minFreq = 500.0 * ratio
        val baseFreq = 500.0
        val startFreq = minFreq + (baseFreq * ratio)
        val endFreq = minFreq + (baseFreq * ratio * 2)
        val startPan = link.getLine().from.x.toDouble() / World.can.width
        val endPan = link.getLine().to.x.toDouble() / World.can.width
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, startFreq, endFreq, duration)
        val panNode = createLinearRampPan(startPan, endPan, duration)
        playSound(oscNode, panNode, gain, duration)
    }

    fun playFieldingSound(field: Field) {
        val areaRatio = field.calculateArea() / World.totalArea()
        val gain = 0.2
        val minDuration = 1.0 / Constants.phi
        val maxDuration = 1.0
        val diff = maxDuration - minDuration
        val additionalDuration = diff * areaRatio
        val duration = minDuration + additionalDuration
        val minFreq = 70.0
        val baseFreq = 20.0
        val startFreq = minFreq + (baseFreq * areaRatio)
        val endFreq = startFreq * 2.0
        val startPan = field.origin.x() / World.can.width
        val endPan = 0.5 * (field.primaryAnchor.x() + field.secondaryAnchor.x()) / World.can.width
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
