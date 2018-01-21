package items.level

import portal.Quality
import kotlin.math.min
import kotlin.math.max

enum class ResonatorLevel(val level: Int, val deployablePerPlayer: Int, val color: String, val energy: Int) {
    ONE(1, 8, "#FECE5A", 1000),
    TWO(2, 4, "#FFA630", 1500),
    THREE(3, 4, "#FF7315", 2000),
    FOUR(4, 4, "#E40000", 2500),
    FIVE(5, 2, "#FD2992", 3000),
    SIX(6, 2, "#EB26CD", 4000),
    SEVEN(7, 1, "#C124E0", 5000),
    EIGHT(8, 1, "#9627F4", 6000);

    fun calculateRecycleXm(): Int = level * 20
    companion object {
        fun valueOf(level: Int) = values().find { it.level == clipLevel(level) }!!
        fun find(level: Int, quality: Quality): ResonatorLevel = valueOf(clipLevel(level + quality.addLevels))
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))
    }
}
