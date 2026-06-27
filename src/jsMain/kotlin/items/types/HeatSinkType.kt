package items.types

/**
 * Heat sinks reduce a portal's hack cooldown for everyone (rarest applies full, each subsequent halved
 * — see Portal.cooldownFactor). ~2018 Ingress: Common 20%, Rare 50%, Very Rare 70%. See docs/MECHANICS.
 */
enum class HeatSinkType(
    val level: Int,
    val abbr: String,
    val rarity: Rarity,
    val cooldownReduction: Int, // percent off the portal hack cooldown
    val deployCostXm: Int,
) {
    COMMON(1, "CHS", Rarity.COMMON, 20, 500),
    RARE(2, "RHS", Rarity.RARE, 50, 750),
    VERY_RARE(3, "VRHS", Rarity.VERY_RARE, 70, 1000),
    ;

    val color: String get() = rarity.color

    companion object {
        fun getColorForLevel(level: Int) = values().firstOrNull { it.level == level }?.color ?: "#FFFFFF"
    }
}
