package agent

import config.Dim
import util.data.Pos

/**
 * Flags agents/NPCs that aren't making progress — frozen against geometry, or looping/wandering in a
 * small region (the vector-field spiral / off-screen-detour symptom). **Detection here**; the recovery
 * (a temporary bee-line, then a re-target) lives in [Agent.recoverIfStuck] + [NonFaction]. Fed every
 * tick (also powers the `?debug` 3D marker + HUD count). Keyed by a stable id, and only fed entities
 * *currently trying to travel*, so legitimately waiting / at-destination entities aren't flagged.
 *
 * An entity is "stuck" when its net displacement over a full window of samples stays under one
 * deployment range — covers both frozen-in-place and back-and-forth looping (start ≈ end).
 */
object StuckTracker {
    private const val WINDOW = 100 // position samples kept per entity (one per tick)
    private const val MIN_SAMPLES = 60 // judge after this many samples — short so recovery kicks in fast, not after ~2 min
    private val stuckRadius get() = Dim.maxDeploymentRange // net move under this over the window = stuck

    /** When flagged stuck, agents/NPCs bee-line straight at their target for this many ticks before re-targeting. */
    const val RECOVERY_BEELINE_TICKS = 60

    private val history = mutableMapOf<String, ArrayDeque<Pos>>()
    private var stuck: Set<String> = emptySet()

    fun reset() {
        history.clear()
        stuck = emptySet()
    }

    /** Record positions of currently-moving entities ([moving] = stableKey→pos) and recompute the stuck set. */
    fun sample(moving: List<Pair<String, Pos>>) {
        val live = moving.mapTo(HashSet()) { it.first }
        history.keys.retainAll(live) // forget entities that stopped moving / departed (resets their window)
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
