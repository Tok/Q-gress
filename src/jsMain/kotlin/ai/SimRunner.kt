package ai

import World
import agent.Agent
import agent.Faction
import agent.NonFaction
import agent.StuckTracker
import config.Config
import config.StartStage
import extension.Grid
import portal.Portal
import portal.XmMap
import system.Checkpoint
import system.Com
import system.Cycle
import system.Simulation
import system.effect.Fx
import util.NameGen
import util.Util

/**
 * The outcome of a headless match — the per-checkpoint MU history is the **fitness signal** (PLAN
 * Phase 6.1): each faction maximizes its Mind Units at every checkpoint, so we score the *sum/average*
 * of per-checkpoint MU (sustained large fields), not just the final MU.
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

    /** The faction with the greater summed per-checkpoint MU (the fitness objective), or null on a tie. */
    fun winner(): Faction? {
        val enl = checkpointMuSum(Faction.ENL)
        val res = checkpointMuSum(Faction.RES)
        return when {
            enl > res -> Faction.ENL
            res > enl -> Faction.RES
            else -> null
        }
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
        Util.seed(seed)
        Config.headlessFieldCompute = setup.flowFields // off by default → cheap abstract (straight-line) movement
        // END = full L8 gear + AP (what "quick start" meant); the roster COUNT is setup.frogs/smurfs, not the
        // stage, so this only sets each seeded agent's level + loadout.
        Config.startStage = if (setup.quickStart) StartStage.END else StartStage.START
        World.grid = grid
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
