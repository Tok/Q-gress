package agent.action.cond

import World
import agent.Agent
import agent.Movement
import agent.action.ActionItem
import config.Config
import portal.Portal
import system.ChurnMath
import system.Com
import util.Rng
import util.data.Positions

/**
 * Discovery — the OTHER idle/fallback an agent does with no gameplay action left (the sibling of [Recruiter]). It
 * strolls to nearby open ground ([Movement.wander] → [ActionItem.EXPLORE]) and, on arrival ([Agent.wanderStep]),
 * [resolve]s a neutral, density-driven portal CHANGE: a new portal is **discovered**, or a random one is found
 * **gone** — converging the board's portal count toward [Config.targetPortals] (the pure curve is [ChurnMath]:
 * discovery dominates when sparse, removal when packed, ~1:1 at the target).
 *
 * This REPLACES the old per-checkpoint `Cycle.managePortalDensity`: portal churn is now something agents DO when
 * idle, not a disembodied tick — so it self-throttles (a busy board churns little; a sparse one floods discovery
 * via the no-portal wander fallback, exactly the bootstrap we want). Not a Q-slider, not in the action roulette.
 */
object Discoverer {
    /** Should an IDLE [agent] discover (vs go seek work)? Only [Config.maxConcurrentDiscoverers] per faction
     *  explore at once; the rest head off to find a portal. (When the board has no portals to seek, the wander
     *  fallback in [Agent.moveElsewhere] still routes idle agents here — flooding discovery when sparse.) */
    fun canDiscover(agent: Agent): Boolean {
        if (agent.action.item == ActionItem.EXPLORE) return false // already on it
        val active = World.allAgents.count { it.faction == agent.faction && it.action.item == ActionItem.EXPLORE }
        return active < Config.maxConcurrentDiscoverers
    }

    /** Start the stroll to open ground; the [Agent] drives the walk and calls [resolve] on arrival. */
    fun performAction(agent: Agent): Agent = Movement.wander(agent)

    /**
     * Arrived from a discovery stroll → roll the neutral, density-driven portal churn (create OR remove). When
     * there's no room for a non-clipping portal the create budget rolls into removal, so a board packed to its
     * walkable capacity thins out instead of stalling. The [minPortals]/[maxPortals] floors/ceilings hold either way.
     */
    fun resolve(agent: Agent) {
        if (!World.portalDiscoveryEnabled) return // title sim et al. keep a fixed board (the stroll still happens)
        val count = World.countPortals()
        val hasSpace = count < Config.maxPortals && Positions.hasPortalSpace()
        val churn = ChurnMath.churnChances(count, Config.targetPortals(), Config.portalChurnRate, hasSpace)
        if (hasSpace && Rng.random() < churn.create) {
            val discovered = Portal.createRandom()
            World.allPortals.add(discovered)
            Com.addMessage("${agent.name} discovered a new portal $discovered.", Com.Importance.MAJOR, Com.NEUTRAL)
        } else if (count > Config.minPortals && Rng.random() < churn.remove) {
            val gone = World.randomPortal()
            gone.remove()
            Com.addMessage("Portal $gone no longer exists.", Com.Importance.MAJOR, Com.NEUTRAL)
        }
    }
}
