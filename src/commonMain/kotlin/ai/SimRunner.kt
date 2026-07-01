package ai

import World
import agent.Agent
import agent.Faction
import agent.NonFaction
import agent.StuckTracker
import config.Config
import config.Sim
import config.StartStage
import extension.Grid
import portal.Portal
import portal.XmMap
import system.Checkpoint
import system.CheckpointStats
import system.Com
import system.Cycle
import system.Simulation
import system.audio.Snd
import system.effect.Fx
import system.grid.FieldFlow
import system.grid.Nav
import system.grid.SyncFieldFlow
import util.NameGen
import util.Rng
import util.data.Pos

/**
 * The outcome of a headless match — the per-checkpoint winner history is the **fitness signal** (PLAN
 * Phase 6.1): a cycle is won by leading the MOST checkpoints (the [system.CheckpointStats] rule), so
 * [checkpointFitness] scores the net checkpoints won across the cycle, with the summed MU margin only as a
 * sub-integer tiebreak — reward consistently leading, not one blowout.
 */
data class MatchResult(
    val seed: Int,
    val ticks: Int,
    val checkpoints: List<Checkpoint>, // sorted by tick (oldest → newest)
    val finalEnlMu: Int,
    val finalResMu: Int,
) {
    fun checkpointMuSum(faction: Faction): Int = checkpoints.sumOf { it.mu(faction) }

    fun checkpointMuAvg(faction: Faction): Double =
        if (checkpoints.isEmpty()) 0.0 else checkpointMuSum(faction).toDouble() / checkpoints.size

    /** Net checkpoints [faction] led across the cycle (won minus lost; ties count for neither). */
    fun checkpointWinMargin(faction: Faction): Int {
        val tally = CheckpointStats.tally(checkpoints)
        return tally.getValue(faction) - tally.getValue(faction.enemy())
    }

    /**
     * The bake/eval fitness for [faction]: net checkpoints won, plus a fractional MU-margin tiebreak (kept ≪ 1
     * by [MU_TIEBREAK_SCALE]) so more checkpoint wins always ranks higher and equal win-counts break toward the
     * bigger fields. The objective the champion bake + [Tournament] optimize.
     */
    fun checkpointFitness(faction: Faction): Double =
        checkpointWinMargin(faction) + (checkpointMuSum(faction) - checkpointMuSum(faction.enemy())) / MU_TIEBREAK_SCALE

    /** The faction that won the cycle (led the most checkpoints), or null on a tie. */
    fun winner(): Faction? {
        val margin = checkpointWinMargin(Faction.ENL)
        return when {
            margin > 0 -> Faction.ENL
            margin < 0 -> Faction.RES
            else -> null
        }
    }

    companion object {
        // Large enough that the MU-margin tiebreak stays a fraction < 1 (so it never overturns a checkpoint-win
        // difference), while still ordering genomes that tie on checkpoint wins toward the bigger fields.
        const val MU_TIEBREAK_SCALE = 1_000_000.0
    }
}

/**
 * How a match starts: roster sizes (defaults mirror the live game's onboarding) and the movement model.
 *
 * [flowFields] = false (the default) runs **straight-line movement** — agents head directly for their
 * target (the live game's no-field fallback). The AI only consumes portal/agent/field *stats*
 * ([ai.Observation]), never cell data, so obstacle-routed navigation doesn't change what it learns; off is
 * the right default for training. Set true to spot-check against the live game's exact movement (pays an
 * O(cells) flow-field compute per portal). Note: match speed comes from the O(1) spawn pick in
 * `Positions.createRandomPassable`, not this toggle — a full match runs in ~tens of ms either way.
 */
