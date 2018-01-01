package items.level

import portal.Quality
import kotlin.math.min
import kotlin.math.max

enum class UltraStrikeLevel(val level: Int, val color: String, val damage: Int, val rangeM: Int, val xmCost: Int) {
    ONE(1, "#FECE5A", 150, 10, 50),
    TWO(2, "#FFA630", 300, 13, 100),
    THREE(3,"#FF7315", 500, 16, 150),
    FOUR(4, "#E40000", 900, 18, 200),
    FIVE(5, "#FD2992", 1200, 21, 250),
    SIX(6, "#EB26CD", 1500, 24, 360),
    SEVEN(7, "#C124E0", 1800, 27, 490),
    EIGHT(8, "#9627F4", 2700, 30, 640);

    fun calculateRecycleXm(): Int = level * 20
    fun critRate(): Double = 0.05
    fun critDamage(): Int = damage * 3

    companion object {
        fun find(level: Int, quality: Quality): UltraStrikeLevel = valueOf(clipLevel(level + quality.addLevels))
        fun valueOf(level: Int) = UltraStrikeLevel.values().find { it.level == clipLevel(level) }!!
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))
    }
}
