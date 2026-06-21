package system.display

import external.Three

/**
 * Transient "dissolve" effect for control fields that have just been destroyed. [Scene3D] detects a
 * field's removal (via the [Spawns] registry) and calls [dissolve] with the field's last-known
 * shape; the plasma triangle then shrinks away over a fraction of a second. Owns its own group +
 * active list (not cleared by the per-tick sync), driven by [Scene3D]'s render loop.
 */
object FieldFx {
    private const val LIFE = 0.45 // seconds to collapse

    private var group: dynamic = null
    private val active = mutableListOf<Dissolve>()

    private class Dissolve(val mesh: dynamic, var age: Double)

    fun register(scene: dynamic) {
        group = Three.Group()
        scene.add(group)
        active.clear()
    }

    fun hasActive() = active.isNotEmpty()

    /** Spawn a collapsing copy of a removed field: [rel] = the 3 centroid-relative vertices. */
    fun dissolve(cx: Double, cy: Double, cz: Double, rel: Array<DoubleArray>, color: String) {
        val grp = group ?: return
        val pts = rel.map { Three.Vector3(it[0], it[1], it[2]) }.toTypedArray()
        val mesh = Three.Mesh(Three.BufferGeometry().setFromPoints(pts), PlasmaShader.material(color))
        mesh.asDynamic().position.set(cx, cy, cz)
        grp.add(mesh)
        active.add(Dissolve(mesh, 0.0))
    }

    fun update(dt: Double) {
        val grp = group ?: return
        val iter = active.iterator()
        while (iter.hasNext()) {
            val d = iter.next()
            d.age += dt
            val s = (1.0 - d.age / LIFE).coerceAtLeast(0.0) // shrink to nothing
            d.mesh.scale.set(s, s, s)
            if (d.age >= LIFE) {
                grp.remove(d.mesh)
                d.mesh.geometry.dispose()
                iter.remove()
            }
        }
    }
}
