package agent
import World
import agent.action.ActionItem
import config.Dim
import config.Sim
import items.level.PortalLevel
import portal.Portal
import util.Rng
import util.data.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Movement {
    private const val WANDER_RADIUS = 220.0 // px — a regional stroll to collect stray XM, not a map-wide teleport
    private const val WANDER_TRIES = 12 // samples to find a wanderable point before falling back

    // Flow-field cells read empty while the field computes async (see Pathfinding.computeFieldAsync).
    // Until it lands, head straight for the destination at unit magnitude so the agent keeps moving
    // (never stalls on Complex.ZERO → stuck → never re-targets) and re-samples the real field later.
    fun headingTo(from: Pos, to: Pos): Complex = MovementMath.headingTo(from, to)

    fun findUncapturedPortals() = World.allPortals.filter { it.isUncaptured() }
    fun hasUncapturedPortals() = findUncapturedPortals().isNotEmpty()
    fun findEnemyPortals(agent: Agent): List<Portal> = World.allPortals.filter { it.isEnemyOf(agent) }
    fun hasEnemyPortals(agent: Agent) = findEnemyPortals(agent).isNotEmpty()
    fun findFriendlyPortals(agent: Agent) = World.allPortals.filter { it.isFriendlyTo(agent) }
    fun hasFriendlyPortals(agent: Agent) = findFriendlyPortals(agent).isNotEmpty()

    /* Uncaptured Portals */
    fun moveToUncapturedPortal(agent: Agent): Agent {
        check(hasUncapturedPortals())
        val uncaptured = World.allPortals.filter { it.isUncaptured() }.sortedBy { agent.distanceToPortal2(it) }
        uncaptured.forEach { portal ->
            if (Rng.random() < agent.skills.reliability) {
                return goToDestinationPortal(agent, portal)
            }
        }
        return agent
    }
    /* End Uncaptured Portals */

    /* Friendly Portals */
    fun moveToFriendlyHighLevelPortal(agent: Agent): Agent {
        check(hasFriendlyPortals(agent))
        val friendlyPortals = World.allPortals.filter { it.isFriendlyTo(agent) }
        val maxLevel = friendlyPortals.maxByOrNull { it.getLevel() }?.getLevel() ?: PortalLevel.ZERO
        val selection = friendlyPortals.filter { it.getLevel() == maxLevel }
        val target = selection[(Rng.random() * (selection.size - 1)).toInt()]
        return goToDestinationPortal(agent, target)
    }
    /* End Friendly Portals */

    /* Enemy Portals */
    fun attackClosePortal(a: Agent) = goAttack(a, findEnemyPortals(a).minByOrNull { a.distanceToPortal2(it) })

    fun attackMostLinkedPortal(a: Agent) = goAttack(a, findEnemyPortals(a).sortedBy { it.links.size }.firstOrNull())
    fun attackMostVulnerablePortal(a: Agent) = goAttack(a, findEnemyPortals(a).sortedBy { -it.calcHealth() }.firstOrNull())
    private fun goAttack(agent: Agent, target: Portal?): Agent = if (target != null) {
        goToDestinationPortal(agent, target)
    } else {
        agent.action.end()
        agent
    }
    /* End Enemy Portals */

    /* All Portals */
    fun moveToNearestPortal(agent: Agent): Agent {
        val target = World.allPortals.minByOrNull { agent.distanceToPortal2(it) } ?: return agent
        return goToDestinationPortal(agent, target)
    }

    fun moveToRandomPortal(agent: Agent): Agent = goToDestinationPortal(agent, World.randomPortal())
    /* End All Portals */

    /**
     * The no-idle fallback — a portal-independent action. With nothing productive to do (portal on cooldown,
     * empty inventory, capped roster), the agent strolls to nearby open ground rather than waiting: it covers
     * the area and sweeps up stray XM (energy) faster than standing still, which is exactly what it needs to
     * act again. Genuine roaming (no portal target) — discovery/recruiting happen wherever the agent ends up.
     */
    fun wander(agent: Agent): Agent {
        agent.action.start(ActionItem.EXPLORE)
        return agent.copy(destination = openGroundNear(agent.pos))
    }

    // A "wanderable" point a short stroll from [from] — passable AND inside the play area (same grid + field
    // bounds the rest of the sim uses, so a wandering agent never strays off-map / off-field the way loose NPCs
    // can). Samples the ring around [from]; falls back to anywhere wanderable, then to staying put (re-evaluate).
    private fun openGroundNear(from: Pos): Pos {
        repeat(WANDER_TRIES) {
            val angle = Rng.random() * 2.0 * PI
            val dist = Dim.maxDeploymentRange + Rng.random() * WANDER_RADIUS
            val point = Pos((from.x + dist * cos(angle)).toInt(), (from.y + dist * sin(angle)).toInt())
            if (isWanderable(point)) return point
        }
        repeat(WANDER_TRIES) {
            val point = Positions.createRandomPassable(World.grid)
            if (isWanderable(point)) return point
        }
        return from
    }

    private fun isWanderable(p: Pos) = p.isPassable() && Sim.isInPlayArea(p.x, p.y)

    private fun goToDestinationPortal(agent: Agent, destination: Portal): Agent {
        val distance = agent.skills.deployPrecision * Dim.maxDeploymentRange
        val nextDest = destination.findRandomPointNearPortal(distance.toInt())
        agent.action.start(ActionItem.MOVE)
        return agent.copy(actionPortal = destination, destination = nextDest)
    }

    fun move(velocity: Complex, force: Complex, limit: Double): Complex = MovementMath.move(velocity, force, limit)
}