data class MatchSetup(
    val portals: Int = Config.startPortals,
    val frogs: Int = DEFAULT_ROSTER,
    val smurfs: Int = DEFAULT_ROSTER,
    val npcs: Int = DEFAULT_NPCS,
    val flowFields: Boolean = false,
    // Start agents with a full inventory + AP (the game's "quick start"). On by default for matches because
    // empty-handed, level-1, 0-AP agents almost never get as far as creating a FIELD in a bounded match —
    // so MU (the fitness signal) stays flat 0 for both sides and there's nothing for the AI to learn from.
    val quickStart: Boolean = true,
    // Clean-eval: run with the anti-runaway mechanics OFF (comebackMax / dominanceDecay / leaderDistraction = 0).
    // They deliberately muddy a leader's advantage to keep games close — great for play, but they add noise to
    // the training/ranking signal, so an eval can ask for a cleaner gradient. SimRunner restores them after.
    val cleanEval: Boolean = false,
    // Pin the player-tunable gameplay balance (combat dynamics / progress speed / portal churn) to the shipped
    // DEFAULTS for the match, ignoring whatever the player has moved the live menu sliders to. ON by default so
    // training/eval has ONE canonical target — champions are "one fits all", not a champion-per-balance matrix.
    // SimRunner snapshots + restores the live values around the match. Opt out (false) only when the caller is
    // itself sweeping balance (e.g. BalanceSweep). (Drop rates aren't player-tunable yet; extend when they are.)
    val useDefaultBalance: Boolean = true,
) {
    companion object {
        const val DEFAULT_NPCS = 30
        const val DEFAULT_ROSTER = 8 // a full quick-start roster per faction
    }
}

/**
 * The headless match harness (PLAN Phase 6.1) — runs a full simulation in Node (no browser): seed the
 * RNG, install a grid, seed portals/agents/NPCs, then tick through the shared [Simulation.stepEntities]
 * + [Cycle] scoring, capturing per-checkpoint MU. Deterministic (same seed + grid → same result), so it
 * powers AI training/evaluation (6.2+). Effects stay on the default headless [Fx] sink (no-op); flow
 * fields compute synchronously ([Config.headlessFieldCompute]).
 *
 * The functional-core/imperative-shell split made this possible: Stage 1 (the effect sink) stopped the
 * renderer crashing headless, Stage 2 ([util.Pathfinding.computeFieldSync]) gave deterministic inline
 * pathfinding, and [Simulation.stepEntities] is the shared tick core the live game also runs.
 */
object SimRunner {
    /**
     * The flow-field compute a match binds when it opts into fields. Defaults to the pure commonMain
     * [SyncFieldFlow] (inline compute — headless Node/JVM matches + the in-Node trainer); the jsMain shell
     * swaps in the frame-yielding `PathFieldFlow` at boot (`Bootstrap.load`) so in-browser evals stay async.
     */
    var fieldFlow: FieldFlow = SyncFieldFlow

