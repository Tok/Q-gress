package system

import World
import agent.StuckTracker
import agent.action.ActionItem
import portal.XmMap
import util.Util

/**
 * The functional-core tick step, shared by the live game (`HtmlUtil.tick`, which wraps this in a
 * `requestAnimationFrame` render + HUD scoring) and the headless match harness (`ai.SimRunner`, which
 * adds synchronous `Cycle` scoring). Advances every agent + NPC one tick and feeds the stuck tracker —
 * no rendering / DOM / audio scheduling. Mutates `World` in place (the imperative shell calls it).
 */
object Simulation {

    /** Advance all agents (on a snapshot so mid-tick recruits buffer safely) then all NPCs, one tick. */
    fun stepEntities() {
        // Iterate a snapshot so mid-tick recruiting can't mutate the set we're looping (recruits are
        // buffered in World.pendingAgents, flushed below). SHUFFLE the order (seeded → deterministic) so
        // neither faction is consistently processed first: in insertion order the ENL roster always acted
        // first and grabbed every neutral portal each tick, shutting RES out — a turn-order bias, not balance.
        val nextAgents = Util.shuffle(World.allAgents.toList()).map { it.act() }.toSet()
        XmMap.updateStrayXm()

        World.allAgents.clear()
        World.allAgents.addAll(nextAgents)
        World.flushPendingAgents()

        World.allNonFaction.forEach { it.act() }
        sampleStuck() // always on: drives stuck-recovery (Agent.recoverIfStuck / NonFaction); also the ?debug viz
    }

    // Feed StuckTracker only the entities actively trying to travel this tick (powers recovery + ?debug viz).
    private fun sampleStuck() {
        val agents = World.allAgents.filter { it.action.item == ActionItem.MOVE }.map { it.key() to it.pos }
        val npcs = World.allNonFaction.filter { it.isStuckCandidate(World.tick) }.map { "npc:${it.id}" to it.pos }
        StuckTracker.sample(agents + npcs)
    }
}
