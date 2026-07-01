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

    // Probed step fractions when the full step is blocked — creep as far toward the target as the gap allows
    // instead of holding. Recovers the pre-2×-speed reach: a doubled step (~5 px) that overshoots a thin
    // one-cell (≈10 px) passage into a wall still lands the agent inside the gap at a shorter fraction.
    private val CREEP_FRACTIONS = doubleArrayOf(1.0, 0.66, 0.33)

    /**
     * Keep an **agent** on walkable ground: only step into a cell that's **passable** — which also means
     * **on the grid** ([Pos.isPassable] is false off-map and on water/buildings). So an agent never walks on
     * water or off the playable map, even when chasing a destination outside it. When the diagonal is blocked
     * it **slides along one axis** (hug the wall); when even the full slide overshoots a thin passage it takes
     * a **shorter step** toward the target (creep through the gap) rather than freezing — only a true dead-end
     * corner (every probe blocked) holds, and [StuckTracker] re-targets if that persists. (NPCs roam off-screen
     * by design — they don't go through this.) Clamping to passability, not the round-field circle, so agents
     * can still reach portals placed in the passable corners outside the inscribed circle.
     */
    fun clampToPlayable(from: Pos, to: Pos): Pos {
        if (to.isPassable()) return to
        creepToward(from, Pos(to.x, from.y))?.let { return it } // slide horizontally (sub-stepped)
        creepToward(from, Pos(from.x, to.y))?.let { return it } // slide vertically (sub-stepped)
        creepToward(from, to)?.let { return it } // shortened diagonal, last resort before holding
        return from // truly boxed in on every probe → hold (StuckTracker will re-target if it persists)
    }

    // The passable point nearest [to] along the segment from [from], probing decreasing fractions; null when
    // every probe (down to a near-[from] nudge) is blocked. Lets the agent reach as far into a thin/overshot
    // passage as it can rather than holding at [from].
    private fun creepToward(from: Pos, to: Pos): Pos? {
        if (to == from) return null
        CREEP_FRACTIONS.forEach { f ->
            val p = Pos(from.x + ((to.x - from.x) * f).toInt(), from.y + ((to.y - from.y) * f).toInt())
            if (p != from && p.isPassable()) return p
        }
        return null
    }

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
    // Portals the agent is NOT already standing on (≥ a deployment range from the centre) — the valid "move
    // ELSEWHERE" targets. Picking a portal the agent is already at makes the MOVE arrive instantly → end → re-select
    // → MOVE again: an in-place oscillation that never relocates and also slips past StuckTracker (its MOVE-only
    // sampling resets on the 1-tick WAIT between each cycle). When EVERY portal is within range (a tiny/clustered
    // board) we wander instead, so the agent at least moves rather than spinning on the spot.
    private fun awayPortals(agent: Agent) = World.allPortals.filter { agent.distanceToPortal(it) >= Dim.maxDeploymentRange }

    fun moveToNearestPortal(agent: Agent): Agent {
        val target = awayPortals(agent).minByOrNull { agent.distanceToPortal2(it) } ?: return wander(agent)
        return goToDestinationPortal(agent, target)
    }

    fun moveToRandomPortal(agent: Agent): Agent {
        val away = awayPortals(agent)
        if (away.isEmpty()) return wander(agent)
        return goToDestinationPortal(agent, away[(Rng.random() * (away.size - 1)).toInt()])
    }
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
