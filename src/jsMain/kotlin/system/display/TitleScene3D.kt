package system.display

import external.Three
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set
import org.w3c.dom.HTMLCanvasElement
import util.SoundUtil
import util.Util
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A standalone three.js scene behind the **CHOOSE YOUR FACTION** title screen: randomly-timed
 * **thunderbolts** arc across a dark storm in **white / ENL-green / RES-blue** only (white reads as
 * neutral, so the palette stays grayscale-plus-faction). Ported from the qlippostasis title-screen
 * lightning (`BuildBoltFractal` / Poisson `NextStrikeDelay`) — fractal tapered camera-facing ribbons
 * with an additive emissive material, a flash light, and a procedural thunder clap per strike.
 *
 * This is NOT the MapLibre custom layer ([Scene3D]) — that loads later with the map; the title runs
 * its own renderer + RAF loop, torn down when a faction is picked.
 */
@Suppress("TooManyFunctions") // small self-contained vec3 helpers + the scene/bolt logic
object TitleScene3D {
    private const val ENL_COLOR = "#03dc03"
    private const val RES_COLOR = "#0088ff"
    private const val NEUTRAL_COLOR = "#ffffff"

    private const val BOLT_SEGS = 7 // jagged segments along the main path
    private const val BOLT_JITTER = 0.16 // sideways wander as a fraction of length
    private const val BOLT_BRANCH_DEPTH = 2 // recursive fork depth
    private const val BOLT_WIDTH = 0.42 // ribbon half-width at the start (metres)
    private const val BOLT_END_WIDTH_FRAC = 0.18 // taper to this fraction at the tip
    private const val BOLT_LIFE = 0.2 // seconds a bolt stays before it has fully faded
    private const val FLASH_RANGE = 26.0
    private const val FLASH_ENERGY = 9.0

    private const val STRIKE_MEAN = 0.5 // mean Poisson gap (seconds) between strikes
    private const val STRIKE_MIN_GAP = 0.1
    private const val STRIKE_MAX_GAP = 2.4

    private const val CAM_Z = 34.0
    private const val SPAWN_X = 24.0 // half-width of the strike volume
    private const val SPAWN_TOP = 17.0
    private const val SPAWN_BOTTOM = -17.0
    private const val SPAWN_Z = 5.0

    private class Bolt(val container: dynamic, val mat: dynamic, val flash: dynamic, val flashEnergy: Double, var age: Double)

    private var renderer: dynamic = null
    private var scene: dynamic = null
    private var camera: dynamic = null
    private var canvas: HTMLCanvasElement? = null
    private val bolts = mutableListOf<Bolt>()

    private var running = false
    private var lastMs = 0.0
    private var nextStrike = 0.0
    private var rafId = 0

    private val resizeListener: (dynamic) -> Unit = { resize() }

    fun start(parent: org.w3c.dom.Element) {
        if (running) return
        val c = document.createElement("canvas") as HTMLCanvasElement
        c.className = "titleCanvas"
        parent.insertBefore(c, parent.firstChild) // behind the title content
        canvas = c

        val params: dynamic = js("({})")
        params.canvas = c
        params.alpha = true
        params.antialias = true
        val r = Three.WebGLRenderer(params)
        r.asDynamic().setPixelRatio(window.devicePixelRatio)
        renderer = r

        scene = Three.Scene()
        scene.add(Three.AmbientLight("#1a1f1a", 1.0))
        camera = Three.asDynamic().PerspectiveCamera(55.0, 1.0, 0.1, 240.0)
        camera.position.set(0.0, 0.0, CAM_Z)
        resize()

        running = true
        lastMs = now()
        nextStrike = STRIKE_MIN_GAP
        window.addEventListener("resize", resizeListener)
        rafId = window.requestAnimationFrame { frame() }
    }

    fun stop() {
        if (!running) return
        running = false
        window.cancelAnimationFrame(rafId)
        window.removeEventListener("resize", resizeListener)
        bolts.forEach { scene?.remove(it.container) }
        bolts.clear()
        renderer?.dispose()
        renderer = null
        scene = null
        camera = null
        canvas?.remove()
        canvas = null
    }

