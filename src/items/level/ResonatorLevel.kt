package items.level

import portal.Quality
import kotlin.math.max
import kotlin.math.min

enum class ResonatorLevel(val level: Int, val deployablePerPlayer: Int, val energy: Int) : ItemLevel {
    ONE(1, 8, 1000),
    TWO(2, 4, 1500),
    THREE(3, 4, 2000),
    FOUR(4, 4, 2500),
    FIVE(5, 2, 3000),
    SIX(6, 2, 4000),
    SEVEN(7, 1, 5000),
    EIGHT(8, 1, 6000);

    fun calculateRecycleXm(): Int = level * 20

    override fun toInt() = level
    override fun getColor(): String = LevelColor.map.get(level) ?: "#FFFFFF"

    companion object {
        fun valueOf(level: Int) = values().find { it.level == clipLevel(level) }!!
        fun find(level: Int, quality: Quality): ResonatorLevel = valueOf(clipLevel(level + quality.addLevels))
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))
    }
}