    /**
     * Run a deterministic match and return its per-checkpoint MU history. [grid] is the passability grid
     * (build one from a `GridFixture`); [maxTicks] is the match length (checkpoints land every
     * [Config.ticksPerCheckpoint]). [setup] sizes the starting roster; optional [policyEnl]/[policyRes]
     * install AI/slider policies per faction.
     */
    @Suppress("LongParameterList") // a harness entry point: grid + seed + length + roster + 2 policies + a debug hook
    fun runMatch(
        grid: Grid,
        seed: Int,
        maxTicks: Int,
        setup: MatchSetup = MatchSetup(),
        policyEnl: FactionPolicy? = null,
        policyRes: FactionPolicy? = null,
        onTick: ((Int) -> Unit)? = null, // called after each tick's scoring (for determinism/debug inspection)
    ): MatchResult {
        reset()
        Rng.seed(seed)
        Nav.bind(fieldFlow) // matches compute flow fields when flowFields is on (sync headless; async in-browser)
        Config.headlessFieldCompute = setup.flowFields // off by default → cheap abstract (straight-line) movement
        // END = full L8 gear + AP (what "quick start" meant); the roster COUNT is setup.frogs/smurfs, not the
        // stage, so this only sets each seeded agent's level + loadout.
        Config.startStage = if (setup.quickStart) StartStage.END else StartStage.START
        World.grid = grid
        // Size the arena to THIS grid (not the live onboarding default) and make it circular, so a match is a
        // self-contained round play field — deterministic regardless of the onboarding preset, with the play-area
        // circle inscribed in the match grid. A mid-game eval restores the player's real size via WorldSnapshot.
        Sim.setExactSize((grid.keys.maxOf { it.x } + 1).toInt() * Pos.res, (grid.keys.maxOf { it.y } + 1).toInt() * Pos.res)
        Sim.roundField = true
        policyEnl?.let { FactionPolicies.set(Faction.ENL, it) }
        policyRes?.let { FactionPolicies.set(Faction.RES, it) }

        // Clean-eval temporarily silences the anti-runaway knobs; snapshot them so we always restore the live
        // values (even if a match throws) — they have no in-game UI but we still must not leave them zeroed.
        val antiRunaway = Triple(Config.comebackMax, Config.dominanceDecay, Config.leaderDistraction)
        if (setup.cleanEval) {
            Config.comebackMax = 0.0
            Config.dominanceDecay = 0.0
            Config.leaderDistraction = 0.0
        }

        // Pin the player-tunable balance to the shipped defaults (canonical training target); snapshot + restore.
        val liveBalance = Triple(Config.combatDynamism, Config.progressSpeed, Config.portalChurnRate)
        if (setup.useDefaultBalance) {
            Config.combatDynamism = Config.DEFAULT_COMBAT_DYNAMISM
            Config.progressSpeed = Config.DEFAULT_PROGRESS_SPEED
            Config.portalChurnRate = Config.DEFAULT_PORTAL_CHURN
        }

        try {
            seedPortals(setup.portals)
            seedAgents(setup.frogs, setup.smurfs)
            seedNpcs(setup.npcs)
            World.userFaction = Faction.ENL
            World.tick = 0
            World.isReady = true

            repeat(maxTicks) {
                Simulation.stepEntities()
                Cycle.updateCheckpoints(World.tick, World.calcTotalMu(Faction.ENL), World.calcTotalMu(Faction.RES))
                onTick?.invoke(World.tick)
                World.tick++
            }

            val checkpoints = Cycle.INSTANCE.checkpoints.toList().sortedBy { it.first }.map { it.second }
            return MatchResult(seed, maxTicks, checkpoints, World.calcTotalMu(Faction.ENL), World.calcTotalMu(Faction.RES))
        } finally {
            Config.comebackMax = antiRunaway.first
            Config.dominanceDecay = antiRunaway.second
            Config.leaderDistraction = antiRunaway.third
            if (setup.useDefaultBalance) {
                Config.combatDynamism = liveBalance.first
                Config.progressSpeed = liveBalance.second
                Config.portalChurnRate = liveBalance.third
            }
        }
    }

    /** Clear every piece of mutable match state so matches don't bleed into one another. */
    fun reset() {
        World.allAgents.clear()
        World.pendingAgents.clear()
        World.allNonFaction.clear()
        World.allPortals.clear()
        World.userFaction = null
        World.tick = 0
        World.isReady = false
        Cycle.INSTANCE.checkpoints.clear()
        Com.clear()
        XmMap.clear()
        NonFaction.reset()
        StuckTracker.reset()
        NameGen.reset()
        FactionPolicies.reset()
        Fx.reset()
        Snd.reset()
        Nav.reset()
        Config.headlessFieldCompute = false
        Config.startStage = StartStage.START
    }

    private fun seedPortals(count: Int) {
        repeat(count) { World.allPortals.add(Portal.createRandom()) }
    }

    private fun seedAgents(frogs: Int, smurfs: Int) {
        // Portals are seeded first, so Agent.initialActionPortal points each agent at a real world portal.
        repeat(frogs) { World.allAgents.add(Agent.createFrog(World.grid)) }
        repeat(smurfs) { World.allAgents.add(Agent.createSmurf(World.grid)) }
    }

    private fun seedNpcs(count: Int) {
        repeat(count) { World.allNonFaction.add(NonFaction.create(World.grid)) }
    }
}
