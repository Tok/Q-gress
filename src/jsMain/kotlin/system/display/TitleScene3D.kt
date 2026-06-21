package system.display

import external.Three
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set
import org.w3c.dom.HTMLCanvasElement
import util.SoundUtil
import util.Util
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A standalone three.js scene behind the **CHOOSE YOUR FACTION** title screen: a small live demo of
 * the game's own theatre. ~5 portals stand on a dark stage and, **randomly and automatically**, change
 * ownership (faction colour), level, and resonator count, throw **links** + **control fields** between
 * each other, and **emit thunderbolts** (tesla-coil retaliation) at one another — so the title shows
 * the portal animations in motion. Bolts are white / ENL-green / RES-blue only (white = neutral).
 *
 * This is NOT the MapLibre custom layer ([Scene3D], which loads later with the map); the title runs
 * its own renderer + RAF loop with simple lit materials, torn down when a faction is picked. Lightning
 * + thunder ported from qlippostasis (`BuildBoltFractal` / Poisson timing / `ThunderSynth.TeslaBolt`).
 */
@Suppress("TooManyFunctions", "LargeClass") // self-contained title scene: vec helpers + portals + bolts
object TitleScene3D {
    private const val ENL_COLOR = "#03dc03"
    private const val RES_COLOR = "#0088ff"
    private const val NEUTRAL_COLOR = "#cfcfcf"
    private const val WHITE = "#ffffff"

    private const val PORTAL_COUNT = 5
    private const val POLE_R = 0.34
    private const val POLE_H1 = 4.2 // pole height at L1
    private const val POLE_PER_LEVEL = 0.55
    private const val ORB_R = 1.7
    private const val RESO_RING = 0.9
    private const val RESO_ROD_R = 0.22
    private const val LEVEL_LERP = 3.0 // per-second ease toward the target level
    private const val FLASH_DECAY = 3.5 // capture-pop emissive flash decay (per second)

    private const val BOLT_SEGS = 7
    private const val BOLT_JITTER = 0.16
    private const val BOLT_BRANCH_DEPTH = 2
    private const val BOLT_WIDTH = 0.4
    private const val BOLT_END_WIDTH_FRAC = 0.18
    private const val BOLT_LIFE = 0.22
    private const val FLASH_RANGE = 26.0
    private const val FLASH_ENERGY = 9.0

    private const val EVENT_MEAN = 0.7 // mean Poisson gap (s) between portal events
    private const val EVENT_MIN = 0.18
    private const val EVENT_MAX = 2.2
    private const val LINK_LIFE = 6.0
    private const val FIELD_LIFE = 6.0
    private const val LINK_R = 0.12

    private const val CAM_Z = 34.0

    private class Portal(
        val group: dynamic,
        val orb: dynamic,
        val orbMat: dynamic,
        val pole: dynamic,
        val resoGroup: dynamic,
        val pos: DoubleArray, // base (x, y, z) on the stage
    ) {
        var color: String = "#cfcfcf"
        var level: Double = 1.0 // shown level (eased)
        var targetLevel: Double = 1.0
        var resos: Int = 0
        var flash: Double = 0.0 // capture pop 1→0
    }

    private class Timed(val obj: dynamic, val mat: dynamic, val baseOpacity: Double, var age: Double, val life: Double)
    private class Bolt(val container: dynamic, val mat: dynamic, val flash: dynamic, var age: Double)

    private var renderer: dynamic = null
    private var scene: dynamic = null
    private var camera: dynamic = null
    private var canvas: HTMLCanvasElement? = null

    private val portals = mutableListOf<Portal>()
    private val bolts = mutableListOf<Bolt>()
    private val timed = mutableListOf<Timed>() // links + fields that fade

    private var poleGeo: dynamic = null
    private var orbGeo: dynamic = null
    private var rodGeo: dynamic = null
    private var metalMat: dynamic = null

    private var running = false
    private var lastMs = 0.0
    private var nextStrike = 0.0
    private var nextEvent = 0.0
    private var rafId = 0

    private val resizeListener: (dynamic) -> Unit = { resize() }

