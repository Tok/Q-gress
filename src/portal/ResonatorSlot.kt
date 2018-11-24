package portal

import agent.Agent
import items.deployable.Resonator

data class ResonatorSlot(var owner: Agent?, var resonator: Resonator?, var distance: Int) {
    fun isEmpty() = resonator == null
    fun isOwnedBy(agent: Agent) = owner == agent
    fun deployReso(owner: Agent, reso: Resonator, dist: Int) {
        this.owner = owner
        this.resonator = reso
        this.distance = dist
    }
    fun clear() {
        this.owner = null
        this.resonator = null
        this.distance = 0
    }
    override fun toString() = if (resonator != null) "[" + resonator.toString() + "]" else "[]"

    companion object {
        fun create() = ResonatorSlot(null, null, 0)
    }
}
