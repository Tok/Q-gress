package items.deployable

import agent.Agent
import items.types.Rarity
import items.types.ShieldType

data class Shield(val type: ShieldType, val owner: Agent) : Mod {
    override val rarity: Rarity get() = type.rarity
    override val abbr: String get() = type.abbr
    override fun modType() = ModType.SHIELD
    override fun toString() = type.abbr
    override fun getOwnerId(): String = owner.key()
    override fun getLevel(): Int = type.level
}
