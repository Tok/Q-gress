package system.display

import portal.Octant

/**
 * The "drop into place" animation for a freshly-deployed resonator. Agents deploy one resonator at a
 * time; each placement [record]s a (portal, octant) with a wall-clock start, and the just-deployed rod
 * **lerps** into its slot — growing from nearly nothing while sliding radially out from the pole to its
 * grommet. Like [HackFx], game portals are rebuilt every [Scene3D.sync], so we key by portal-id+octant
 * and drive from absolute time (the tween survives the rebuild); [bind] the current pivot each sync,
 * [update] each frame.
 */
object DeployFx {
    private const val DUR = 0.45 // seconds for a reso to settle into its slot

    private class Deploy(val start: Double)

    private val deploys = mutableMapOf<String, Deploy>() // "portal:<id>|<octant>" → start
    private val pivots = mutableMapOf<String, dynamic>() // same key → current rod pivot (re-bound per sync)

    /** Mark a resonator as just deployed at [id]'s [octant] (starts the drop-in tween). */
    fun record(id: String, octant: Octant) {
        deploys[key(id, octant)] = Deploy(now())
    }

    fun hasActive() = deploys.isNotEmpty()

    /** Drop the per-sync pivot bindings (call before re-adding portals, like HackFx). */
    fun resetBindings() = pivots.clear()

    /** Bind the freshly-built rod [pivot] for [id]'s [octant] this sync, if it's currently animating. */
    fun bind(id: String, octant: Octant, pivot: dynamic) {
        val k = key(id, octant)
        if (elapsed(k) != null) pivots[k] = pivot
    }

    /** Lerp every bound, still-animating rod into place; prune finished deploys. */
    fun update() {
        deploys.keys.filter { elapsed(it) == null }.toList().forEach {
            deploys.remove(it)
            pivots.remove(it)
        }
        pivots.forEach { (k, pivot) -> elapsed(k)?.let { apply(pivot, it) } }
    }

    private fun apply(pivot: dynamic, e: Double) {
        val t = (e / DUR).coerceIn(0.0, 1.0)
        val ease = 1.0 - (1.0 - t) * (1.0 - t) // easeOutQuad
        val s = 0.12 + 0.88 * ease
        pivot.scale.set(s, s, s)
        // Slide from the pole axis (0,0) out to the rod's resting slot position (stamped in buildResonators).
        pivot.position.x = (pivot.userData.targetX as Double) * ease
        pivot.position.y = (pivot.userData.targetY as Double) * ease
    }

    private fun key(id: String, octant: Octant) = "$id|${octant.name}"

    private fun elapsed(k: String): Double? {
        val d = deploys[k] ?: return null
        val e = (now() - d.start) / 1000.0
        return if (e > DUR) null else e
    }

    private fun now() = js("performance.now()") as Double
}
