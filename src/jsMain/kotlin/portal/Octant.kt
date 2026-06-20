package portal

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class Octant(val arrow: Char, private val angle: Double) {
    E('→', 0 * PI / 180),
    SE('↘', 45 * PI / 180),
    S('↓', 90 * PI / 180),
    SW('↙', 135 * PI / 180),
    W('←', 180 * PI / 180),
    NW('↖', 225 * PI / 180),
    N('↑', 270 * PI / 180),
    NE('↗', 315 * PI / 180);

    fun calcXOffset(radius: Int): Int = (radius * cos(angle)).toInt()
    fun calcYOffset(radius: Int): Int = (radius * sin(angle)).toInt()
    override fun toString() = arrow.toString()
}
