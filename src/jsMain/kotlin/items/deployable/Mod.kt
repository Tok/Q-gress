package items.deployable

import items.types.Rarity

/** The kinds of portal mod (each renders with its own shape; see Scene3D.buildMods). */
enum class ModType { SHIELD, HEAT_SINK, LINK_AMP }

/**
 * A portal mod — a slot item (shield / heat sink / link amp). Portals have 4 mod slots; mods are
 * deployed by agents and rendered inside the orb, coloured by [rarity].
 */
interface Mod : DeployableItem {
    val rarity: Rarity
    val abbr: String

    /** Resistance to being knocked out of its slot by an XMP/Ultra-Strike (0 = pops out easily; see
     *  [items.Combat.knockChance]). Low-tier items default to 0; shields override with their tier value. */
    val stickiness: Int get() = 0

    fun modType(): ModType
}
