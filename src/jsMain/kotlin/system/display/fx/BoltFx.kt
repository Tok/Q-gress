package system.display.fx

import external.Three
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set
import system.display.add
import system.display.cross
import system.display.dist
import system.display.lenSq
import system.display.lerp
import system.display.lerp1
import system.display.norm
import system.display.rotate
import system.display.scale
import system.display.shader.GlassShader
import system.display.sub
import util.Util

/**
 * Tesla-coil **bolt** VFX: a fractal lightning ribbon (camera-facing, additive) + a flash point light
 * at the strike end, fading over [LIFE]. Ported from the title/qlippostasis lightning into the game
 * scene so portals can fire **retaliation bolts** at attackers (see `Portal.retaliate`). [register]
 * once (in `Scene3D.onAdd`), [fire] per bolt, [update] each frame while [hasActive].
 */
object BoltFx {
    private const val SEGS = 7 // segments per bolt span
    private const val JITTER = 0.16 // lateral jitter as a fraction of the span length
    private const val BRANCH_DEPTH = 2
    private const val WIDTH = 1.1 // ribbon half-width at the source (scene metres)
    private const val END_WIDTH_FRAC = 0.18
    private const val LIFE = 0.3 // seconds the bolt + flash live
    private const val FLASH_RANGE = 40.0
    private const val FLASH_ENERGY = 14.0

    private var scene: dynamic = null
    private val bolts = mutableListOf<Bolt>()

    private class Bolt(val container: dynamic, val mat: dynamic, val flash: dynamic, var age: Double)

    fun register(s: Three.Scene) {
        scene = s
    }

    fun hasActive() = bolts.isNotEmpty()

    /** Fire a bolt from [start] to [end] (scene metres), coloured [color]. */
    fun fire(start: DoubleArray, end: DoubleArray, color: String) {
        val s = scene ?: return
        val verts = ArrayList<Double>()
        buildFractal(verts, start, end, WIDTH, WIDTH * END_WIDTH_FRAC, BRANCH_DEPTH)
        val arr = Float32Array(verts.size)
        for (i in verts.indices) arr[i] = verts[i].toFloat()
        val geo = Three.BufferGeometry()
        geo.asDynamic().setAttribute("position", Three.Float32BufferAttribute(arr, 3))
        val mat = additive(color)
        val container = Three.Group()
        container.asDynamic().add(Three.Mesh(geo, mat))
        val flash: dynamic = Three.PointLight(color, FLASH_ENERGY, FLASH_RANGE)
        flash.position.set(end[0], end[1], end[2])
        container.asDynamic().add(flash)
        s.add(container)
        bolts.add(Bolt(container, mat, flash, 0.0))
    }

    fun update(dt: Double) {
        val it = bolts.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.age += dt
            val f = (1.0 - b.age / LIFE).coerceIn(0.0, 1.0)
            b.mat.opacity = f
            b.flash.intensity = FLASH_ENERGY * f * f
            if (b.age >= LIFE) {
                scene.remove(b.container)
                it.remove()
            }
        }
    }

    private fun additive(color: String): dynamic {
        val p: dynamic = js("({})")
        p.color = color
        p.transparent = true
        p.opacity = 1.0
        p.blending = Three.AdditiveBlending
        p.depthWrite = false
        p.side = Three.DoubleSide
        return Three.MeshBasicMaterial(p)
    }

    // --- lightning fractal (ported from qlippostasis / the title scene) ---
    private fun buildFractal(verts: ArrayList<Double>, a: DoubleArray, b: DoubleArray, wStart: Double, wEnd: Double, depth: Int) {
        val n = SEGS
        val len = dist(a, b)
        val pts = Array(n + 1) { i ->
            val p = lerp(a, b, i.toDouble() / n)
            if (i in 1 until n) {
                doubleArrayOf(
                    p[0] + rand(-1.0, 1.0) * len * JITTER,
                    p[1] + rand(-1.0, 1.0) * len * JITTER,
                    p[2] + rand(-1.0, 1.0) * len * JITTER,
                )
            } else {
                p
            }
        }
        appendRibbon(verts, pts, wStart, wEnd)
        if (depth <= 0) return
        repeat(1 + (Util.random() * 3).toInt()) {
            val idx = 1 + (Util.random() * (n - 1)).toInt()
            val from = pts[idx]
            val dir = norm(sub(pts[idx + 1], pts[idx - 1]))
            val axis = norm(doubleArrayOf(rand(-1.0, 1.0), rand(-1.0, 1.0), rand(-1.0, 1.0)))
            val dev = rotate(dir, axis, rand(0.4, 1.0))
            val to = add(from, scale(dev, len * rand(0.2, 0.45)))
            val bw = lerp1(wStart, wEnd, idx.toDouble() / n) * 0.7
            buildFractal(verts, from, to, bw, bw * 0.15, depth - 1)
        }
    }

    private fun appendRibbon(verts: ArrayList<Double>, pts: Array<DoubleArray>, wStart: Double, wEnd: Double) {
        val cam = GlassShader.eye() // camera-facing ribbon (shared recovered camera eye)
        val n = pts.size - 1
        for (i in 0 until n) {
            val p0 = pts[i]
            val p1 = pts[i + 1]
            val w0 = lerp1(wStart, wEnd, i.toDouble() / n) * 0.5
            val w1 = lerp1(wStart, wEnd, (i + 1).toDouble() / n) * 0.5
            val perp0 = scale(boltPerp(p0, sub(p1, p0), cam), w0)
            val perp1 = scale(boltPerp(p1, if (i + 1 < n) sub(pts[i + 2], p1) else sub(p1, p0), cam), w1)
            tri(verts, sub(p0, perp0), add(p0, perp0), add(p1, perp1))
            tri(verts, sub(p0, perp0), add(p1, perp1), sub(p1, perp1))
        }
    }

    private fun boltPerp(point: DoubleArray, dir: DoubleArray, cam: DoubleArray): DoubleArray {
        val d = norm(dir)
        val view = norm(sub(point, cam))
        val perp = cross(d, view)
        return norm(if (lenSq(perp) < 1e-6) cross(d, doubleArrayOf(0.0, 1.0, 0.0)) else perp)
    }

    private fun tri(verts: ArrayList<Double>, a: DoubleArray, b: DoubleArray, c: DoubleArray) {
        listOf(a, b, c).forEach { v ->
            verts.add(v[0])
            verts.add(v[1])
            verts.add(v[2])
        }
    }

    private fun rand(lo: Double, hi: Double) = lo + Util.random() * (hi - lo)
}