    private fun resize() {
        val w = window.innerWidth
        val h = window.innerHeight
        renderer?.setSize(w, h)
        val cam = camera ?: return
        cam.aspect = w.toDouble() / h.toDouble()
        cam.updateProjectionMatrix()
    }

    private fun frame() {
        if (!running) return
        val t = now()
        val dt = ((t - lastMs) / 1000.0).coerceIn(0.0, 0.1)
        lastMs = t

        nextStrike -= dt
        if (nextStrike <= 0.0) {
            spawnBolt()
            nextStrike = nextStrikeDelay()
        }
        updateBolts(dt)
        renderer?.render(scene, camera)
        rafId = window.requestAnimationFrame { frame() }
    }

    // Poisson gap → uneven, occasionally clustered strikes (not a fixed metronome).
    private fun nextStrikeDelay(): Double {
        val u = Util.random().coerceIn(0.0001, 0.9999)
        return (-ln(u) * STRIKE_MEAN).coerceIn(STRIKE_MIN_GAP, STRIKE_MAX_GAP)
    }

    private fun boltColor(): String = when ((Util.random() * 5).toInt()) {
        0 -> NEUTRAL_COLOR // ~1 in 5 white (neutral); the rest split green / blue evenly
        1, 2 -> ENL_COLOR
        else -> RES_COLOR
    }

    private fun spawnBolt() {
        val color = boltColor()
        val sx = rand(-SPAWN_X, SPAWN_X)
        val start = doubleArrayOf(sx, rand(SPAWN_TOP * 0.45, SPAWN_TOP), rand(-SPAWN_Z, SPAWN_Z))
        val end = doubleArrayOf(sx + rand(-10.0, 10.0), rand(SPAWN_BOTTOM, SPAWN_BOTTOM * 0.2), rand(-SPAWN_Z, SPAWN_Z))
        addBolt(start, end, color)
        // pan by horizontal position; brighter/closer-sounding for white "player-ish" bolts
        SoundUtil.playThunderSound((sx / SPAWN_X).coerceIn(-1.0, 1.0), 0.7 + Util.random() * 0.7)
    }

    private fun addBolt(start: DoubleArray, end: DoubleArray, color: String) {
        val verts = ArrayList<Double>()
        buildFractal(verts, start, end, BOLT_WIDTH, BOLT_WIDTH * BOLT_END_WIDTH_FRAC, BOLT_BRANCH_DEPTH)
        val arr = Float32Array(verts.size)
        for (i in verts.indices) arr[i] = verts[i].toFloat()
        val geo = Three.BufferGeometry()
        geo.asDynamic().setAttribute("position", Three.asDynamic().Float32BufferAttribute(arr, 3))

        val matParams: dynamic = js("({})")
        matParams.color = color
        matParams.transparent = true
        matParams.opacity = 1.0
        matParams.blending = Three.AdditiveBlending
        matParams.depthWrite = false
        matParams.side = Three.asDynamic().DoubleSide
        val mat = Three.MeshBasicMaterial(matParams)

        val container = Three.Group()
        container.asDynamic().add(Three.Mesh(geo, mat))
        val flash = Three.asDynamic().PointLight(color, FLASH_ENERGY, FLASH_RANGE)
        flash.position.set(end[0], end[1], end[2])
        container.asDynamic().add(flash)
        scene.add(container)
        bolts.add(Bolt(container, mat, flash, FLASH_ENERGY, 0.0))
    }

