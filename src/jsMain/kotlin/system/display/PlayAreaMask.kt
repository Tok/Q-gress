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
    private var ringMat: dynamic = null

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
        // Walls sit JUST OUTSIDE the play boundary (inner face at ±hx/±hy), so the playable rect is clear of
        // the wall and the boundary line marks its inner edge. Long walls overhang by a thickness to meet corners.
        val out = thickness / 2.0
        wall(2.0 * hx + 2.0 * thickness, thickness, 0.0, hy + out) // north
        wall(2.0 * hx + 2.0 * thickness, thickness, 0.0, -(hy + out)) // south
        wall(thickness, 2.0 * hy, hx + out, 0.0) // east
        wall(thickness, 2.0 * hy, -(hx + out), 0.0) // west
    }

    private fun buildWallMaterial(): dynamic {
        val p: dynamic = js("({})")
        p.color = "#ffffff"
        p.transparent = true
        p.opacity = 0.2 // a touch more solid so the thicker 3D wall reads as a boundary, not a film
        p.depthWrite = false
        // DoubleSide: cull-backface (FrontSide) left each wall showing only the faces pointing at the camera,
        // so the far side of every box / the inside of the ring just vanished — the boundary read as a thin,
        // inside-out film. Rendering both faces makes it read as a real translucent wall from any angle (the
        // slight extra blend where you look through both layers is what a glass wall actually does).
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
        // depthTest ON: the flat mask reads as a sea-level disc through the terrain if forced on top,
        // so let the terrain occlude it (it dims the low/flat out-of-bounds; the wall marks the edge).
        p.side = 2 // DoubleSide
        return Three.MeshBasicMaterial(p)
    }

    private const val MASK_FAR = 12.0 // how far past the play area the dim mask extends (× the half-extent)

    /** Round arena dim mask: a dark annulus (a big square with a circular hole of radius [hx]×[hy]). */
    fun buildRoundMask(group: dynamic, hx: Double, hy: Double, z: Double, dim: Double) {
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
    }

    /** A solid annular wall (real thickness, extruded vertically) standing from [base] up by [height]. The wall
     *  sits JUST OUTSIDE [hx]×[hy]: its INNER face is the play radius and it extends a thickness outward. */
    fun buildRoundWall(group: dynamic, hx: Double, hy: Double, base: Double, height: Double) {
        if (wallMat == null) wallMat = buildWallMaterial()
        val thick = minOf(hx, hy) * WALL_THICK_FRAC
        val shape = Three.Shape()
        shape.asDynamic().absellipse(0.0, 0.0, hx + thick, hy + thick, 0.0, 2.0 * PI) // outer wall (boundary + thickness)
        val hole = Three.Path()
        hole.asDynamic().absellipse(0.0, 0.0, hx, hy, 0.0, 2.0 * PI) // inner face = the play radius
        shape.asDynamic().holes.push(hole)
        val opts: dynamic = js("({ bevelEnabled: false })")
        opts.depth = height
        opts.curveSegments = 64
        val mesh = Three.Mesh(Three.ExtrudeGeometry(shape, opts), wallMat)
        mesh.asDynamic().position.set(0.0, 0.0, base) // extrudes +Z from the terrain base
        group.add(mesh)
        // Explicit top + bottom rings close the inner/outer bands into a solid 3D object (the extrude's
        // own caps can be backface-culled at our viewing angle, leaving the wall looking like two bands).
        if (ringMat == null) ringMat = buildRingMaterial()
        val r = minOf(hx, hy)
        listOf(base, base + height).forEach { z ->
            val ring = Three.Mesh(Three.RingGeometry(r, r + thick, 64), ringMat)
            ring.asDynamic().scale.set(hx / r, hy / r, 1.0) // back to the ellipse aspect (1,1 for a circle)
            ring.asDynamic().position.set(0.0, 0.0, z)
            group.add(ring)
        }
    }

    private fun buildRingMaterial(): dynamic {
        val p: dynamic = js("({})")
        p.color = "#ffffff"
        p.transparent = true
        p.opacity = 0.3 // the rim rings read a touch stronger than the wall bands
        p.depthWrite = false
        p.side = 2 // DoubleSide — the rim is seen from above and (rarely) below
        return Three.MeshBasicMaterial(p)
    }

    const val WALL_THICK_FRAC = 0.03 // round-wall thickness as a fraction of the radius
}
