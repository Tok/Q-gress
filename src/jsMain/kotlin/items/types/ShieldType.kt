package items.types

/** Portal shields — reduce incoming XMP damage (mitigation). Colour comes from [rarity]. */
enum class ShieldType(
    val level: Int,
    val abbr: String,
    val rarity: Rarity,
    val mitigation: Int,
    val stickiness: Int,
    val deployCostXm: Int,
    val chance: Double,
) {
    COMMON(1, "CS", Rarity.COMMON, 30, 0, 250, 10.0 / 50),
    RARE(2, "RS", Rarity.RARE, 40, 15, 500, 10.0 / 500),
    VERY_RARE(3, "VRS", Rarity.VERY_RARE, 60, 45, 1000, 10.0 / 1500),
    AEGIS(4, "AEGIS", Rarity.VERY_RARE, 70, 80, 1000, 10.0 / 1500),
    ;

    val color: String get() = rarity.color

    companion object {
        fun getColorForLevel(level: Int) = values().firstOrNull { it.level == level }?.color ?: "#FFFFFF"
    }
}
