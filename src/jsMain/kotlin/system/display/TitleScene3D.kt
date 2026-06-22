package system.display

import config.Sim
import external.Three
import items.level.LevelColor
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set
import org.w3c.dom.HTMLCanvasElement
import util.SoundUtil
import util.Util
import util.data.Pos
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
// TooGenericExceptionCaught: 3D setup is best-effort — any failure must degrade to bolts, not crash.
@Suppress("TooManyFunctions", "LargeClass", "TooGenericExceptionCaught")
object TitleScene3D {
    private const val ENL_COLOR = "#03dc03"
    private const val RES_COLOR = "#0088ff"
    private const val NEUTRAL_COLOR = "#cfcfcf"
    private const val WHITE = "#ffffff"

    private const val PORTAL_COUNT = 5
    private const val PORTALS_ENABLED = true // on: synced to in-game proportions (gasket + double-shell orb)

    // Game proportions (Scene3D), built at game scale then shrunk by TITLE_SCALE to fit the stage —
    // so the title portals read exactly like the in-game ones (tall pole, smaller orb, reso collar).
    private const val POLE_R = 2.0
    private const val POLE_H = 22.5 // pole height at L1; ×poleScale(level)
    private const val TOP_R = 7.0 // orb radius; ×orbScale(level)
    private const val INNER_SHELL_FRAC = 0.89 // inner glass shell radius (× orb) — gives the orb wall thickness
    private const val RESO_RADIUS_FRAC = 1.7 // reso slot distance from the pole axis (× POLE_R)
    private const val RESO_COLLAR_FRAC = 0.78 // collar height as a fraction of pole height
    private const val RESO_ROD_LEN_FRAC = 0.22 // rod length as a fraction of pole height
    private const val RESO_ROD_R = POLE_R * 0.26
    private const val TITLE_SCALE = 0.22 // game-scale portal → title stage
    private const val LEVEL_LERP = 3.0 // per-second ease toward the target level
    private const val FLASH_DECAY = 3.5 // capture scale-pop decay (per second)

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

    private const val STAGE_TOP = -7.0 // the portals stand on a concrete stage at this y
    private const val STAGE_W = 46.0
    private const val STAGE_D = 22.0
    private const val STAGE_THICK = 4.0
    private const val SKY_TOP = "#0a1430" // deep night-blue overhead
    private const val SKY_MID = "#1b3a6b" // horizon glow
    private const val SKY_BOTTOM = "#05060a" // dark ground haze

    private class Portal(
        val group: dynamic,
        val orb: dynamic,
        var orbMat: dynamic, // swapped to a new faction glass on capture
        val pole: dynamic,
        val gasket: dynamic, // rubber donut at the pole top (rides up with the pole as the level grows)
        val resoGroup: dynamic,
        val pos: DoubleArray, // base (x, y, z) on the stage
    ) {
        var color: String = "#cfcfcf"
        val resoLevels = IntArray(8) // per-slot reso level (0 = empty); the portal level derives from these
        var level: Double = 1.0 // eased toward the derived level
        var targetLevel: Double = 1.0
        var flash: Double = 0.0 // capture scale-pop 1→0
    }

    private class Timed(val obj: dynamic, val mat: dynamic, val baseOpacity: Double, var age: Double, val life: Double)
    private class Bolt(val container: dynamic, val mat: dynamic, val flash: dynamic, var age: Double)
    private class Xmp(val mesh: dynamic, val mat: dynamic, var age: Double)

    private const val XMP_LIFE = 0.5
    private const val XMP_MAX_R = 13.0

    private var renderer: dynamic = null
    private var scene: dynamic = null
    private var camera: dynamic = null
    private var canvas: HTMLCanvasElement? = null

    private val portals = mutableListOf<Portal>()
    private val bolts = mutableListOf<Bolt>()
    private val timed = mutableListOf<Timed>() // links + fields that fade
    private val xmps = mutableListOf<Xmp>() // expanding XMP burst spheres

    private var poleGeo: dynamic = null
    private var orbGeo: dynamic = null
    private var rodGeo: dynamic = null
    private var gasketGeo: dynamic = null

    private var running = false
    private var lastMs = 0.0
    private var nextStrike = 0.0
    private var nextEvent = 0.0
    private var rafId = 0

    private val resizeListener: (dynamic) -> Unit = { resize() }
    private val unlockListener: (dynamic) -> Unit = { SoundUtil.enableAudio() } // first gesture → bolt thunder

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
        scene.add(Three.AmbientLight("#6a6f6a", 1.6)) // bright enough that the metal poles read, not black
        camera = Three.PerspectiveCamera(55.0, 1.0, 0.1, 240.0)
        camera.position.set(0.0, 1.5, CAM_Z)
        camera.asDynamic().lookAt(0.0, STAGE_TOP + 5.0, 0.0) // aim down at the portals on the stage
        resize()

