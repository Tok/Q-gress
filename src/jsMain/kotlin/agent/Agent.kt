package agent

import World
import agent.action.Action
import agent.action.ActionItem
import agent.action.ActionSelector
import agent.action.cond.Attacker
import agent.action.cond.Deployer
import agent.action.cond.Recruiter
import agent.qvalue.QDestinations
import config.Config
import config.Dim
import extension.Grid
import items.deployable.Resonator
import items.level.XmpLevel
import portal.Portal
import portal.XmMap
import system.audio.Sound
import system.effect.Fx
import system.ui.Bootstrap
import util.MathUtil
import util.NameGen
import util.Rng
import util.data.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class Agent(
    val faction: Faction,
    val name: String,
    val pos: Pos,
    val skills: Skills,
    val inventory: Inventory,
    val action: Action,
    var actionPortal: Portal,
    var destination: Pos,
    var ap: Int = 0,
    var xm: Int = 0,
    var velocity: Complex = Complex.ZERO,
    private var beelineTicks: Int = 0, // >0 = temporarily bee-line straight at the target, ignoring the (looping) field
    private var triedBeeline: Boolean = false, // a bee-line was already spent this stuck episode → escalate to re-target
    var recruitTargetId: Int? = null, // the NPC being recruited (walk up → meet → resolve); null = not recruiting
) {
    // Distance actually moved last tick (sim px), recomputed by act() each tick. NOT a constructor param, so a
    // movement copy() never carries a stale value. Drives the AGENTS-table speed readout (px/tick → m/s).
    var stepPx: Double = 0.0

    // STABLE identity for equals/hashCode + Scene3D keys. Must NOT include mutable state: toString()
    // embeds getLevel() (from the var ap), so keying on it changed an agent's hashCode when it levelled
    // up — corrupting the allAgents/frogs/smurfs sets ("Have object hashCodes changed?"). faction+name are
    // vals and survive the movement copy(), so they identify the same agent across moves + level-ups.
    fun key() = faction.abbr + "-" + name
    private fun distanceToDestination(): Double = pos.distanceTo(destination)
    fun distanceToPortal(portal: Portal): Double = pos.distanceTo(portal.location)
    fun distanceToPortal2(portal: Portal): Double = pos.distanceTo2(portal.location) // squared — for nearest/sort only
    fun isAtActionPortal(): Boolean = distanceToPortal(actionPortal) < Dim.maxDeploymentRange

    fun getLevel(): Int = getLevel(this.ap)
    fun xmCapacity(): Int = xmCapacity(getLevel())

    private fun calcAbsXmBar() = min(xmCapacity(), max(0, xm))
    private fun xmBarPercent() = calcAbsXmBar() * 100 / xmCapacity()
    private fun isXmBarEmpty() = xmBarPercent() == 0
    fun isXmFilled() = xmBarPercent() >= 80
    fun keySet() = inventory.findUniqueKeys().orEmpty()

    fun removeXm(v: Int) {
        this.xm = MathUtil.clip(xm - v, 0, xmCapacity())
    }

    fun addXm(v: Int) {
        this.xm = MathUtil.clip(xm + v, 0, xmCapacity())
    }

    fun addAp(v: Int) {
        val before = getLevel()
        this.ap += (v * Config.apMultiplier * Config.progressSpeed).roundToInt() // progressSpeed → faster levelling
        if (faction == World.userFaction && getLevel() > before) Sound.playLevelUp(pos) // player agent dinged up
    }

    fun act(): Agent {
        val next = when {
            action.item == ActionItem.ATTACK -> attackPortal(false)
            action.item == ActionItem.DEPLOY -> deployPortal(false)
            action.item == ActionItem.MOVE -> moveCloserToDestinationPortal()
            action.item == ActionItem.EXPLORE -> wanderStep() // roam toward open ground (the no-idle fallback)
            action.item == ActionItem.RECRUIT -> recruitStep() // walk up to the NPC, then meet (handled even while "busy")
            // WAIT is never a chosen behaviour — it's only the default/just-ended state. Re-select a real action
            // immediately (recruit / seek a portal) so agents never sit idle, even on the title scenes.
            action.item == ActionItem.WAIT -> ActionSelector.doSomethingElse(this)
            action.isBusy() -> this
            else -> ActionSelector.doSomethingElse(this)
        }
        next.collectXm()
        next.stepPx = pos.distanceTo(next.pos) // actual ground covered this tick → the AGENTS-table speed readout
        return next
    }

    fun moveElsewhere(): Agent {
        // Default is to go FIND a portal — a RANDOM one, so a blocked agent keeps getting fresh headings and
        // frees itself — never an aimless wander / idle wait. Wander only when the board has no portals at all.
        val newAgent = Rng.select(moveOptions(), { seekRandomPortalOrWander() }).invoke()
        // Force MOVE for the portal-seeking picks (incl. their no-op paths), but leave a wander fallback on its
        // own EXPLORE action so it actually roams open ground instead of heading to a portal.
        if (newAgent.action.item != ActionItem.EXPLORE) newAgent.action.start(ActionItem.MOVE)
        return newAgent
    }

    // The no-portal-work fallback: head for a random portal (always available; a fresh heading each time, so a
    // wedged agent slides free) — only roam open ground when the board genuinely has no portals.
    private fun seekRandomPortalOrWander(): Agent =
        if (World.allPortals.isNotEmpty()) Movement.moveToRandomPortal(this) else Movement.wander(this)

    // The weighted destination options for [moveElsewhere] — each gated by what's reachable/possible.
    private fun moveOptions(): List<Pair<Double, () -> Agent>> {
        val agent = this
        val hasEnemyPortals = Movement.hasEnemyPortals(agent)
        with(QDestinations) {
            val randomQ = ActionSelector.q(faction, MOVE_TO_RANDOM)
            val nearQ = ActionSelector.q(faction, MOVE_TO_NEAR)
            val uncapturedQ =
                if (Movement.hasUncapturedPortals()) ActionSelector.q(faction, MOVE_TO_UNCAPTURED) else -1.0
            val friendlyQ =
                if (Movement.hasFriendlyPortals(agent)) ActionSelector.q(faction, MOVE_TO_MOST_FRIENDLY) else -1.0
            val nearEnemyQ = if (hasXmps() && hasEnemyPortals) ActionSelector.q(faction, MOVE_TO_NEAR_ENEMY) else -1.0
            val weakEnemyQ = if (hasXmps() && hasEnemyPortals) ActionSelector.q(faction, MOVE_TO_WEAK_ENEMY) else -1.0
            val strongEnemyQ =
                if (hasXmps() && hasEnemyPortals) ActionSelector.q(faction, MOVE_TO_STRONG_ENEMY) else -1.0
            return listOf(
                randomQ to { Movement.moveToRandomPortal(agent) },
                nearQ to { Movement.moveToNearestPortal(agent) },
                uncapturedQ to { Movement.moveToUncapturedPortal(agent) },
                friendlyQ to { Movement.moveToFriendlyHighLevelPortal(agent) },
                nearEnemyQ to { Movement.attackClosePortal(agent) },
                weakEnemyQ to { Movement.attackMostVulnerablePortal(agent) },
                strongEnemyQ to { Movement.attackMostLinkedPortal(agent) },
            )
        }
    }

    /**
     * Un-stick a travelling agent flagged by [StuckTracker] (looping / wedged for a full window).
     * Escalates: first spend a **bee-line** (walk straight at the target, ignoring the looping field);
     * if that doesn't free it (e.g. wedged against the play-area edge), **re-target** a fresh portal.
     * Called on the ~minute checkpoint cadence (see [system.Cycle]); a no-op when not stuck.
     */
    fun recoverIfStuck() {
        if (!StuckTracker.isStuck(key())) {
            triedBeeline = false
            return
        }
        if (beelineTicks > 0) return // a bee-line is already underway — let it run its course
        if (triedBeeline) {
            val newDest = World.randomPortal()
            val dist = skills.deployPrecision * Dim.maxDeploymentRange
            this.actionPortal = newDest
            this.destination = newDest.findRandomPointNearPortal(dist.toInt())
            triedBeeline = false
        } else {
            beelineTicks = BEELINE_DURATION
            triedBeeline = true
        }
    }

    private fun moveCloserToDestinationPortal(): Agent {
        if (!World.isReady) {
            console.warn("World is not ready.")
            return this // init/teardown only — keep the action; re-selecting here would touch a half-built World
        }
        if (isAtActionPortal()) {
            action.end()
            beelineTicks = 0
            triedBeeline = false
            return this
        }
        val beelining = beelineTicks > 0
        if (beelining) beelineTicks--
        val force = if (beelining) {
            Movement.headingTo(pos, actionPortal.location) // un-stick override: straight line through the spiral
        } else {
            actionPortal.vectors[pos.toShadow()] ?: Movement.headingTo(pos, actionPortal.location)
        }
        velocity = Movement.move(velocity, force, skills.speed)
        return stepByVelocity()
    }

    // Apply the integrated [velocity] one tick, kept on passable ground. If a wall DEFLECTS the step (the clamp
    // couldn't reach the intended target), drop the into-wall momentum so the wall-aware flow field re-steers
    // cleanly NEXT tick instead of grinding into the wall for several ticks while momentum bleeds off — the key
    // to threading tight corners and not wedging. A sub-pixel tick (target == pos, velocity still building from
    // rest) keeps its velocity so the agent still accelerates.
    private fun stepByVelocity(): Agent {
        val target = Pos((pos.x + velocity.re).toInt(), (pos.y + velocity.im).toInt())
        val next = Movement.clampToPlayable(pos, target)
        if (target != pos && next != target) velocity = Complex(next.x - pos.x, next.y - pos.y)
        return this.copy(pos = next)
    }

    // The no-idle stroll (ActionItem.EXPLORE): roam straight toward a nearby open-ground [destination],
    // sweeping up stray XM on the way; end on arrival so the agent re-evaluates (likely able to act again).
    private fun wanderStep(): Agent {
        if (!World.isReady || distanceToDestination() <= skills.inRangeSpeed()) {
            action.end()
            return this
        }
        velocity = Movement.move(velocity, Movement.headingTo(pos, destination), skills.speed)
        return stepByVelocity()
    }

    // Recruiting (ActionItem.RECRUIT): walk straight up to the target NPC (holding it in place), then stand
    // together for the meeting (the action timer); when it runs out, [Recruiter.resolve] rolls success + sound.
    private fun recruitStep(): Agent {
        val npc = recruitTargetId?.let { id -> World.allNonFaction.firstOrNull { it.id == id } }
        if (npc == null) { // target recruited by someone else / churned away → abort + re-select THIS tick
            recruitTargetId = null
            action.end()
            return ActionSelector.doSomethingElse(this)
        }
        destination = npc.pos
        npc.holdInPlace(World.tick + RECRUIT_HOLD_TICKS) // keep them waiting while we approach + meet
        if (distanceToDestination() > Dim.maxDeploymentRange) {
            action.start(ActionItem.RECRUIT) // keep the meeting timer fresh until we actually arrive
            velocity = Movement.move(velocity, Movement.headingTo(pos, destination), skills.speed)
            return stepByVelocity()
        }
        if (action.isBusy()) return this // standing together — the meeting (the head bobs in the render)
        // Meeting over → roll the result, then re-select THIS tick. [Recruiter.resolve] ends the action (→ WAIT for
        // a tick); re-selecting now means that transitional WAIT is never rendered, so its empty pill can't flash in
        // between the no-pill recruit/roam states (the agent lands straight on its next action this frame).
        return ActionSelector.doSomethingElse(Recruiter.resolve(this, npc))
    }

    private fun hasXmps() = inventory.findXmps().isNotEmpty()

    private fun isArrived() = distanceToDestination() <= skills.inRangeSpeed()

    // The precise final approach to the in-range spot (attack/deploy). Steered by the PORTAL'S flow field so it
    // rounds walls toward the target instead of grinding straight into them — the old straight-line approach
    // wedged against walls and hopped in place forever (agents carrying many XMPs re-attacked the same
    // unreachable spot endlessly). Slow (inRangeSpeed) for precision; straight heading until the field lands.
    private fun moveCloserInRange(): Agent {
        val force = actionPortal.vectors[pos.toShadow()] ?: Movement.headingTo(pos, destination)
        velocity = Movement.move(velocity, force, skills.inRangeSpeed())
        return stepByVelocity()
    }

    private fun collectXm() {
        val heaps = XmMap.findXmInRange(pos)
        heaps.forEach { heap ->
            if (xm < xmCapacity()) {
                addXm(heap.value.xm)
                heap.value.collect()
                Fx.sink.collectXmFx(heap.key, pos) // mote flies to the agent (no-op headless)
            }
        }
    }

    fun attackPortal(isFirst: Boolean): Agent {
        if (isFirst) {
            action.start(ActionItem.ATTACK)
            fun findExactDestination(): Pos {
                if (actionPortal.calcHealth() > 0.8) {
                    return actionPortal.location // center
                }
                val maybeDestination = actionPortal.findStrongestResoPos()
                return if (maybeDestination != null && maybeDestination.isPassable()) {
                    maybeDestination
                } else {
                    actionPortal.location // center
                }
            }

            val inRangePosition = findExactDestination()
            this.destination = inRangePosition
        }

        return when {
            !isArrived() && action.isBusy() -> moveCloserInRange()
            // Target fell (resos gone — incl. our own takedown) or flipped friendly: stop attacking nothing.
            // End the action so act() re-selects next tick, instead of re-firing forever on a dead portal.
            !Attacker.isTargetValid(this) -> {
                action.end()
                this
            }
            Attacker.isActionPossible(this) -> Attacker.performAction(this)
            // Worthy target but too few XMPs right now — don't idle: end so act() re-selects (likely HACK to earn
            // XMPs, or recruit / seek another portal). Resting wouldn't yield XMPs anyway (those come from hacking).
            else -> {
                action.end()
                this
            }
        }
    }

    fun capturePortal(isFirst: Boolean) = deployPortal(isFirst)
    fun deployPortal(isFirst: Boolean): Agent {
        if (isFirst) {
            action.start(ActionItem.DEPLOY)
            val distance =
                max(Dim.minDeploymentRange, Dim.maxDeploymentRange * Rng.random() * skills.deployPrecision).toInt()
            val dest = actionPortal.findRandomPointNearPortal(distance)
            this.destination = dest
        }

        return when {
            !isArrived() && action.isBusy() -> moveCloserInRange()
            else -> {
                if (Deployer.isActionPossible(this)) {
                    Deployer.performAction(this)
                } else {
                    action.end() // can't deploy yet (low XM) → re-select (recharge / hack to earn XM) rather than idle
                    this
                }
            }
        }
    }

    private fun findPortalsInAttackRange(level: XmpLevel): List<Portal> =
        enemyPortalsInRange(World.allPortals, faction, pos, (level.rangeM * 0.5) + Dim.portalRadius)

    fun findResosInAttackRange(level: XmpLevel): List<Resonator> {
        val attackDistance = level.rangeM * 0.5
        val portals = findPortalsInAttackRange(level)
        val slots = portals.flatMap { it.slots.map { s -> s.value } }
        val resosInRange =
            slots.filter { it.resonator?.position?.distanceTo(this.pos) ?: attackDistance * 2 <= attackDistance }
        return resosInRange.map { it.resonator }.filterNotNull()
    }

    override fun toString() = "L" + getLevel() + " " + faction.abbr + "-" + name
    override fun equals(other: Any?) = other is Agent && this.key() == other.key()
    override fun hashCode() = this.key().hashCode() * 31

    companion object {
        private val BEELINE_DURATION = StuckTracker.RECOVERY_BEELINE_TICKS // ticks to bee-line before re-targeting
        private const val RECRUIT_HOLD_TICKS = 8 // re-applied each tick → the target NPC waits while approached + met

        /** Pure: enemy (non-[faction]) portals within [attackDistance] of [from], nearest first. Takes the
         *  portal list as a parameter (not the `World` singleton) so the targeting filter is unit-testable. */
        internal fun enemyPortalsInRange(portals: List<Portal>, faction: Faction, from: Pos, attackDistance: Double): List<Portal> =
            portals.filter { it.owner?.faction != faction }
                .filter { it.location.distanceTo(from) <= attackDistance }
                .sortedBy { it.location.distanceTo(from) }

        private fun xmCapacity(level: Int): Int = when (level) {
            1 -> 3000
            2 -> 4000
            3 -> 5000
            4 -> 6000
            5 -> 7000
            6 -> 8000
            7 -> 9000
            8 -> 10000
            9 -> 10900
            10 -> 11700
            11 -> 12400
            12 -> 13000
            13 -> 13500
            14 -> 13900
            15 -> 14200
            else -> 14400
        }

        private fun getLevel(actionPoints: Int): Int = when (actionPoints) {
            in 0..10000 -> 1
            in 10000..30000 -> 2
            in 30000..70000 -> 3
            in 70000..150000 -> 4
            in 150000..300000 -> 5
            in 300000..600000 -> 6
            in 600000..1200000 -> 7
            in 1200000..2400000 -> 8
            in 2400000..4000000 -> 9 // + 1 gold 4 silver
            in 4000000..6000000 -> 10 // + 2 gold 5 silver
            in 6000000..8400000 -> 11 // + 4 gold 6 silver
            in 8400000..12000000 -> 12 // + 6 gold 7 silver
            in 12000000..17000000 -> 13 // + 1 Platinum 7 Gold
            in 17000000..24000000 -> 14 // + 2 Platinum 7 Gold
            in 24000000..40000000 -> 15 // + 3 Platinum 7 Gold
            else -> 16 // TODO + 2 Black 4 Platinum 7 Gold
        }

        private fun getLinkingRange(level: Int): Int = when (level) {
            9 -> 2250
            10 -> 2500
            11 -> 2750
            12 -> 3000
            13 -> 3250
            14 -> 3500
            15 -> 3750
            16 -> 4000
            else -> 2000
        }

        // Prefer an existing world portal (browser + headless): a fresh agent shouldn't mint a throwaway
        // portal — that also means a headless match (ai.SimRunner) doesn't pay a flow-field compute per
        // agent/recruit. Only the degenerate "no portals yet" case falls back to creating one.
        private fun initialActionPortal(pos: Pos) = Portal.nearestTo(pos) ?: Portal.create(pos)

        fun createFrog(grid: Grid, at: Pos? = null) = create(grid, Faction.ENL, at)
        fun createSmurf(grid: Grid, at: Pos? = null) = create(grid, Faction.RES, at)

        // [at] places the agent at a specific spot (a recruited NPC turning faction in place); null → random.
        private fun create(grid: Grid, faction: Faction, at: Pos? = null): Agent {
            val ap = Config.initialAp()
            val initialXm = xmCapacity(getLevel(ap))
            val coords = at ?: Positions.createRandomPassable(grid)
            val actionPortal = initialActionPortal(coords)
            val agent = Agent(
                faction, NameGen.handle(faction, Bootstrap.locationName()), coords, Skills.createRandom(),
                Inventory.empty(), Action.create(), actionPortal, actionPortal.location,
                ap, initialXm,
            )
            agent.inventory.items.addAll(Inventory.startingGear(agent, Config.startStage))
            return agent
        }
    }
}
