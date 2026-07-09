package agent.action.cond

import World
import agent.Agent
import agent.Movement
import agent.action.ActionItem
import config.Config
import config.Time
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
 * This REPLACES the old per-checkpoint `Cycle.managePortalDensity`: portal churn is something agents DO when idle,
 * not a disembodied tick. Its **cadence**, though, is per unit of sim time — an arrival only *samples* the churn
 * process, it no longer drives it, so the board's churn rate doesn't depend on how many wanders happen to finish
 * (see [resolve]). Which agent discovers, and whether the board fills or thins, still belong to the agents and to
 * [ChurnMath]'s density curve. Not a Q-slider, not in the action roulette.
 */
object Discoverer {
    /** Should an IDLE [agent] discover (vs go seek work)? Only [Config.maxConcurrentDiscoverers] per faction
     *  explore at once; the rest head off to find a portal. (When the board has no portals to seek, the wander
     *  fallback in [Agent.moveElsewhere] still routes idle agents here — so a sparse board is sampled often, and
     *  [ChurnMath]'s curve then makes those samples overwhelmingly discoveries.) */
    fun canDiscover(agent: Agent): Boolean {
        if (agent.action.item == ActionItem.EXPLORE) return false // already on it
        val active = World.allAgents.count { it.faction == agent.faction && it.action.item == ActionItem.EXPLORE }
        return active < Config.maxConcurrentDiscoverers
    }

    /** Start the stroll to open ground; the [Agent] drives the walk and calls [resolve] on arrival. */
    fun performAction(agent: Agent): Agent = Movement.wander(agent)

    /** Sim time worth one whole churn roll — the board's churn CADENCE, independent of how often a stroll ends.
     *  Calibrated to the historical rate: a 1800-tick headless match rolled ~24 times, i.e. one per ~75 ticks. */
    private val CHURN_PERIOD_TICKS = Time.secondsToTicks(75)

    private var lastRollTick = 0

    /** Drop the churn clock — a fresh world/match starts its cadence at tick 0 (see [ai.SimRunner.reset]). */
    fun reset() {
        lastRollTick = 0
    }

    /**
     * Arrived from a discovery stroll → roll the neutral, density-driven portal churn (create OR remove). When
     * there's no room for a non-clipping portal the create budget rolls into removal, so a board packed to its
     * walkable capacity thins out instead of stalling. The [minPortals]/[maxPortals] floors/ceilings hold either way.
     *
     * The roll is weighted by the sim time since the last one ([ChurnMath.perElapsed]), so churn is a rate per
     * unit time that an arrival merely *samples* — a board whose agents stroll constantly no longer churns faster
     * than one whose agents are busy. It used to be one whole roll per arrival, which tied the board's churn to
     * how many wanders happened to finish: wedged wanderers held the discoverer slots forever and throttled it.
     */
    fun resolve(agent: Agent) {
        if (!World.portalDiscoveryEnabled) return // title sim et al. keep a fixed board (the stroll still happens)
        if (World.tick < lastRollTick) lastRollTick = World.tick // a new world rewound the clock
        val elapsed = World.tick - lastRollTick
        if (elapsed <= 0) return // two strollers home on the same tick → the second adds no churn
        lastRollTick = World.tick
        val count = World.countPortals()
        val hasSpace = count < Config.maxPortals && Positions.hasPortalSpace()
        val churn = ChurnMath.perElapsed(
            ChurnMath.churnChances(count, Config.targetPortals(World.walkability), Config.portalChurnRate, hasSpace),
            elapsed,
            CHURN_PERIOD_TICKS,
        )
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
