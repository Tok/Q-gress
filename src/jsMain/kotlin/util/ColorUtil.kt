package util

import config.Colors
import config.Constants
import extension.toHexString
import util.data.Complex
import kotlin.math.max
import kotlin.math.min

/*
 * Recycled from https://github.com/Tok/Zir-Watchface/blob/master/Wearable/src/main/kotlin/zir/teq/wearable/watchface/util/ColorUtil.kt
 */
object ColorUtil {
    private const val MAX_RGB = 0xFF
    fun getColor(c: Complex): String = getColorFromMagnitudeAndPhase(c.magnitude, c.phase)
    private fun getColorFromMagnitudeAndPhase(magnitude: Double, phase: Double): String {
        if (magnitude <= 1.0 / MAX_RGB) {
            return Colors.black
        }
        val mag = min(1.0, magnitude)
        val clippedPhase = if (phase < 0.0) phase + Constants.tau else phase
        val p = clippedPhase * 6.0 / Constants.tau
        val range = min(5.0, max(0.0, p)).toInt()
        val fraction = p - range
        val rgbValues = spectrum(range, fraction)
        val maxMag = mag * MAX_RGB
        val red = (rgbValues.first * maxMag).toInt()
        val green = (rgbValues.second * maxMag).toInt()
        val blue = (rgbValues.third * maxMag).toInt()
        return "#" + red.toHexString() + green.toHexString() + blue.toHexString()
    }

    /** Parse a "#rrggbb" (or "rrggbb") hex colour into an [r, g, b] array in 0..1 — e.g. for GLSL uniforms. */
    fun hexToRgb(hex: String): DoubleArray {
        val h = hex.removePrefix("#")
        return doubleArrayOf(
            h.substring(0, 2).toInt(16) / 255.0,
            h.substring(2, 4).toInt(16) / 255.0,
            h.substring(4, 6).toInt(16) / 255.0,
        )
    }

    /** The channel-wise midpoint of two "#rrggbb" colours, as an "rgb(r, g, b)" string. */
    fun blendHex(a: String, b: String): String {
        fun channel(hex: String, i: Int) = hex.substring(1 + i * 2, 3 + i * 2).toInt(16)
        val r = (channel(a, 0) + channel(b, 0)) / 2
        val g = (channel(a, 1) + channel(b, 1)) / 2
        val bl = (channel(a, 2) + channel(b, 2)) / 2
        return "rgb($r, $g, $bl)"
    }

    fun spectrum(range: Int, fraction: Double) = when (range) {
        0 -> Triple(1.0, fraction, 0.0) // Red -> Yellow
        1 -> Triple(1.0 - fraction, 1.0, 0.0) // Yellow -> Green
        2 -> Triple(0.0, 1.0, fraction) // Green -> Cyan
        3 -> Triple(0.0, 1.0 - fraction, 1.0) // Cyan -> Blue
        4 -> Triple(fraction, 0.0, 1.0) // Blue -> Magenta
        5 -> Triple(1.0, 0.0, 1.0 - fraction) // Magenta -> Red
        else -> throw IllegalArgumentException("Out of range: $range")
    }
}
