package system.display

import external.Three
import kotlin.math.PI

/**
 * The dark translucent frame that greys out everything beyond the playable area (so the out-of-bounds
 * map reads as inactive) + the upright boundary walls. Rectangular by default; a [buildRound] variant
 * draws an inscribed-ellipse arena (an annulus mask + an open elliptical wall). Split out of [Scene3D].
 */
object PlayAreaMask {
    private var material: dynamic = null
    private var wallMat: dynamic = null

    /**
     * Add four upright, semi-transparent white walls along the play-area edges (so the boundary reads
     * as a physical arena, not just a ground line). Thin boxes: no rotation, dimensions are explicit.
     */
    fun buildWalls(group: dynamic, hx: Double, hy: Double, height: Double, thickness: Double, z0: Double) {
        if (wallMat == null) wallMat = buildWallMaterial()
        val mat = wallMat
        fun wall(w: Double, d: Double, cx: Double, cy: Double) {
            val mesh = Three.Mesh(Three.BoxGeometry(w, d, height), mat)
            mesh.asDynamic().position.set(cx, cy, z0 + height / 2.0)
            group.add(mesh)
        }
        wall(2.0 * hx + thickness, thickness, 0.0, hy) // north
        wall(2.0 * hx + thickness, thickness, 0.0, -hy) // south
        wall(thickness, 2.0 * hy, hx, 0.0) // east
        wall(thickness, 2.0 * hy, -hx, 0.0) // west
    }

    private fun buildWallMaterial(): dynamic {
        val p: dynamic = js("({})")
        p.color = "#ffffff"
        p.transparent = true
        p.opacity = 0.1 // faint glass pane — visible boundary without blocking the view
        p.depthWrite = false
        p.side = 2 // DoubleSide
        return Three.MeshBasicMaterial(p)
    }

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

    /**
     * Round arena: a dark annulus (a big square with an elliptical hole) dims everything outside the
     * inscribed ellipse [hx]×[hy], and an open elliptical wall stands at its edge.
     */
    private const val MASK_FAR = 12.0 // how far past the play area the dim mask extends (× the half-extent)

    fun buildRound(group: dynamic, hx: Double, hy: Double, z: Double, dim: Double, height: Double) {
        if (material == null) material = buildMaterial(dim)
        val far = MASK_FAR * maxOf(hx, hy)
        val shape = Three.Shape()
        shape.asDynamic().moveTo(-far, -far)
        shape.asDynamic().lineTo(far, -far)
        shape.asDynamic().lineTo(far, far)
        shape.asDynamic().lineTo(-far, far)
        shape.asDynamic().lineTo(-far, -far)
        val hole = Three.Path()
        hole.asDynamic().absellipse(0.0, 0.0, hx, hy, 0.0, 2.0 * PI)
        shape.asDynamic().holes.push(hole)
        val mask = Three.Mesh(Three.ShapeGeometry(shape), material)
        mask.asDynamic().position.set(0.0, 0.0, z)
        group.add(mask)
        buildRoundWall(group, hx, hy, height)
    }

    private fun buildRoundWall(group: dynamic, hx: Double, hy: Double, height: Double) {
        if (wallMat == null) wallMat = buildWallMaterial()
        val cyl = Three.CylinderGeometry(1.0, 1.0, height, 64, 1, true) // unit open tube (axis = local Y)
        val mesh = Three.Mesh(cyl, wallMat)
        mesh.asDynamic().scale.set(hx, 1.0, hy) // local X/Z → ellipse radii; Y stays the wall height
        mesh.asDynamic().rotation.x = PI / 2 // stand the tube up (Y → world Z)
        mesh.asDynamic().position.set(0.0, 0.0, height / 2.0)
        group.add(mesh)
    }
}
