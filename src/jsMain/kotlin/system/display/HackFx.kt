package system.display

import agent.Faction
import external.Three
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The resonator "hack" animation for live game portals: when a portal is hacked its collar spins and
 * the rods centrifuge outward. Direction is per **faction** — ENL clockwise, RES counter-clockwise —
 * and **glyph** hacks are stronger (faster, wider splay, longer) than normal hacks. Game portals are
 * rebuilt every [Scene3D.sync], so we key the animation by portal id and drive it from elapsed
 * wall-clock time (absolute, so the spin survives the rebuild). [record] on each hack, [bind] the
 * current reso group each sync, [update] each frame. [spin] is also reused by the demo's showcases.
 */
object HackFx {
    const val SPIN_S = 2.0 // seconds a normal hacked collar spins
    const val GLYPH_SPIN_S = 3.2 // glyph hacking is stronger → spins longer
    private const val SPIN_RATE = 6.0 // rad/s
    private const val TILT_MAX = 0.7 // peak outward rod splay (rad ≈ 40°)
    private const val GLYPH_RATE_MUL = 1.5 // glyph spins faster
    private const val GLYPH_TILT_MUL = 1.35 // ...and splays wider

    // three.js +Z rotation is CCW (top-down) → ENL clockwise = −1, RES counter-clockwise = +1.
    private fun directionFor(faction: Faction) = if (faction == Faction.ENL) -1.0 else 1.0

    private class Hack(val start: Double, val dir: Double, val rateMul: Double, val tiltMul: Double, val dur: Double)

    private val hacks = mutableMapOf<String, Hack>() // portal id → active hack
    private val groups = mutableMapOf<String, dynamic>() // portal id → current reso group (re-bound per sync)

    /** Mark [id] as hacked now (restarts the spin). [glyph] hacks read as stronger. */
    fun record(id: String, faction: Faction, glyph: Boolean) {
        hacks[id] = Hack(
            now(),
            directionFor(faction),
            if (glyph) GLYPH_RATE_MUL else 1.0,
            if (glyph) GLYPH_TILT_MUL else 1.0,
            if (glyph) GLYPH_SPIN_S else SPIN_S,
        )
    }

    fun hasActive() = hacks.isNotEmpty()

    /** Drop the per-sync reso-group bindings (call at the start of each sync, before re-adding portals). */
    fun resetBindings() = groups.clear()

    /** Bind the freshly-built reso group for [id] this sync, if it's currently hacking. */
    fun bind(id: String, resoGroup: dynamic) {
        if (elapsed(id) != null) groups[id] = resoGroup
    }

    /** Spin every bound, still-hacking collar by elapsed time; prune finished hacks. */
    fun update() {
        hacks.keys.filter { elapsed(it) == null }.toList().forEach {
            hacks.remove(it)
            groups.remove(it)
        }
        groups.forEach { (id, group) ->
            val h = hacks[id] ?: return@forEach
            elapsed(id)?.let { spin(group, it, h.dir, h.rateMul, h.tiltMul, h.dur) }
        }
    }

    /** Apply the spin + centrifuge to [resoGroup] at [e] seconds in. The demo showcases call it with
     *  defaults (apart from [dir]); the game passes the per-hack direction + glyph intensity. */
    fun spin(resoGroup: dynamic, e: Double, dir: Double = 1.0, rateMul: Double = 1.0, tiltMul: Double = 1.0, dur: Double = SPIN_S) {
        resoGroup.rotation.z = e * SPIN_RATE * rateMul * dir
        val tilt = TILT_MAX * tiltMul * sin((e / dur) * PI) // 0 → peak → 0 over the duration
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

    /** Demo helper: spin a showcase collar at [elapsed] seconds with the [glyph] intensity + [dir]. */
    fun spinShowcase(resoGroup: dynamic, elapsed: Double, dir: Double, glyph: Boolean) {
        if (glyph) {
            spin(resoGroup, elapsed, dir, GLYPH_RATE_MUL, GLYPH_TILT_MUL, GLYPH_SPIN_S)
        } else {
            spin(resoGroup, elapsed, dir)
        }
    }

    private fun elapsed(id: String): Double? {
        val h = hacks[id] ?: return null
        val e = (now() - h.start) / 1000.0
        return if (e > h.dur) null else e
    }

    private fun now() = js("performance.now()") as Double
}
