package portal

import agent.Agent
import agent.Faction
import config.Dim
import items.deployable.Resonator

data class ResonatorSlot(var owner: Agent?, var resonator: Resonator?, var distance: Int) {
    fun isEmpty() = resonator == null
    fun isOwnedBy(agent: Agent) = owner == agent
    fun deployReso(deployer: Agent, reso: Resonator, dist: Int) {
        check(dist >= Dim.minDeploymentRange)
        check(dist <= Dim.maxDeploymentRange)
        check(deployer.faction != Faction.NONE)
        this.owner = deployer
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
