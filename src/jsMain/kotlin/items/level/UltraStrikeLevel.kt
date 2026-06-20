package items.level

import portal.Quality
import kotlin.math.max
import kotlin.math.min

enum class UltraStrikeLevel(val level: Int, val damage: Int, val rangeM: Int, val xmCost: Int) : ItemLevel {
    ONE(1, 150, 10, 50),
    TWO(2, 300, 13, 100),
    THREE(3, 500, 16, 150),
    FOUR(4, 900, 18, 200),
    FIVE(5, 1200, 21, 250),
    SIX(6, 1500, 24, 360),
    SEVEN(7, 1800, 27, 490),
    EIGHT(8, 2700, 30, 640);

    fun calculateRecycleXm(): Int = level * 20
    fun critRate(): Double = 0.05
    fun critDamage(): Int = damage * 3

    override fun toInt() = level
    override fun getColor(): String = LevelColor.map[level] ?: "#FFFFFF"

    companion object {
        fun find(level: Int, quality: Quality): UltraStrikeLevel = valueOf(clipLevel(level + quality.addLevels))
        fun valueOf(level: Int) = UltraStrikeLevel.values().find { it.level == clipLevel(level) }!!
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))
    }
}
