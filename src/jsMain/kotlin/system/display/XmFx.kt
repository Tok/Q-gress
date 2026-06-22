package system.display

import external.Three

/**
 * Stray-XM pickup: when an agent collects an XM heap, a glowing mote **lerps from the heap to the
 * agent** (accelerating + shrinking) before vanishing, instead of just blinking out. Kinematic (no
 * physics); driven by absolute time so it's independent of the sim tick. [register] once,
 * [spawn] on pickup, [update] each frame while [hasActive].
 */
object XmFx {
    private const val DUR = 0.4 // seconds for a mote to reach the agent
    private const val XM_R = 0.7

    private var group: dynamic = null
    private val geo: dynamic by lazy { Three.SphereGeometry(XM_R, 8, 8) }

    private class Mote(val mesh: dynamic, val from: DoubleArray, val to: DoubleArray, val start: Double)

    private val motes = mutableListOf<Mote>()

    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
    }

    fun hasActive() = motes.isNotEmpty()

    /** Fly a mote from [from] (the heap, scene metres) to [to] (the agent). */
    fun spawn(from: DoubleArray, to: DoubleArray) {
        val g = group ?: return
        val mesh = Three.Mesh(geo, Materials.xmGlow())
        mesh.asDynamic().position.set(from[0], from[1], from[2])
        g.add(mesh)
        motes.add(Mote(mesh, from, to, now()))
    }

    fun update() {
        val iter = motes.iterator()
        while (iter.hasNext()) {
            val m = iter.next()
            val t = ((now() - m.start) / 1000.0 / DUR).coerceIn(0.0, 1.0)
            val ease = t * t // accelerate toward the agent
            m.mesh.position.set(
                m.from[0] + (m.to[0] - m.from[0]) * ease,
                m.from[1] + (m.to[1] - m.from[1]) * ease,
                m.from[2] + (m.to[2] - m.from[2]) * ease,
            )
            val s = 1.0 - t // shrink as it's absorbed
            m.mesh.scale.set(s, s, s)
            if (t >= 1.0) {
                group.remove(m.mesh)
                iter.remove()
            }
        }
    }

    private fun now() = js("performance.now()") as Double
}