    fun start(parent: org.w3c.dom.Element) {
        if (running) return
        val c = document.createElement("canvas") as HTMLCanvasElement
        c.className = "titleCanvas"
        parent.insertBefore(c, parent.firstChild)
        canvas = c

        val params: dynamic = js("({})")
        params.canvas = c
        params.alpha = true
        params.antialias = true
        val r = Three.WebGLRenderer(params)
        r.asDynamic().setPixelRatio(window.devicePixelRatio)
        renderer = r

        scene = Three.Scene()
        scene.add(Three.AmbientLight("#2a2f2a", 1.0))
        val key = Three.DirectionalLight(WHITE, 1.1)
        key.asDynamic().position.set(6.0, 14.0, 20.0)
        scene.add(key)
        camera = Three.PerspectiveCamera(55.0, 1.0, 0.1, 240.0)
        camera.position.set(0.0, 1.5, CAM_Z)
        resize()

        poleGeo = Three.CylinderGeometry(POLE_R, POLE_R, 1.0, 10) // unit-tall, scaled per level
        orbGeo = Three.SphereGeometry(ORB_R, 18, 14)
        rodGeo = Three.CylinderGeometry(RESO_ROD_R, RESO_ROD_R, 1.0, 6)
        metalMat = standardMat("#9a9a9a", "#000000", 0.85, 0.45)
        buildPortals()

        running = true
        lastMs = now()
        nextStrike = 0.4
        nextEvent = 0.6
        window.addEventListener("resize", resizeListener)
        rafId = window.requestAnimationFrame { frame() }
    }

