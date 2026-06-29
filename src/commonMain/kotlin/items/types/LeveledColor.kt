package items.types

/**
 * A levelled, colour-bearing mod type. Shields / heat sinks / multi-hacks all carry a [level] and a
 * (rarity-derived) [color], so their identical "what colour is the entry at this level?" lookup lives once
 * here ([colorForLevel]) instead of being copy-pasted into each enum's companion.
 */
interface LeveledColor {
    val level: Int
    val color: String
}

/** Neutral white fallback (R==G==B) when no entry matches the requested level. */
const val NO_MOD_COLOR = "#ffffff"

/** The [color] of the entry at [level], or [NO_MOD_COLOR]. Backs the mod-type companions' `getColorForLevel`. */
fun Array<out LeveledColor>.colorForLevel(level: Int): String = firstOrNull { it.level == level }?.color ?: NO_MOD_COLOR
