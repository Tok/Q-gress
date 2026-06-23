package system.display

import portal.Octant
import util.data.Pos

/**
 * The "fly into place" animation for a freshly-deployed resonator: it lerps **from the deploying
 * agent's position, through the air, to its slot** in the portal, growing as it travels. Agents deploy
 * one resonator at a time; each placement [record]s the (portal, octant) + the agent's start position
 * with a wall-clock time. Like [HackFx], game portals are rebuilt every [Scene3D.sync], so we key by
 * portal-id+octant and drive from absolute time (the tween survives the rebuild): [bind] the current
 * rod pivot each sync (Scene3D stamps its start/target on `userData`), [update] each frame.
 */
object DeployFx {
    private const val DUR = 0.6 // seconds for a resonator to fly into its slot
    private const val OUT_FRAC = 0.35 // first portion: rise straight up out of the agent's energy bar
    private const val RISE = 9.0 // how far the rod rises (out of the bar) before peeling off to the slot

    private class Deploy(val start: Double, val from: Pos)

    private val deploys = mutableMapOf<String, Deploy>() // "portal:<id>|<octant>" → start time + agent pos
    private val pivots = mutableMapOf<String, dynamic>() // same key → current rod pivot (re-bound per sync)

    /** Mark a resonator as just deployed at [id]'s [octant], flying in from [from] (the agent's pos). */
    fun record(id: String, octant: Octant, from: Pos) {
        deploys[key(id, octant)] = Deploy(now(), from)
    }

    fun hasActive() = deploys.isNotEmpty()

    /** The agent start position for an in-progress deploy at [id]'s [octant], or null if none/finished. */
    fun fromOf(id: String, octant: Octant): Pos? {
        val k = key(id, octant)
        return if (elapsed(k) != null) deploys[k]?.from else null
    }

    /** Drop the per-sync pivot bindings (call before re-adding portals, like HackFx). */
    fun resetBindings() = pivots.clear()

    /** Bind the freshly-built rod [pivot] for [id]'s [octant] this sync, if it's currently animating. */
    fun bind(id: String, octant: Octant, pivot: dynamic) {
        val k = key(id, octant)
        if (elapsed(k) != null) pivots[k] = pivot
    }

    /** Fly every bound, still-animating rod from the agent to its slot (growing); prune finished. */
    fun update() {
        deploys.keys.filter { elapsed(it) == null }.toList().forEach {
            deploys.remove(it)
            pivots.remove(it)
        }
        pivots.forEach { (k, pivot) -> elapsed(k)?.let { apply(pivot, it) } }
    }

    private fun apply(pivot: dynamic, e: Double) {
        val t = (e / DUR).coerceIn(0.0, 1.0)
        // Start (agent's energy bar) + target (slot) are stamped on userData by Scene3D in the reso frame.
        val sx = pivot.userData.flyStartX as Double
        val sy = pivot.userData.flyStartY as Double
        val sz = pivot.userData.flyStartZ as Double
        val tx = pivot.userData.targetX as Double
        val ty = pivot.userData.targetY as Double
        val tz = pivot.userData.targetZ as Double
        if (t < OUT_FRAC) {
            // Phase 1: the rod rises straight up out of the energy bar (it IS a rod, like the bar).
            val p = t / OUT_FRAC
            pivot.position.set(sx, sy, sz + RISE * p)
            val s = 0.25 + 0.25 * p
            pivot.scale.set(s, s, s)
        } else {
            // Phase 2: peel off from the top of the bar and lerp into the slot, growing to full.
            val p = (t - OUT_FRAC) / (1.0 - OUT_FRAC)
            val ease = 1.0 - (1.0 - p) * (1.0 - p) // easeOutQuad
            val oz = sz + RISE
            pivot.position.set(sx + (tx - sx) * ease, sy + (ty - sy) * ease, oz + (tz - oz) * ease)
            val s = 0.5 + 0.5 * ease
            pivot.scale.set(s, s, s)
        }
    }

    private fun key(id: String, octant: Octant) = "$id|${octant.name}"

    private fun elapsed(k: String): Double? {
        val d = deploys[k] ?: return null
        val e = (now() - d.start) / 1000.0
        return if (e > DUR) null else e
    }

    private fun now() = Scene3D.animMs() // sim-scaled clock so FX track sim speed
}