    fun stop() {
        if (!running) return
        running = false
        window.cancelAnimationFrame(rafId)
        window.removeEventListener("resize", resizeListener)
        bolts.clear()
        timed.clear()
        portals.clear()
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

    private fun buildPortals() {
        val span = 17.0
        for (i in 0 until PORTAL_COUNT) {
            val x = -span + 2.0 * span * (i.toDouble() / (PORTAL_COUNT - 1))
            val pos = doubleArrayOf(x, rand(-7.0, -5.0), rand(-3.0, 3.0))
            val color = startColor(i)
            val group = Three.Group()
            group.asDynamic().position.set(pos[0], pos[1], pos[2])

            val pole = Three.Mesh(poleGeo, metalMat)
            val orbMat = standardMat(color, color, 0.2, 0.4, 0.7)
            val orb = Three.Mesh(orbGeo, orbMat)
            val resoGroup = Three.Group()
            group.asDynamic().add(pole)
            group.asDynamic().add(orb)
            group.asDynamic().add(resoGroup)
            scene.add(group)

            val p = Portal(group, orb, orbMat, pole, resoGroup, pos)
            p.color = color
            p.targetLevel = rand(1.0, 6.0)
            p.resos = (Util.random() * 9).toInt()
            applyPortal(p)
            portals.add(p)
        }
    }

    private fun startColor(i: Int) = when (i % 3) {
        0 -> ENL_COLOR
        1 -> RES_COLOR
        else -> NEUTRAL_COLOR
    }

    // Position the pole + orb + reso rods for the portal's current (eased) level + reso count.
    private fun applyPortal(p: Portal) {
        val poleH = POLE_H1 + (p.level - 1.0) * POLE_PER_LEVEL
        p.pole.asDynamic().scale.set(1.0, poleH, 1.0)
        p.pole.asDynamic().position.set(0.0, poleH / 2.0, 0.0)
        val s = 0.6 + (p.level - 1.0) / 7.0 * 0.7
        p.orb.asDynamic().scale.set(s, s, s)
        p.orb.asDynamic().position.set(0.0, poleH + ORB_R * s, 0.0)
        rebuildResos(p, poleH)
    }

    private fun rebuildResos(p: Portal, poleH: Double) {
        val g = p.resoGroup
        while ((g.children.length as Int) > 0) g.remove(g.children[0])
        val rodLen = 1.4
        for (i in 0 until p.resos.coerceIn(0, 8)) {
            val ang = i * PI / 4.0
            val rod = Three.Mesh(rodGeo, standardMat(p.color, p.color, 0.25, 0.4, 0.5))
            rod.asDynamic().scale.set(1.0, rodLen, 1.0)
            rod.asDynamic().position.set(RESO_RING * cos(ang), poleH * 0.74, RESO_RING * sin(ang))
            g.asDynamic().add(rod)
        }
    }

    private fun frame() {
        if (!running) return
        val t = now()
        val dt = ((t - lastMs) / 1000.0).coerceIn(0.0, 0.1)
        lastMs = t

        nextStrike -= dt
        if (nextStrike <= 0.0) {
            spawnBolt()
            nextStrike = poisson(0.5, 0.1, 2.4)
        }
        nextEvent -= dt
        if (nextEvent <= 0.0) {
            portalEvent()
            nextEvent = poisson(EVENT_MEAN, EVENT_MIN, EVENT_MAX)
        }
        updatePortals(dt)
        updateBolts(dt)
        updateTimed(dt)
        renderer?.render(scene, camera)
        rafId = window.requestAnimationFrame { frame() }
    }

    private fun updatePortals(dt: Double) {
        portals.forEach { p ->
            var dirty = false
            if (kotlin.math.abs(p.level - p.targetLevel) > 0.01) {
                p.level += (p.targetLevel - p.level) * (LEVEL_LERP * dt).coerceAtMost(1.0)
                dirty = true
            }
            if (p.flash > 0.0) {
                p.flash = (p.flash - FLASH_DECAY * dt).coerceAtLeast(0.0)
                p.orbMat.emissiveIntensity = 0.4 + p.flash * 2.4
            }
            if (dirty) applyPortal(p)
        }
    }

    // A random, automatic portal event: capture (flip faction), level up/down, reso change, link, or field.
    private fun portalEvent() {
        if (portals.isEmpty()) return
        val p = portals[(Util.random() * portals.size).toInt()]
        when ((Util.random() * 5).toInt()) {
            0 -> capture(p)
            1 -> p.targetLevel = rand(1.0, 8.0)
            2 -> {
                p.resos = (Util.random() * 9).toInt()
                applyPortal(p)
            }
            3 -> makeLink(p)
            else -> makeField()
        }
    }

    private fun capture(p: Portal) {
        p.color = when ((Util.random() * 3).toInt()) {
            0 -> ENL_COLOR
            1 -> RES_COLOR
            else -> NEUTRAL_COLOR
        }
        p.orbMat.color.set(p.color)
        p.orbMat.emissive.set(p.color)
        p.flash = 1.0 // emissive pop
        applyPortal(p) // reso rods take the new colour
        emitBolt(orbWorld(p), upFrom(p), p.color) // a discharge on capture
    }

    private fun makeLink(from: Portal) {
        val to = portals[(Util.random() * portals.size).toInt()]
        if (to === from) return
        val a = orbWorld(from)
        val b = orbWorld(to)
        val mid = scale(add(a, b), 0.5)
        val len = dist(a, b)
        val geo = Three.CylinderGeometry(LINK_R, LINK_R, len, 6)
        val mat = basicAdditive(from.color, 0.6)
        val mesh = Three.Mesh(geo, mat)
        mesh.asDynamic().position.set(mid[0], mid[1], mid[2])
        orientY(mesh, sub(b, a))
        scene.add(mesh)
        timed.add(Timed(mesh, mat, 0.6, 0.0, LINK_LIFE))
    }

    private fun makeField() {
        if (portals.size < 3) return
        val idx = (0 until portals.size).shuffled().take(3)
        val verts = ArrayList<Double>()
        idx.forEach {
            val w = orbWorld(portals[it])
            verts.add(w[0])
            verts.add(w[1])
            verts.add(w[2])
        }
        val arr = Float32Array(verts.size)
        for (i in verts.indices) arr[i] = verts[i].toFloat()
        val geo = Three.BufferGeometry()
        geo.asDynamic().setAttribute("position", Three.Float32BufferAttribute(arr, 3))
        val mat = basicAdditive(portals[idx[0]].color, 0.16)
        mat.side = Three.DoubleSide
        val mesh = Three.Mesh(geo, mat)
        scene.add(mesh)
        timed.add(Timed(mesh, mat, 0.16, 0.0, FIELD_LIFE))
    }

    private fun updateTimed(dt: Double) {
        val it = timed.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.age += dt
            val f = (1.0 - e.age / e.life).coerceIn(0.0, 1.0)
            e.mat.opacity = e.baseOpacity * f
            if (e.age >= e.life) {
                scene.remove(e.obj)
                it.remove()
            }
        }
    }

    private fun spawnBolt() {
        if (portals.isEmpty()) return
        val from = portals[(Util.random() * portals.size).toInt()]
        val a = orbWorld(from)
        // Retaliate at another portal, or discharge straight up into the storm.
        val target = if (Util.random() < 0.6 && portals.size > 1) {
            var to = from
            while (to === from) to = portals[(Util.random() * portals.size).toInt()]
            orbWorld(to)
        } else {
            upFrom(from)
        }
        emitBolt(a, target, boltColor(from.color))
    }

    private fun boltColor(portalColor: String): String = if (Util.random() < 0.25) WHITE else portalColor

