package util

import config.Colors
import config.Constants
import util.data.Complex
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min

/*
 * Recycled from https://github.com/Tok/Zir-Watchface/blob/master/Wearable/src/main/kotlin/zir/teq/wearable/watchface/util/ColorUtil.kt
 */
object ColorUtil {
    val MAX_RGB = 0xFF
    fun getColor(c: Complex): String = getColorFromMagnitudeAndPhase(c.magnitude.toDouble(), c.phase)
    private fun getColorFromMagnitudeAndPhase(magnitude: Double, phase: Double): String {
        if (magnitude <= 1.0 / MAX_RGB) {
            return Colors.black
        }
        val mag = min(1.0, magnitude)
        val clippedPhase = if (phase < 0.0) phase + Constants.tau else phase
        val p = clippedPhase * 6.0 / Constants.tau
        val range = min(5.0, max(0.0, p)).toInt()
        val fraction = p - range
        val rgbValues = getFullSpectrum(range, fraction)
        val maxMag = mag * MAX_RGB
        val red = (rgbValues.first * maxMag).toInt()
        val green = (rgbValues.second * maxMag).toInt()
        val blue = (rgbValues.third * maxMag).toInt()
        return "#" + toHexString(red) + toHexString(green) + toHexString(blue)
    }

    private fun toHexString(int: Int): String {
        val v = (int.toByte() and Byte.MAX_VALUE).toInt()
        return Constants.hexChars[v ushr 4].toString() + Constants.hexChars[v and 0x0F]
    }

    private fun getFullSpectrum(range: Int, fraction: Double) = when (range) {
        0 -> Triple(1.0, fraction, 0.0) //Red -> Yellow
        1 -> Triple(1.0 - fraction, 1.0, 0.0) //Yellow -> Green
        2 -> Triple(0.0, 1.0, fraction) //Green -> Cyan
        3 -> Triple(0.0, 1.0 - fraction, 1.0) //Cyan -> Blue
        4 -> Triple(fraction, 0.0, 1.0) //Blue -> Magenta
        5 -> Triple(1.0, 0.0, 1.0 - fraction) //Magenta -> Red
        else -> throw IllegalArgumentException("Out of range: " + range)
    }
}
