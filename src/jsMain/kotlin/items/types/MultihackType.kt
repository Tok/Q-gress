package items.types

/**
 * Multi-hack mods raise a portal's hacks-before-burnout limit for everyone. Authentic Ingress: Common **+4**,
 * Rare **+8**, Very Rare **+12**; with several deployed only the rarest counts full, the rest half (see
 * [items.deployable.Multihack.additionalHacks]). Colour comes from [rarity]. See docs/MECHANICS.
 */
enum class MultihackType(val level: Int, val abbr: String, val rarity: Rarity, val additionalHacks: Int, val deployCostXm: Int) {
    COMMON(1, "MH", Rarity.COMMON, 4, 400),
    RARE(2, "RMH", Rarity.RARE, 8, 800),
    VERY_RARE(3, "VRMH", Rarity.VERY_RARE, 12, 1000),
    ;

    val color: String get() = rarity.color
}
