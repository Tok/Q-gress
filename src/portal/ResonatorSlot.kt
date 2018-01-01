package portal

import agent.Agent
import items.deployable.Resonator

data class ResonatorSlot(val owner: String?, val resonator: Resonator?, val distance: Int) {
    fun isEmpty() = resonator == null
    fun isOwnedBy(agent: Agent) = owner == agent.key()
}
