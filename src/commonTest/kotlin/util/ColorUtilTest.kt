package util

import util.data.Complex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ColorUtilTest {

    // Expected colours are the config.Colors values (black / red / chartreuse), inlined so this
    // test is platform-agnostic (config.Colors stays in the JS shell).
    @Test
    fun complexBlack() = assertEquals("#000000", ColorUtil.getColor(Complex.ZERO))

    @Test
    fun complexRed() = assertEquals("#FF0000", ColorUtil.getColor(Complex.ONE))

    @Test
    fun complexBrown() = assertEquals("#7FFF00", ColorUtil.getColor(Complex.I))

    @Test
    fun getColorWrapsNegativePhase() {
        // A negative-imaginary input → phase < 0 → wrapped by +TAU (the else arm of the phase clip).
        val s = ColorUtil.getColor(Complex(0.0, -0.5))
        assertTrue(s.startsWith("#") && s.length == 7, "still a #rrggbb colour ($s)")
    }

    @Test
    fun hexToRgbNormalizesChannels() {
        val rgb = ColorUtil.hexToRgb("#ff8000")
        assertEquals(1.0, rgb[0], 1e-9)
        assertEquals(128.0 / 255.0, rgb[1], 1e-9)
        assertEquals(0.0, rgb[2], 1e-9)
        assertEquals(1.0, ColorUtil.hexToRgb("ffffff")[0], 1e-9) // tolerates a missing '#'
    }

    @Test
    fun blendHexTakesChannelMidpoint() {
        assertEquals("rgb(127, 127, 127)", ColorUtil.blendHex("#000000", "#ffffff")) // (0+255)/2
        assertEquals("rgb(128, 0, 0)", ColorUtil.blendHex("#ff0000", "#010000"))
    }

    @Test
    fun spectrum() {
        val fact = 0.5
        assertEquals(
            Triple(1.0, fact, 0.0),
            ColorUtil.spectrum(0, fact),
            "Red -> Yellow",
        )
        assertEquals(
            Triple(1.0 - fact, 1.0, 0.0),
            ColorUtil.spectrum(1, fact),
            "Yellow -> Green",
        )
        assertEquals(
            Triple(0.0, 1.0, fact),
            ColorUtil.spectrum(2, fact),
            "Green -> Cyan",
        )
        assertEquals(
            Triple(0.0, 1.0 - fact, 1.0),
            ColorUtil.spectrum(3, fact),
            "Cyan -> Blue",
        )
        assertEquals(
            Triple(fact, 0.0, 1.0),
            ColorUtil.spectrum(4, fact),
            "Blue -> Magenta",
        )
        assertEquals(
            Triple(1.0, 0.0, 1.0 - fact),
            ColorUtil.spectrum(5, fact),
            "Magenta -> Red",
        )
    }

    @Test
    fun negativeSpectrum() {
        assertFailsWith(IllegalArgumentException::class) {
            ColorUtil.spectrum(-1, 0.0)
        }
    }

    @Test
    fun illegalNegativeSpectrum() {
        assertFailsWith(IllegalArgumentException::class) {
            ColorUtil.spectrum(6, 0.0)
        }
    }
}
