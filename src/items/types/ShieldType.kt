package items.types

enum class ShieldType(val level: Int, val abbr: String, val color: String,
                      val mitigation: Int, val stickiness: Int,
                      val deployCostXm: Int, val chance: Double) {
    COMMON(1, "CS", "#8CFBBD", 30, 0, 250, 10.0 / 50),
    RARE(2, "RS", "#B18DFD", 40, 15, 500, 10.0 / 500),
    VERY_RARE(3, "VRS", "#F88BF5", 60, 45, 1000, 10.0 / 1500),
    AEGIS(4, "AEGIS", "#F88BF5", 70, 80, 1000, 10.0 / 1500);

    companion object {
        fun getColorForLevel(level: Int) = when (level) {
            1 -> COMMON.color
            2 -> RARE.color
            3 -> VERY_RARE.color
            4 -> AEGIS.color
            else -> "#FFFFFF"
        }
    }
}
