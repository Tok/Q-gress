package items.deployable

import agent.Agent
import items.level.ResonatorLevel
import portal.Octant
import portal.Portal
import util.data.Coords
import kotlin.math.max

data class Resonator(val level: ResonatorLevel, val owner: Agent, var energy: Int,
                     var portal: Portal? = null, var octant: Octant? = null, var coords: Coords? = null) : DeployableItem {
    //TODO move location and octant to ResonatorSlot
    fun calcHealthPrecent() = energy * 100 / level.energy
    fun isAtCriticalLevel() = calcHealthPrecent() < 20
    fun recharge(agent: Agent, xm: Int) {
        val rest = max((energy + xm) - level.energy, 0)
        val energy = xm - rest
        agent.removeXm(energy)
        agent.addAp(10) //FIXME
        this.energy += energy
    }
    private fun decayEnergy() = (level.energy * DECAY_RATIO).toInt()

    fun decay() {
        energy = energy - decayEnergy()
        if (energy <= 0) {
            portal?.removeReso(octant!!, null)
        }
    }

    fun takeDamage(agent: Agent, damage: Int) {
        this.energy = energy - damage
        if (energy <= 0) {
            agent.ap = agent.ap + 75
            portal?.removeReso(octant!!, agent)
        }
    }

    fun deploy(portal: Portal, octant: Octant, coords: Coords) {
        this.portal = portal
        this.octant = octant
        this.coords = coords
    }

    override fun toString() = "R" + level.level
    override fun getOwnerId(): String = owner.key()
    override fun getLevel(): Int = level.level

    companion object {
        val DECAY_RATIO = 0.15
        fun create(level: ResonatorLevel, agent: Agent) = Resonator(level, agent, level.energy)
        fun create(level: Int, agent: Agent) = create(ResonatorLevel.valueOf(level), agent)
    }
}
