package items.deployable

import agent.Agent
import items.types.LinkAmpType
import items.types.Rarity

/** Inactive for now (not dropped, no gameplay effect) — defined so it can be modelled + drawn. */
data class LinkAmp(val type: LinkAmpType, val owner: Agent) : Mod {
    override val rarity: Rarity get() = type.rarity
    override val abbr: String get() = type.abbr
    override fun modType() = ModType.LINK_AMP
    override fun toString() = type.abbr
    override fun getOwnerId(): String = owner.key()
    override fun getLevel(): Int = type.level
}
