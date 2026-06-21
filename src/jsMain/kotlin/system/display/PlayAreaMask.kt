package system.display

import external.Three

/**
 * The dark translucent frame that greys out everything beyond the playable rectangle (so the
 * out-of-bounds map reads as inactive). Four quads around the play area — split out of [Scene3D]
 * to keep that class under the size limit.
 */
object PlayAreaMask {
    private var material: dynamic = null

    /** Add the four masking quads to [group], framing the play area of half-extents [hx]×[hy] (scene m). */
    fun build(group: dynamic, hx: Double, hy: Double, far: Double, z: Double, dim: Double) {
        if (material == null) material = buildMaterial(dim)
        val mat = material
        fun quad(cx: Double, cy: Double, w: Double, h: Double) {
            val mesh = Three.Mesh(Three.PlaneGeometry(w, h), mat)
            mesh.asDynamic().position.set(cx, cy, z)
            group.add(mesh)
        }
        // top / bottom span the full width; left / right fill the middle band (no corner double-up).
        quad(0.0, (hy + far) / 2.0, 2.0 * far, far - hy)
        quad(0.0, -(hy + far) / 2.0, 2.0 * far, far - hy)
        quad(-(hx + far) / 2.0, 0.0, far - hx, 2.0 * hy)
        quad((hx + far) / 2.0, 0.0, far - hx, 2.0 * hy)
    }

    private fun buildMaterial(dim: Double): dynamic {
        val p: dynamic = js("({})")
        p.color = "#000000"
        p.transparent = true
        p.opacity = dim
        p.depthWrite = false
        p.side = 2 // DoubleSide
        return Three.MeshBasicMaterial(p)
    }
}
