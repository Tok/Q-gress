package items.level

import portal.Quality
import kotlin.math.max
import kotlin.math.min

enum class XmpLevel(val level: Int, val color: String, val damage: Int, val rangeM: Int, val xmCost: Int) {
    ONE(1, "#FECE5A", 150, 42, 50),
    TWO(2, "#FFA630", 300, 48, 100),
    THREE(3,"#FF7315", 500, 58, 150),
    FOUR(4, "#E40000", 900, 72, 200),
    FIVE(5, "#FD2992", 1200, 90, 250),
    SIX(6, "#EB26CD", 1500, 112, 360),
    SEVEN(7, "#C124E0", 1800, 138, 490),
    EIGHT(8, "#9627F4", 2700, 168, 640);

    fun calculateRecycleXm(): Int = level * 20

    companion object {
        fun find(level: Int, quality: Quality): XmpLevel = valueOf(Companion.clipLevel(level + quality.addLevels))
        fun valueOf(level: Int) = XmpLevel.values().find { it.level == clipLevel(level) }!!
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))
    }
}
