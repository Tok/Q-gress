package items.deployable

import agent.Agent
import items.level.ResonatorLevel
import portal.Octant
import portal.Portal
import util.data.Coords

data class Resonator(val level: ResonatorLevel, val owner: Agent, var health: Int = MAX_HEALTH,
                     var portal: Portal? = null, var octant: Octant? = null, var coords: Coords? = null) : DeployableItem {
    //TODO move location and octant to ResonatorSlot
    fun isAtCriticalLevel() = health < 20
    fun recharge(xm: Int) {
        health = health + xm
    }

    fun decay() {
        health = health - (health * DECAY_RATIO).toInt()
        if (health <= 0) {
            portal?.removeReso(octant!!, null)
        }
    }

    fun takeDamage(agent: Agent, damage: Int) {
        this.health = health - damage
        if (health <= 0) {
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

    companion object {
        val MAX_HEALTH = 100
        val DECAY_RATIO = 0.15
        fun create(level: ResonatorLevel, agent: Agent) = Resonator(level, agent)
        fun create(level: Int, agent: Agent) = create(ResonatorLevel.valueOf(level), agent)
    }
}
