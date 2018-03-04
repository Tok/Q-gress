package items.level

import portal.Quality
import kotlin.math.max
import kotlin.math.min

enum class PowerCubeLevel(val level: Int, val xmValue: Int) : ItemLevel {
    ONE(1, 1000),
    TWO(2, 2000),
    THREE(3,3000),
    FOUR(4, 4000),
    FIVE(5,  5000),
    SIX(6, 6000),
    SEVEN(7,  7000),
    EIGHT(8, 8000);

    fun calculateRecycleXm(): Int = xmValue

    override fun toInt() = level
    override fun getColor(): String = LevelColor.map.get(level) ?: "#FFFFFF"

    companion object {
        fun find(level: Int, quality: Quality): PowerCubeLevel = valueOf(clipLevel(level + quality.addLevels))
        fun valueOf(level: Int) = PowerCubeLevel.values().find { it.level == clipLevel(level) }!!
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))
    }
}
