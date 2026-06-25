package system

import World
import agent.Agent
import agent.Faction
import agent.NonFaction
import ai.FactionPolicies
import ai.FactionPolicy
import config.Config
import extension.Grid
import portal.Portal

/**
 * Captures + restores the mutable simulation state a headless eval would clobber (PLAN Phase 6.4), so a
 * **tournament/benchmark can run mid-session and the live game resumes untouched**. [ai.SimRunner.runMatch]
 * shares the [World] singletons — it clears `allPortals`/`allAgents`/… and installs its own throwaway
 * portals/agents — so without this a benchmark would wipe the player's game. [capture] saves references to
 * the live objects (the eval makes its *own* new ones, never mutating these) + the scalar/registry state;
 * [restore] puts them all back.
 *
 * Transient bookkeeping the eval also resets (stray XM, NPC swarm counters, stuck timers, name pool) is left
 * regenerated — it's cosmetic and refills within a checkpoint; the *visible* world (portals, agents, links,
 * fields, NPCs, the installed AI drivers, the score history) is what's preserved.
 */
object WorldSnapshot {

    @Suppress("LongParameterList") // a pure state-snapshot DTO — every captured field is independent
    class Snapshot(
        val portals: List<Portal>,
        val agents: List<Agent>,
        val pending: List<Agent>,
        val nonFaction: List<NonFaction>,
        val userFaction: Faction?,
        val tick: Int,
        val isReady: Boolean,
        val grid: Grid,
        val checkpoints: Map<Int, Checkpoint>,
        val policies: Map<Faction, FactionPolicy>,
        val headlessFieldCompute: Boolean,
        val quickStart: Boolean,
    )

    /** Snapshot the live world. Requires [World.grid] to be initialised (a running game always has it). */
    fun capture(): Snapshot = Snapshot(
        portals = World.allPortals.toList(),
        agents = World.allAgents.toList(),
        pending = World.pendingAgents.toList(),
        nonFaction = World.allNonFaction.toList(),
        userFaction = World.userFaction,
        tick = World.tick,
        isReady = World.isReady,
        grid = World.grid,
        checkpoints = Cycle.INSTANCE.checkpoints.toMap(),
        policies = Faction.all().associateWith { FactionPolicies.of(it) },
        headlessFieldCompute = Config.headlessFieldCompute,
        quickStart = Config.quickStart,
    )

    /** Restore a [capture]d world (after an eval reset/ran on the shared singletons). */
    fun restore(snapshot: Snapshot) {
        replace(World.allPortals, snapshot.portals)
        replace(World.allAgents, snapshot.agents)
        replace(World.pendingAgents, snapshot.pending)
        replace(World.allNonFaction, snapshot.nonFaction)
        World.userFaction = snapshot.userFaction
        World.tick = snapshot.tick
        World.isReady = snapshot.isReady
        World.grid = snapshot.grid
        Cycle.INSTANCE.checkpoints.clear()
        Cycle.INSTANCE.checkpoints.putAll(snapshot.checkpoints)
        FactionPolicies.reset()
        snapshot.policies.forEach { (faction, policy) -> FactionPolicies.set(faction, policy) }
        Config.headlessFieldCompute = snapshot.headlessFieldCompute
        Config.quickStart = snapshot.quickStart
    }

    private fun <T> replace(target: MutableCollection<T>, source: List<T>) {
        target.clear()
        target.addAll(source)
    }
}
