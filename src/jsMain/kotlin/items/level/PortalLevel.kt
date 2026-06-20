package items.level

enum class PortalLevel(val value: Int, val display: String) {
    ZERO(0, " "),
    ONE(1, "1"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8");

    fun toInt(): Int = value
    fun getColor(): String = LevelColor.map[value] ?: "#FFFFFF"

    companion object {
        fun findByValue(value: Int): PortalLevel = PortalLevel.values().find { it.value == value }!!
    }
}