        // Core scene is up — start the loop unconditionally so bolts ALWAYS render.
        running = true
        lastMs = now()
        nextStrike = 0.4
        nextEvent = 0.6
        window.addEventListener("resize", resizeListener)
        window.addEventListener("pointerdown", unlockListener) // autoplay: unlock audio on first click
        rafId = window.requestAnimationFrame { frame() }

        // Portals + key light + sound listener are best-effort: if any of this throws, the bolts keep
        // running via the sky-bolt fallback in spawnBolt.
        try {
            val key = Three.DirectionalLight(WHITE, 2.2)
            key.asDynamic().position.set(8.0, 16.0, 24.0)
            scene.add(key)
            val rim = Three.DirectionalLight("#b6c0d8", 1.1) // cool back-rim so poles separate from the dark
            rim.asDynamic().position.set(-12.0, -2.0, 12.0)
            scene.add(rim)
            poleGeo = Three.CylinderGeometry(POLE_R, POLE_R, 1.0, 12) // unit-tall, scaled to pole height
            orbGeo = Three.SphereGeometry(TOP_R, 20, 16) // radius TOP_R, scaled by orbScale(level)
            rodGeo = Three.CylinderGeometry(RESO_ROD_R, RESO_ROD_R, 1.0, 8) // unit, scaled to rod length
            gasketGeo = Three.TorusGeometry(POLE_R * 1.15, POLE_R * 0.4, 10, 20) // rubber donut at the pole top
            if (PORTALS_ENABLED) buildPortals() // portals first (priority); sky/stage below can't break them
            addSky() // gradient backdrop (also gives the chrome poles something to reflect)
            addStage() // a concrete slab the portals stand on
            // neutral listener so the (3D-panned) game sounds we reuse pan by portal x and stay audible
            SoundUtil.updateListener(doubleArrayOf(0.0, 0.0, 0.0), doubleArrayOf(0.0, 0.0, -1.0), doubleArrayOf(0.0, 1.0, 0.0))
        } catch (e: Throwable) {
            console.error("TitleScene3D portal setup failed; bolts continue:", e)
        }
    }

    fun stop() {
        if (!running) return
        running = false
        window.cancelAnimationFrame(rafId)
        window.removeEventListener("resize", resizeListener)
        window.removeEventListener("pointerdown", unlockListener)
        bolts.clear()
        timed.clear()
        xmps.clear()
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

    // A vertical gradient sky as the scene background (and reflection env for the chrome poles).
    private fun addSky() {
        val c = document.createElement("canvas") as HTMLCanvasElement
        c.width = 8
        c.height = 256
        val ctx = c.getContext("2d").asDynamic()
        val grad = ctx.createLinearGradient(0.0, 0.0, 0.0, 256.0)
        grad.addColorStop(0.0, SKY_TOP)
        grad.addColorStop(0.55, SKY_MID)
        grad.addColorStop(1.0, SKY_BOTTOM)
        ctx.fillStyle = grad
        ctx.fillRect(0, 0, 8, 256)
        val tex = Three.CanvasTexture(c)
        tex.asDynamic().mapping = Three.EquirectangularReflectionMapping
        scene.background = tex // `scene` is already dynamic — no .asDynamic()
        scene.environment = tex
    }

    // A matte concrete slab the portals stand on (top at STAGE_TOP).
    private fun addStage() {
        val p: dynamic = js("({})")
        p.color = "#6b6b6b"
        p.metalness = 0.0
        p.roughness = 0.95
        val slab = Three.Mesh(Three.BoxGeometry(STAGE_W, STAGE_THICK, STAGE_D), Three.MeshStandardMaterial(p))
        slab.asDynamic().position.set(0.0, STAGE_TOP - STAGE_THICK / 2.0, 0.0)
        scene.add(slab)
    }

    // A lit, faction-tinted glassy orb (the game's GlassShader needs a per-frame camera eye that only
    // Scene3D supplies — in this standalone scene it renders black, so use a simple emissive material).
    private fun titleGlass(color: String): dynamic {
        val p: dynamic = js("({})")
        p.color = color
        p.emissive = color
        p.emissiveIntensity = 0.55
        p.metalness = 0.0
        p.roughness = 0.25
        p.transparent = true
        p.opacity = 0.78
        return Three.MeshStandardMaterial(p)
    }

    private fun buildPortals() {
        val span = 17.0
        for (i in 0 until PORTAL_COUNT) {
            try {
                val x = -span + 2.0 * span * (i.toDouble() / (PORTAL_COUNT - 1))
                val pos = doubleArrayOf(x, STAGE_TOP, rand(-4.0, 4.0)) // stand on the concrete stage
                val color = startColor(i)
                val group = Three.Group()
                group.asDynamic().position.set(pos[0], pos[1], pos[2])
                group.asDynamic().scale.set(TITLE_SCALE, TITLE_SCALE, TITLE_SCALE) // game-scale → stage

                val pole = Three.Mesh(poleGeo, Materials.metal()) // chrome + envMap (visible without scene lights)
                val orbMat = titleGlass(color) // lit emissive glass (GlassShader renders black without Scene3D)
                val orb = Three.Mesh(orbGeo, orbMat)
                val inner = Three.Mesh(orbGeo, orbMat) // double-shell: a concentric inner surface = glass wall thickness
                inner.asDynamic().scale.set(INNER_SHELL_FRAC, INNER_SHELL_FRAC, INNER_SHELL_FRAC)
                orb.asDynamic().add(inner) // child of the orb → inherits its per-level scale + position
                val gasket = Three.Mesh(gasketGeo, Materials.rubber()) // black rubber ring under the orb
                gasket.asDynamic().rotation.x = PI / 2 // lie flat (ring around the up axis) in this Y-up scene
                val resoGroup = Three.Group()
                group.asDynamic().add(pole)
                group.asDynamic().add(gasket)
                group.asDynamic().add(orb)
                group.asDynamic().add(resoGroup)
                scene.add(group)

                val p = Portal(group, orb, orbMat, pole, gasket, resoGroup, pos)
                p.color = color
                randomLoadout(p)
                p.level = derivedLevel(p)
                applyPortal(p)
                portals.add(p)
            } catch (e: Throwable) {
                console.error("TitleScene3D: portal $i failed:", e)
            }
        }
    }

    private fun startColor(i: Int) = when (i % 3) {
        0 -> ENL_COLOR
        1 -> RES_COLOR
        else -> NEUTRAL_COLOR
    }

    // Fill a random subset of the 8 reso slots so portals start populated (level derives from these).
    private fun randomLoadout(p: Portal) {
        for (s in 0 until 8) {
            p.resoLevels[s] = if (Util.random() < 0.7) 1 + (Util.random() * 8).toInt() else 0
        }
        p.targetLevel = derivedLevel(p)
    }

    // Portal level = average of the 8 slots (empty = 0), like the game; clamped so a sparse portal still reads.
    private fun derivedLevel(p: Portal) = (p.resoLevels.sum() / 8.0).coerceIn(1.0, 8.0)

    // Match the in-game growth (Scene3D): generous per-level so levels read clearly. Pole 1.0→2.2×, orb 0.45→1.6.
    private fun poleScale(level: Double) = 1.0 + (level.coerceIn(1.0, 8.0) - 1.0) / 7.0 * 1.2
    private fun orbScale(level: Double) = 0.45 + (level.coerceIn(1.0, 8.0) - 1.0) / 7.0 * 1.15

    // Pose the pole + orb + reso rods for the portal's (eased) level — built at GAME scale; the group's
    // TITLE_SCALE shrinks the whole thing to fit the stage, so the proportions match the in-game portal.
    private fun applyPortal(p: Portal) {
        val poleH = POLE_H * poleScale(p.level)
        p.pole.asDynamic().scale.set(1.0, poleH, 1.0)
        p.pole.asDynamic().position.set(0.0, poleH / 2.0, 0.0)
        p.gasket.asDynamic().position.set(0.0, poleH, 0.0) // rides the pole top
        val os = orbScale(p.level) * (1.0 + p.flash * 0.3) // flash = a brief scale pop
        p.orb.asDynamic().scale.set(os, os, os)
        p.orb.asDynamic().position.set(0.0, poleH + TOP_R * orbScale(p.level), 0.0)
        rebuildResos(p, poleH)
    }

    private fun rebuildResos(p: Portal, poleH: Double) {
        val g = p.resoGroup
        g.asDynamic().clear()
        val collarY = poleH * RESO_COLLAR_FRAC
        val rodLen = poleH * RESO_ROD_LEN_FRAC
        val ringR = POLE_R * RESO_RADIUS_FRAC
        for (i in 0 until 8) {
            val lvl = p.resoLevels[i]
            if (lvl <= 0) continue // empty slot
            val ang = i * PI / 4.0
            val rod = Three.Mesh(rodGeo, Materials.resonator(LevelColor.map[lvl] ?: "#ffffff"))
            rod.asDynamic().scale.set(1.0, rodLen, 1.0)
            rod.asDynamic().position.set(ringR * cos(ang), collarY, ringR * sin(ang))
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
        updateXmps(dt)
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
                dirty = true // the flash is a scale pop applied in applyPortal
            }
            if (dirty) applyPortal(p)
        }
    }

    // A random, automatic portal event: capture (flip faction), deploy/destroy a resonator (which
    // raises/lowers the level), link, or field.
    private fun portalEvent() {
        if (portals.isEmpty()) return
        val p = portals[(Util.random() * portals.size).toInt()]
        when ((Util.random() * 5).toInt()) {
            0 -> capture(p)
            1 -> deployReso(p)
            2 -> destroyReso(p)
            3 -> makeLink(p)
            else -> makeField()
        }
    }

    // Deploy/upgrade a resonator → portal level rises with its loadout.
    private fun deployReso(p: Portal) {
        val slot = (Util.random() * 8).toInt()
        p.resoLevels[slot] = (p.resoLevels[slot] + 1 + (Util.random() * 2).toInt()).coerceIn(1, 8)
        p.targetLevel = derivedLevel(p)
        applyPortal(p)
        SoundUtil.playDeploySound(titlePos(p), 4)
    }

    // An XMP knocks a resonator out → portal level drops; the portal retaliates with a bolt.
    private fun destroyReso(p: Portal) {
        val filled = (0 until 8).filter { p.resoLevels[it] > 0 }
        if (filled.isEmpty()) return
        p.resoLevels[filled[(Util.random() * filled.size).toInt()]] = 0
        p.targetLevel = derivedLevel(p)
        applyPortal(p)
        SoundUtil.playXmpSound(titlePos(p), 4)
        spawnXmp(orbWorld(p), p.color)
        emitBolt(orbWorld(p), upFrom(p), p.color)
    }

    // An expanding additive sphere — the XMP burst flash at a portal.
    private fun spawnXmp(at: DoubleArray, color: String) {
        val mat = basicAdditive(color, 0.7)
        val mesh = Three.Mesh(Three.SphereGeometry(1.0, 16, 12), mat)
        mesh.asDynamic().position.set(at[0], at[1], at[2])
        scene.add(mesh)
        xmps.add(Xmp(mesh, mat, 0.0))
    }

    private fun updateXmps(dt: Double) {
        val it = xmps.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.age += dt
            val f = (e.age / XMP_LIFE).coerceIn(0.0, 1.0)
            val r = XMP_MAX_R * (1.0 - (1.0 - f) * (1.0 - f)) // ease-out expansion
            e.mesh.asDynamic().scale.set(r, r, r)
            e.mat.opacity = (1.0 - f) * 0.7
            if (f >= 1.0) {
                scene.remove(e.mesh)
                it.remove()
            }
        }
    }

    private fun titlePos(p: Portal) = Pos(Sim.width / 2.0 + p.pos[0], Sim.height / 2.0)

    private fun capture(p: Portal) {
        p.color = when ((Util.random() * 3).toInt()) {
            0 -> ENL_COLOR
            1 -> RES_COLOR
            else -> NEUTRAL_COLOR
        }
        p.orbMat = titleGlass(p.color) // re-skin the orb to the new faction's glass
        p.orb.asDynamic().material = p.orbMat
        randomLoadout(p) // a captured portal gets a fresh reso loadout
        p.flash = 1.0 // scale pop
        applyPortal(p)
        SoundUtil.playGlassShatterSound(titlePos(p), 0.22) // the orb re-skins
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
        SoundUtil.playDeploySound(titlePos(from), 4)
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
        SoundUtil.playFieldDownSound()
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
        if (portals.isEmpty()) {
            spawnSkyBolt() // portal setup failed/empty — keep the storm alive with sky bolts
            return
        }
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

    // Fallback when there are no portals: a bolt arcing top→bottom across the storm.
    private fun spawnSkyBolt() {
        val sx = rand(-22.0, 22.0)
        val a = doubleArrayOf(sx, rand(6.0, 16.0), rand(-4.0, 4.0))
        val b = doubleArrayOf(sx + rand(-10.0, 10.0), rand(-16.0, -2.0), rand(-4.0, 4.0))
        emitBolt(a, b, boltColor(if (Util.random() < 0.5) ENL_COLOR else RES_COLOR))
    }

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
        val poleH = POLE_H * poleScale(p.level)
        val orbY = (poleH + TOP_R * orbScale(p.level)) * TITLE_SCALE // group-local height → world (scaled)
        return doubleArrayOf(p.pos[0], p.pos[1] + orbY, p.pos[2])
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
