package system.display

import kotlin.math.PI

/**
 * Parses the glass-orb fracture GLB into reusable "shard holders". Pure geometry processing (no
 * scene/physics state), split out of [Scene3D] to keep that class under the size limit.
 *
 * A holder is a JS object `{ geo, hx, hy, hz }`: the baked (Z-up) chunk geometry plus its
 * half-extents, used to size a box collider when the shard is spawned.
 */
object ShardAssets {
    /** Group a fractured GLB's meshes into variants by name prefix ("<key>_chunkN"). */
    fun parseVariants(gltf: dynamic): List<List<dynamic>> {
        val groups = mutableMapOf<String, MutableList<dynamic>>()
        gltf.scene.traverse({ obj: dynamic ->
            if (obj.geometry != null) {
                val name = obj.name as String
                val cut = name.indexOf("_chunk")
                val key = if (cut > 0) name.substring(0, cut) else name
                groups.getOrPut(key) { mutableListOf() }.add(makeShard(obj))
            }
        })
        return groups.values.map { it.toList() }
    }

    /** Union bbox of the pieces → uniform scale so they span [target] metres. */
    fun computeScale(pieces: List<dynamic>, target: Double): Double {
        if (pieces.isEmpty()) return 1.0
        var loX = Double.MAX_VALUE
        var loY = Double.MAX_VALUE
        var loZ = Double.MAX_VALUE
        var hiX = -Double.MAX_VALUE
        var hiY = -Double.MAX_VALUE
        var hiZ = -Double.MAX_VALUE
        pieces.forEach { h ->
            val bb = h.geo.boundingBox
            loX = minOf(loX, bb.min.x as Double)
            hiX = maxOf(hiX, bb.max.x as Double)
            loY = minOf(loY, bb.min.y as Double)
            hiY = maxOf(hiY, bb.max.y as Double)
            loZ = minOf(loZ, bb.min.z as Double)
            hiZ = maxOf(hiZ, bb.max.z as Double)
        }
        val d = maxOf(hiX - loX, hiY - loY, hiZ - loZ)
        return if (d > 0.0) target / d else 1.0
    }

    private fun makeShard(mesh: dynamic): dynamic {
        val geo = mesh.geometry
        geo.rotateX(PI / 2) // bake model Y-up → scene Z-up so the rigid-body quaternion drives the mesh
        geo.computeBoundingBox()
        val bb = geo.boundingBox
        val holder: dynamic = js("({})")
        holder.geo = geo
        holder.hx = ((bb.max.x as Double) - (bb.min.x as Double)) / 2.0 // half-extents for the box collider
        holder.hy = ((bb.max.y as Double) - (bb.min.y as Double)) / 2.0
        holder.hz = ((bb.max.z as Double) - (bb.min.z as Double)) / 2.0
        return holder
    }
}