    private fun emitBolt(start: DoubleArray, end: DoubleArray, color: String) {
        val verts = ArrayList<Double>()
        buildFractal(verts, start, end, BOLT_WIDTH, BOLT_WIDTH * BOLT_END_WIDTH_FRAC, BOLT_BRANCH_DEPTH)
        val arr = Float32Array(verts.size)
        for (i in verts.indices) arr[i] = verts[i].toFloat()
        val geo = Three.BufferGeometry()
        geo.asDynamic().setAttribute("position", Three.Float32BufferAttribute(arr, 3))
        val mat = basicAdditive(color, 1.0)
        mat.side = Three.DoubleSide
        val container = Three.Group()
        container.asDynamic().add(Three.Mesh(geo, mat))
        val flash: dynamic = Three.PointLight(color, FLASH_ENERGY, FLASH_RANGE)
        flash.position.set(end[0], end[1], end[2])
        container.asDynamic().add(flash)
        scene.add(container)
        bolts.add(Bolt(container, mat, flash, 0.0))
        SoundUtil.playThunderSound((start[0] / 20.0).coerceIn(-1.0, 1.0), 0.7 + Util.random() * 0.7)
    }

    private fun updateBolts(dt: Double) {
        val it = bolts.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.age += dt
            val f = (1.0 - b.age / BOLT_LIFE).coerceIn(0.0, 1.0)
            b.mat.opacity = f
            b.flash.intensity = FLASH_ENERGY * f * f
            if (b.age >= BOLT_LIFE) {
                scene.remove(b.container)
                it.remove()
            }
        }
    }

    // World position of a portal's orb centre (its group is at pos; orb rides the pole top).
    private fun orbWorld(p: Portal): DoubleArray {
        val poleH = POLE_H1 + (p.level - 1.0) * POLE_PER_LEVEL
        val s = 0.6 + (p.level - 1.0) / 7.0 * 0.7
        return doubleArrayOf(p.pos[0], p.pos[1] + poleH + ORB_R * s, p.pos[2])
    }

    private fun upFrom(p: Portal): DoubleArray {
        val o = orbWorld(p)
        return doubleArrayOf(o[0] + rand(-4.0, 4.0), o[1] + rand(8.0, 16.0), o[2] + rand(-4.0, 4.0))
    }

    private fun poisson(mean: Double, lo: Double, hi: Double): Double {
        val u = Util.random().coerceIn(0.0001, 0.9999)
        return (-ln(u) * mean).coerceIn(lo, hi)
    }

    // --- lightning fractal (ported from qlippostasis) ---
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
        val cam = doubleArrayOf(0.0, 1.5, CAM_Z)
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

    // --- three.js material helpers ---
    private fun standardMat(color: String, emissive: String, rough: Double, metal: Double, emissiveIntensity: Double = 0.0): dynamic {
        val p: dynamic = js("({})")
        p.color = color
        p.emissive = emissive
        p.emissiveIntensity = emissiveIntensity
        p.roughness = rough
        p.metalness = metal
        return Three.MeshStandardMaterial(p)
    }

    private fun basicAdditive(color: String, opacity: Double): dynamic {
        val p: dynamic = js("({})")
        p.color = color
        p.transparent = true
        p.opacity = opacity
        p.blending = Three.AdditiveBlending
        p.depthWrite = false
        return Three.MeshBasicMaterial(p)
    }

    // Orient a unit-Y cylinder mesh to point along [dir].
    private fun orientY(mesh: dynamic, dir: DoubleArray) {
        val d = norm(dir)
        val from = Three.Vector3(0.0, 1.0, 0.0)
        val to = Three.Vector3(d[0], d[1], d[2])
        val q = Three.Quaternion().setFromUnitVectors(from, to)
        mesh.quaternion.copy(q)
        mesh.scale.set(1.0, dist(doubleArrayOf(0.0, 0.0, 0.0), dir), 1.0)
    }

    // --- vec3 helpers ---
    private fun sub(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])
    private fun add(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0] + b[0], a[1] + b[1], a[2] + b[2])
    private fun scale(a: DoubleArray, s: Double) = doubleArrayOf(a[0] * s, a[1] * s, a[2] * s)
    private fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    private fun lenSq(a: DoubleArray) = dot(a, a)
    private fun cross(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])

    private fun norm(a: DoubleArray): DoubleArray {
        val l = sqrt(lenSq(a))
        return if (l < 1e-9) doubleArrayOf(0.0, 1.0, 0.0) else scale(a, 1.0 / l)
    }

    private fun lerp(a: DoubleArray, b: DoubleArray, t: Double) = doubleArrayOf(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t)

    private fun lerp1(a: Double, b: Double, t: Double) = a + (b - a) * t
    private fun dist(a: DoubleArray, b: DoubleArray) = sqrt(lenSq(sub(a, b)))

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
