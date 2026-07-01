package config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The two config value-bags: the [Styles] render-toggle flags (each read + asserted against its shipped
 * default) and the [OscillatorType] Web-Audio waveform name constants (each is the exact string the audio
 * layer hands to an `OscillatorNode.type`).
 */
class StylesOscillatorTest {

    @Test
    fun stylesFlagsCarryTheirShippedDefaults() {
        assertEquals(0.4, Styles.fieldTransparency, 1e-12, "field fill transparency default")
        assertFalse(Styles.isDrawAgentRange, "agent-range overlay is off by default")
        assertFalse(Styles.isDrawDestination, "destination overlay is off by default")
        assertTrue(Styles.isDrawPortalNames, "portal names are drawn by default")
        assertTrue(Styles.isDrawCom, "the COM feed is drawn by default")
        assertFalse(Styles.isDrawResoLevels, "resonator levels are off by default")
        assertTrue(Styles.isDrawTopAgents, "the top-agents panel is drawn by default")
        assertTrue(Styles.use3DBuildings, "3D buildings are on by default")
        assertFalse(Styles.isDrawObstructedVectors, "obstructed-vector overlay is off by default")
        assertTrue(Styles.isDrawResoLineGradient, "resonator-line gradient is on by default")
        assertTrue(Styles.isFillMuDisplay, "the MU display is filled by default")
    }

    @Test
    fun oscillatorTypesAreTheWebAudioWaveformNames() {
        assertEquals("sine", OscillatorType.SINE)
        assertEquals("square", OscillatorType.SQUARE)
        assertEquals("sawtooth", OscillatorType.SAW)
        assertEquals("triangle", OscillatorType.TRIANGLE)
        assertEquals("custom", OscillatorType.CUSTOM)
        // All five names are distinct.
        val all = listOf(
            OscillatorType.SINE,
            OscillatorType.SQUARE,
            OscillatorType.SAW,
            OscillatorType.TRIANGLE,
            OscillatorType.CUSTOM,
        )
        assertEquals(all.size, all.toSet().size, "every oscillator name is unique")
    }
}
