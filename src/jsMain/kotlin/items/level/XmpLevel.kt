package items.level

import portal.Quality
import kotlin.math.max
import kotlin.math.min

enum class XmpLevel(val level: Int, val damage: Int, val rangeM: Int, val xmCost: Int) : ItemLevel {
    ONE(1, 150, 42, 50),
    TWO(2, 300, 48, 100),
    THREE(3, 500, 58, 150),
    FOUR(4, 900, 72, 200),
    FIVE(5, 1200, 90, 250),
    SIX(6, 1500, 112, 360),
    SEVEN(7, 1800, 138, 490),
    EIGHT(8, 2700, 168, 640);

    fun calculateRecycleXm(): Int = level * 20

    override fun toInt() = level
    override fun getColor(): String = LevelColor.map[level] ?: "#FFFFFF"

    companion object {
        fun find(level: Int, quality: Quality): XmpLevel = valueOf(clipLevel(level + quality.addLevels))
        fun valueOf(level: Int) = XmpLevel.values().find { it.level == clipLevel(level) }!!
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))
    }
}
