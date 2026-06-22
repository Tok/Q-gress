package items.types

/** Mod rarity tiers + their canonical colours (mint / purple / pink), shared across all mod types. */
enum class Rarity(val abbr: String, val color: String) {
    COMMON("C", "#8CFBBD"), // mint
    RARE("R", "#B18DFD"), // purple
    VERY_RARE("VR", "#F88BF5"), // pink
}
