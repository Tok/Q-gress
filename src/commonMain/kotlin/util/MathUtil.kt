package util

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Pure, platform-independent numeric helpers — clamping, angle conversion, time formatting. Part of the
 * shared functional core (`commonMain`): no `World`, DOM, WebGL or `js()`, so it is unit-tested + line-covered
 * on the JVM. [util.Util] (the JS-facing facade) delegates here, so existing `MathUtil.clip(...)` call sites are
 * unchanged.
 */
object MathUtil {
    fun clip(value: Int, from: Int, to: Int): Int = max(from, min(to, value))
    fun clipDouble(value: Double, from: Double, to: Double): Double = max(from, min(to, value))

    fun degToRad(degrees: Double): Double = degrees * PI / 180
    fun radToDeg(radians: Double): Double = radians * 180 / PI

    private fun fixTime(v: Int): String = if (v.toString().length <= 1) v.toString().padStart(2, '0') else v.toString()
    fun formatSeconds(absSeconds: Int): String {
        val seconds: Int = absSeconds % 60
        val minutes: Int = floor(absSeconds / 60.0).toInt() % 60
        val hours: Int = floor(absSeconds / 3600.0).toInt()
        return fixTime(hours) + ":" + fixTime(minutes) + ":" + fixTime(seconds)
    }
}
