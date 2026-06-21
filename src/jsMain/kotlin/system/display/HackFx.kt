package system.display

import external.Three
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The resonator "hack" animation for live game portals: when a portal is hacked its collar spins and
 * the rods centrifuge outward for [SPIN_S] seconds. Game portals are rebuilt every [Scene3D.sync],
 * so we key the animation by portal id and drive it from elapsed wall-clock time (absolute, so the
 * spin survives the rebuild instead of resetting). [record] on each hack, [bind] the current reso
 * group each sync, [update] each frame. [spin] is also reused by the demo's showcase hacks.
 */
object HackFx {
    const val SPIN_S = 2.0 // seconds a hacked collar spins
    private const val SPIN_RATE = 6.0 // rad/s
    private const val TILT_MAX = 0.7 // peak outward rod splay (rad ≈ 40°)

    private val started = mutableMapOf<String, Double>() // portal id → hack start (ms)
    private val groups = mutableMapOf<String, dynamic>() // portal id → current reso group (re-bound per sync)

    /** Mark [id] as hacked now (restarts the spin). */
    fun record(id: String) {
        started[id] = now()
    }

    fun hasActive() = started.isNotEmpty()

    /** Drop the per-sync reso-group bindings (call at the start of each sync, before re-adding portals). */
    fun resetBindings() = groups.clear()

    /** Bind the freshly-built reso group for [id] this sync, if it's currently hacking. */
    fun bind(id: String, resoGroup: dynamic) {
        if (elapsed(id) != null) groups[id] = resoGroup
    }

    /** Spin every bound, still-hacking collar by elapsed time; prune finished hacks. */
    fun update() {
        started.keys.filter { elapsed(it) == null }.forEach {
            started.remove(it)
            groups.remove(it)
        }
        groups.forEach { (id, group) -> elapsed(id)?.let { spin(group, it) } }
    }

    /** Apply the spin + centrifuge to [resoGroup] at [e] seconds into a hack (shared with showcases). */
    fun spin(resoGroup: dynamic, e: Double) {
        resoGroup.rotation.z = e * SPIN_RATE
        val tilt = TILT_MAX * sin((e / SPIN_S) * PI) // 0 → peak → 0 over the duration
        val kids = resoGroup.children
        val n = kids.length as Int
        for (i in 0 until n) {
            val pivot = kids[i]
            if (pivot.userData.isRodPivot == true) {
                val ang = pivot.userData.baseAngle as Double
                // Joint at the rod top → negative angle swings the loose bottom radially outward.
                pivot.setRotationFromAxisAngle(Three.Vector3(-sin(ang), cos(ang), 0.0), -tilt)
            }
        }
    }

    private fun elapsed(id: String): Double? {
        val t = started[id] ?: return null
        val e = (now() - t) / 1000.0
        return if (e > SPIN_S) null else e
    }

    private fun now() = js("performance.now()") as Double
}
