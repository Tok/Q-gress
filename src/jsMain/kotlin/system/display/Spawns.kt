package system.display

/**
 * A tiny persistent per-entity animation registry that survives the per-tick [Scene3D.sync]
 * rebuild (which otherwise has no memory of when an entity first appeared). Keyed by stable id, it
 * records each entity's first-seen time and returns an **appearance factor** so synced meshes can
 * grow/fade in over their first moments.
 *
 * Usage each sync: [beginSync], then [appear] per entity while building it, then [endSync] (which
 * forgets ids that vanished — so if the same id returns later it animates again, and removed
 * entities are reported for teardown effects).
 */
object Spawns {
    private val firstSeen = mutableMapOf<String, Double>()
    private val present = mutableSetOf<String>()

    fun beginSync() {
        present.clear()
    }

    /** Eased 0→1 "grown in" factor for [id] over [durationS] seconds since first seen (with overshoot). */
    fun appear(id: String, durationS: Double): Double = easeOutBack(appearRaw(id, durationS))

    /** Raw (un-eased) linear 0→1 progress for [id] over [durationS]s — for custom curves (e.g. a fall). */
    fun appearRaw(id: String, durationS: Double): Double {
        present.add(id)
        val now = nowMs()
        val t = firstSeen.getOrPut(id) { now }
        return (((now - t) / 1000.0) / durationS).coerceIn(0.0, 1.0)
    }

    /** Forget ids not seen this sync; returns the ids that were present last sync but vanished. */
    fun endSync(): Set<String> {
        val gone = firstSeen.keys.filter { it !in present }.toSet()
        firstSeen.keys.retainAll(present)
        return gone
    }

    // easeOutBack — overshoots slightly past 1 then settles, for a little "pop".
    private fun easeOutBack(f: Double): Double {
        val c1 = 1.70158
        val c3 = c1 + 1.0
        val x = f - 1.0
        return 1.0 + c3 * x * x * x + c1 * x * x
    }

    private fun nowMs() = js("performance.now()") as Double
}
