package items.level

import portal.Quality
import kotlin.math.max
import kotlin.math.min

enum class PowerCubeLevel(val level: Int, val color: String, val xmValue: Int) {
    ONE(1, "#FECE5A", 1000),
    TWO(2, "#FFA630", 2000),
    THREE(3,"#FF7315", 3000),
    FOUR(4, "#E40000", 4000),
    FIVE(5, "#FD2992", 5000),
    SIX(6, "#EB26CD", 6000),
    SEVEN(7, "#C124E0", 7000),
    EIGHT(8, "#9627F4", 8000);

    fun calculateRecycleXm(): Int = xmValue

    companion object {
        fun find(level: Int, quality: Quality): PowerCubeLevel = valueOf(clipLevel(level + quality.addLevels))
        fun valueOf(level: Int) = PowerCubeLevel.values().find { it.level == clipLevel(level) }!!
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))
    }
}
