package items.deployable

import agent.Agent
import items.types.HeatSinkType
import items.types.Rarity

data class HeatSink(val type: HeatSinkType, val owner: Agent) : Mod {
    override val rarity: Rarity get() = type.rarity
    override val abbr: String get() = type.abbr
    override fun modType() = ModType.HEAT_SINK
    override fun toString() = type.abbr
    override fun getOwnerId(): String = owner.key()
    override fun getLevel(): Int = type.level
}