    private fun updateBolts(dt: Double) {
        val it = bolts.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.age += dt
            val f = (1.0 - b.age / BOLT_LIFE).coerceIn(0.0, 1.0)
            b.mat.opacity = f
            b.flash.intensity = b.flashEnergy * f * f
            if (b.age >= BOLT_LIFE) {
                scene.remove(b.container)
                it.remove()
            }
        }
    }

    // Jagged path a→b plus recursive forks → a tree-fractal lightning shape (ported from qlippostasis).
    private fun buildFractal(verts: ArrayList<Double>, a: DoubleArray, b: DoubleArray, wStart: Double, wEnd: Double, depth: Int) {
        val n = BOLT_SEGS
        val len = dist(a, b)
        val pts = Array(n + 1) { i ->
            val t = i.toDouble() / n
            val p = lerp(a, b, t)
            if (i in 1 until n) {
                doubleArrayOf(
                    p[0] + rand(-1.0, 1.0) * len * BOLT_JITTER,
                    p[1] + rand(-1.0, 1.0) * len * BOLT_JITTER,
                    p[2] + rand(-1.0, 1.0) * len * BOLT_JITTER,
                )
            } else {
                p
            }
        }
        appendRibbon(verts, pts, wStart, wEnd)
        if (depth <= 0) return
        val branches = 1 + (Util.random() * 3).toInt()
        repeat(branches) {
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

    // Tapered camera-facing ribbon: 2 triangles per segment, perpendicular to both the bolt and the view.
    private fun appendRibbon(verts: ArrayList<Double>, pts: Array<DoubleArray>, wStart: Double, wEnd: Double) {
        val cam = doubleArrayOf(0.0, 0.0, CAM_Z)
        val n = pts.size - 1
        for (i in 0 until n) {
            val p0 = pts[i]
            val p1 = pts[i + 1]
            val w0 = lerp1(wStart, wEnd, i.toDouble() / n) * 0.5
            val w1 = lerp1(wStart, wEnd, (i + 1).toDouble() / n) * 0.5
            val perp0 = scale(boltPerp(p0, sub(p1, p0), cam), w0)
            val perp1 = scale(boltPerp(p1, if (i + 1 < n) sub(pts[i + 2], p1) else sub(p1, p0), cam), w1)
            val a0 = sub(p0, perp0)
            val b0 = add(p0, perp0)
            val a1 = sub(p1, perp1)
            val b1 = add(p1, perp1)
            tri(verts, a0, b0, b1)
            tri(verts, a0, b1, a1)
        }
    }

    private fun boltPerp(point: DoubleArray, dir: DoubleArray, cam: DoubleArray): DoubleArray {
        val d = norm(dir)
        val view = norm(sub(point, cam))
        var perp = cross(d, view)
        if (lenSq(perp) < 1e-6) perp = cross(d, doubleArrayOf(0.0, 1.0, 0.0))
        return norm(perp)
    }

    private fun tri(verts: ArrayList<Double>, a: DoubleArray, b: DoubleArray, c: DoubleArray) {
        verts.add(a[0])
        verts.add(a[1])
        verts.add(a[2])
        verts.add(b[0])
        verts.add(b[1])
        verts.add(b[2])
        verts.add(c[0])
        verts.add(c[1])
        verts.add(c[2])
    }

    // --- small vec3 helpers (DoubleArray of size 3) ---
    private fun sub(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])
    private fun add(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0] + b[0], a[1] + b[1], a[2] + b[2])
    private fun scale(a: DoubleArray, s: Double) = doubleArrayOf(a[0] * s, a[1] * s, a[2] * s)
    private fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    private fun lenSq(a: DoubleArray) = dot(a, a)
    private fun cross(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])

    private fun norm(a: DoubleArray): DoubleArray {
        val l = sqrt(lenSq(a))
        return if (l < 1e-9) doubleArrayOf(0.0, 0.0, 1.0) else scale(a, 1.0 / l)
    }

    private fun lerp(a: DoubleArray, b: DoubleArray, t: Double) = doubleArrayOf(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t)

    private fun lerp1(a: Double, b: Double, t: Double) = a + (b - a) * t
    private fun dist(a: DoubleArray, b: DoubleArray) = sqrt(lenSq(sub(a, b)))

    // Rodrigues' rotation of [v] about unit [axis] by [angle].
    private fun rotate(v: DoubleArray, axis: DoubleArray, angle: Double): DoubleArray {
        val c = cos(angle)
        val s = sin(angle)
        val term2 = scale(cross(axis, v), s)
        val term3 = scale(axis, dot(axis, v) * (1.0 - c))
        return add(add(scale(v, c), term2), term3)
    }

    private fun rand(lo: Double, hi: Double) = lo + Util.random() * (hi - lo)
    private fun now() = js("performance.now()") as Double
}
