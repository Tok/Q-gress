package agent

import config.Dim
import util.data.Pos

/**
 * Flags agents/NPCs that aren't making progress — frozen against geometry, or looping/wandering in a
 * small region (the off-screen-detour symptom). **Detection here**; the recovery lives in
 * [Agent.recoverIfStuck] + [NonFaction]. Fed every tick (also powers the `?debug` 3D marker + HUD count).
 * Keyed by a stable id, and only fed entities *currently trying to travel* ([Agent.isTravelling]), so
 * legitimately waiting / at-destination entities aren't flagged.
 *
 * An entity is "stuck" when its net displacement over a full window of samples stays under one
 * deployment range — covers both frozen-in-place and back-and-forth looping (start ≈ end).
 *
 * A recovery MUST [clear] its entity: the whole window predates the escape, so leaving it in place re-flags
 * the entity on the next check and recovery fires again and again on stale evidence.
 */
object StuckTracker {
    // Windows halved now that walk speed doubled: a genuinely-travelling entity clears [stuckRadius] in ~half the
    // ticks, so the same net-displacement test catches loopers ~2× sooner (the border-circling symptom).
    private const val WINDOW = 50 // position samples kept per entity (one per tick)
    private const val MIN_SAMPLES = 30 // judge after this many samples — short so recovery kicks in fast
    private const val ABSENCE_GRACE = 3 // ticks an entity may drop out of the moving set before its window resets
    private val stuckRadius get() = Dim.maxDeploymentRange // net move under this over the window = stuck

    private val history = mutableMapOf<String, ArrayDeque<Pos>>()
    private val absence = mutableMapOf<String, Int>() // consecutive ticks an entity has been out of the moving set
    private var stuck: Set<String> = emptySet()

    fun reset() {
        history.clear()
        absence.clear()
        stuck = emptySet()
    }

    /** Drop [key]'s window and un-flag it — called the moment a recovery fires, so the next check judges the
     *  entity on where it goes NEXT rather than re-firing for another [WINDOW] ticks on pre-recovery samples. */
    fun clear(key: String) {
        history.remove(key)
        absence.remove(key)
        if (key in stuck) stuck = stuck - key
    }

    /** Record positions of currently-moving entities ([moving] = stableKey→pos) and recompute the stuck set. */
    fun sample(moving: List<Pair<String, Pos>>) {
        val live = moving.mapTo(HashSet()) { it.first }
        // Forget entities that genuinely stopped travelling — but only after [ABSENCE_GRACE] ticks of absence, so a
        // brief flip out of MOVE (the 1-tick WAIT between two travels) doesn't reset the window. Otherwise an agent
        // oscillating MOVE↔WAIT in place would clear its history every other tick and never be flagged stuck.
        history.keys.toList().forEach { key ->
            if (key in live) {
                absence.remove(key)
            } else if ((absence[key] ?: 0) + 1 > ABSENCE_GRACE) {
                history.remove(key)
                absence.remove(key)
            } else {
                absence[key] = (absence[key] ?: 0) + 1
            }
        }
        val nowStuck = HashSet<String>()
        moving.forEach { (key, pos) ->
            val h = history.getOrPut(key) { ArrayDeque() }
            h.addLast(pos)
            while (h.size > WINDOW) h.removeFirst()
            if (h.size >= MIN_SAMPLES && h.first().distanceTo(h.last()) < stuckRadius) nowStuck.add(key)
        }
        stuck = nowStuck
    }

    fun isStuck(key: String) = key in stuck
    fun count() = stuck.size
}
