package util

import config.Colors
import util.data.Complex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ColorUtilTest {

    @Test
    fun complexBlack() = assertEquals(Colors.black, ColorUtil.getColor(Complex.ZERO))

    @Test
    fun complexRed() = assertEquals(Colors.red.toUpperCase(), ColorUtil.getColor(Complex.ONE))

    @Test
    fun complexBrown() = assertEquals(Colors.chartreuse.toUpperCase(), ColorUtil.getColor(Complex.I))

    @Test
    fun spectrum() {
        val fact = 0.5
        assertEquals(
            Triple(1.0, fact, 0.0),
            ColorUtil.spectrum(0, fact),
            "Red -> Yellow"
        )
        assertEquals(
            Triple(1.0 - fact, 1.0, 0.0),
            ColorUtil.spectrum(1, fact),
            "Yellow -> Green"
        )
        assertEquals(
            Triple(0.0, 1.0, fact),
            ColorUtil.spectrum(2, fact),
            "Green -> Cyan"
        )
        assertEquals(
            Triple(0.0, 1.0 - fact, 1.0),
            ColorUtil.spectrum(3, fact),
            "Cyan -> Blue"
        )
        assertEquals(
            Triple(fact, 0.0, 1.0),
            ColorUtil.spectrum(4, fact),
            "Blue -> Magenta"
        )
        assertEquals(
            Triple(1.0, 0.0, 1.0 - fact),
            ColorUtil.spectrum(5, fact),
            "Magenta -> Red"
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
