package system.display.fx

import external.Three
import items.RewardMote
import items.RewardShape
import system.display.Materials
import system.display.Scene3D
import kotlin.math.PI
import kotlin.math.sin

/**
 * Hack/glyph loot: each dropped item flies from the portal orb to the hacking agent as a small **item-coloured
 * cube** (gold = portal key, level-colour = power cube / resonator, rarity = mod, faction colour otherwise) —
 * **except viruses**, which fly as a bigger **faction-coloured sphere** ([SPHERE_R], φ× an agent head). Each
 * arcs up and spins before it's absorbed. Kinematic (no physics), absolute-time driven like [XmFx];
 * [register] once, [spawn] per item (staggered), [update] each frame while [hasActive].
 */
object RewardFx {
    private const val DUR = 0.6 // seconds for a mote to reach the agent
    private const val CUBE_R = 0.9
    private const val SPHERE_R = 0.45 * 1.618 // virus sphere: 1.618× (φ) an agent head (Scene3D.HEAD_R = 0.45)
    private const val ARC_H = 4.0 // how high the mote humps on its way over

    private var group: dynamic = null
    private val cubeGeo: dynamic by lazy { Three.BoxGeometry(CUBE_R, CUBE_R, CUBE_R) }
    private val sphereGeo: dynamic by lazy { Three.SphereGeometry(SPHERE_R, 16, 12) }

    private class Cube(val mesh: dynamic, val from: DoubleArray, val to: DoubleArray, val start: Double, val spin: Double)

    private val cubes = mutableListOf<Cube>()

    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
    }

    fun hasActive() = cubes.isNotEmpty()

    /** Fly a [mote] (cube, or a sphere for viruses) from [from] (the orb, scene metres) to [to] (the agent), beginning [delayS] later. */
    fun spawn(from: DoubleArray, to: DoubleArray, mote: RewardMote, delayS: Double) {
        val g = group ?: return
        val geo = if (mote.shape == RewardShape.SPHERE) sphereGeo else cubeGeo
        val mesh = Three.Mesh(geo, Materials.modSolid(mote.color))
        mesh.asDynamic().position.set(from[0], from[1], from[2])
        g.add(mesh)
        cubes.add(Cube(mesh, from, to, now() + delayS * 1000.0, 4.0 + delayS * 6.0))
    }

    fun update() {
        val iter = cubes.iterator()
        while (iter.hasNext()) {
            val c = iter.next()
            val t = ((now() - c.start) / 1000.0 / DUR).coerceIn(0.0, 1.0)
            val ease = t * t // accelerate toward the agent
            val hump = sin(t * PI) * ARC_H // up-and-over on the way
            c.mesh.position.set(
                c.from[0] + (c.to[0] - c.from[0]) * ease,
                c.from[1] + (c.to[1] - c.from[1]) * ease,
                c.from[2] + (c.to[2] - c.from[2]) * ease + hump,
            )
            val turn = c.spin * t
            c.mesh.rotation.set(turn, turn * 0.7, 0.0)
            val s = 1.0 - t * 0.6 // shrink a little as it's absorbed
            c.mesh.scale.set(s, s, s)
            if (t >= 1.0) {
                group.remove(c.mesh)
                iter.remove()
            }
        }
    }

    private fun now() = Scene3D.animMs() // sim-scaled clock so FX track sim speed
}
