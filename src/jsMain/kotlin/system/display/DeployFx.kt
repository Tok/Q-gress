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
        val ease = 1.0 - (1.0 - t) * (1.0 - t) // easeOutQuad
        // Start (agent pos) + target (slot) are stamped on userData by Scene3D in the reso group's frame.
        val sx = pivot.userData.flyStartX as Double
        val sy = pivot.userData.flyStartY as Double
        val sz = pivot.userData.flyStartZ as Double
        val tx = pivot.userData.targetX as Double
        val ty = pivot.userData.targetY as Double
        val tz = pivot.userData.targetZ as Double
        pivot.position.set(sx + (tx - sx) * ease, sy + (ty - sy) * ease, sz + (tz - sz) * ease)
        val s = 0.2 + 0.8 * ease // grows from small to full as it arrives
        pivot.scale.set(s, s, s)
    }

    private fun key(id: String, octant: Octant) = "$id|${octant.name}"

    private fun elapsed(k: String): Double? {
        val d = deploys[k] ?: return null
        val e = (now() - d.start) / 1000.0
        return if (e > DUR) null else e
    }

    private fun now() = Scene3D.animMs() // sim-scaled clock so FX track sim speed
}
