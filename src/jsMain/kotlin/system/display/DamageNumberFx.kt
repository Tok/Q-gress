package system.display

import external.Cannon
import external.FontLoader
import external.TextGeometry
import external.Three
import util.Util
import kotlin.math.max
import kotlin.math.min

/**
 * Floating 3D **damage numbers**: when an XMP/US damages a portal, the total pops up as extruded Coda
 * digits (same 3D treatment as the title wordmark), flies up off the portal, then — as a cannon-es
 * rigid body — arcs and falls back to the ground before fading out and being removed. Colour runs
 * **yellow (small) → orange → red (big)** by the damage amount (not the weapon level). Own physics
 * world + ground plane (like [ShatterFx]). [register] once; [spawn] per damage event; [update] per frame.
 */
object DamageNumberFx {
    private const val FONT_URL = "fonts/Coda-ExtraBold.typeface.json"
    private const val SIZE = 4.2 // glyph em size (world units)
    private const val DEPTH = 0.8 // extrude depth
    private const val MASS = 0.6
    private const val GRAVITY = 20.0 // ~2× g, matches ShatterFx so debris + numbers fall alike
    private const val UP_VEL = 12.0 // initial upward pop (m/s)
    private const val SPREAD = 3.0 // random horizontal velocity so they don't stack
    private const val SPIN = 1.6 // gentle tumble
    private const val LIFE = 3.2 // seconds before removal (long enough to pop up + fall back)
    private const val FADE = 0.8 // fade out over the final seconds
    private const val RED_AT = 4000.0 // damage at/above which the number reads fully red
    private const val MAX_ACTIVE = 40 // cap concurrent numbers (drop the oldest beyond this)

    private var group: dynamic = null
    private var world: Cannon.World? = null
    private var font: dynamic = null

    private class Num(val mesh: dynamic, val mat: dynamic, val body: Cannon.Body, var age: Double)

    private val active = mutableListOf<Num>()

    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
        val w = Cannon.World()
        w.asDynamic().gravity.set(0.0, 0.0, -GRAVITY)
        val groundOpts: dynamic = js("({ mass: 0 })")
        groundOpts.shape = Cannon.Plane()
        w.addBody(Cannon.Body(groundOpts))
        world = w
        FontLoader().load(FONT_URL, { f -> font = f }) // async; spawns before it lands just no-op
    }

    fun hasActive() = active.isNotEmpty()

    /** Pop a damage number of [amount] at scene-point ([x], [y], [z]); it flies up, falls, then fades. */
    fun spawn(x: Double, y: Double, z: Double, amount: Int) {
        val f = font ?: return
        val w = world ?: return
        val g = group ?: return
        if (amount <= 0) return
        if (active.size >= MAX_ACTIVE) remove(active.first())
        val geo = buildGeometry(f, amount)
        val mat = buildMaterial(amount)
        val mesh = Three.Mesh(geo, mat)
        g.add(mesh)
        w.addBody(spawnBody(geo, x, y, z).also { active.add(Num(mesh, mat, it, 0.0)) })
    }

    /** Step physics + sync every number's mesh to its body; fade + drop finished ones. */
    fun update(dt: Double) {
        val w = world ?: return
        if (active.isEmpty()) return
        w.step(1.0 / 60.0, dt, 3)
        val dead = mutableListOf<Num>()
        active.forEach { n ->
            n.age += dt
            val bp = n.body.asDynamic().position
            n.mesh.position.set(bp.x as Double, bp.y as Double, bp.z as Double)
            val bq = n.body.asDynamic().quaternion
            n.mesh.quaternion.set(bq.x as Double, bq.y as Double, bq.z as Double, bq.w as Double)
            if (n.age > LIFE - FADE) n.mat.opacity = ((LIFE - n.age) / FADE).coerceIn(0.0, 1.0)
            if (n.age >= LIFE) dead.add(n)
        }
        dead.forEach { remove(it) }
    }

    private fun buildGeometry(f: dynamic, amount: Int): dynamic {
        val params: dynamic = js("({})")
        params.font = f
        params.size = SIZE
        params.depth = DEPTH // three r150+
        params.height = DEPTH // older three
        params.curveSegments = 3
        params.bevelEnabled = true
        params.bevelThickness = 0.08
        params.bevelSize = 0.06
        params.bevelSegments = 1
        val geo = TextGeometry(amount.toString(), params)
        geo.computeBoundingBox()
        val bb = geo.boundingBox
        // Centre the glyphs on the body origin so it tumbles around its middle.
        geo.asDynamic().translate(
            -((bb.min.x as Double) + (bb.max.x as Double)) / 2.0,
            -((bb.min.y as Double) + (bb.max.y as Double)) / 2.0,
            -DEPTH / 2.0,
        )
        return geo
    }

    private fun buildMaterial(amount: Int): dynamic {
        val p: dynamic = js("({})")
        p.color = colorFor(amount)
        p.transparent = true
        p.depthTest = false // always readable, even behind buildings
        return Three.MeshBasicMaterial(p)
    }

    private fun spawnBody(geo: dynamic, x: Double, y: Double, z: Double): Cannon.Body {
        val bb = geo.boundingBox
        val hw = max(0.3, ((bb.max.x as Double) - (bb.min.x as Double)) / 2.0)
        val hh = max(0.3, ((bb.max.y as Double) - (bb.min.y as Double)) / 2.0)
        val opts: dynamic = js("({})")
        opts.mass = MASS
        opts.position = Cannon.Vec3(x, y, z)
        opts.shape = Cannon.Box(Cannon.Vec3(hw, hh, DEPTH / 2.0))
        opts.linearDamping = 0.05
        opts.angularDamping = 0.3
        val body = Cannon.Body(opts)
        body.asDynamic().velocity.set((Util.random() - 0.5) * SPREAD, (Util.random() - 0.5) * SPREAD, UP_VEL + Util.random() * 4.0)
        body.asDynamic().angularVelocity.set((Util.random() - 0.5) * SPIN, (Util.random() - 0.5) * SPIN, (Util.random() - 0.5) * SPIN)
        return body
    }

    private fun remove(n: Num) {
        world?.removeBody(n.body)
        group?.remove(n.mesh)
        n.mesh.geometry.dispose()
        n.mat.dispose()
        active.remove(n)
    }

    // Yellow (small) → orange → red (big), by damage amount. Red channel stays full; green + blue drop.
    private fun colorFor(amount: Int): String {
        val t = min(1.0, amount / RED_AT)
        val g = (218 - 184 * t).toInt() // 0xDA → ~0x22
        val b = (54 - 36 * t).toInt() // 0x36 → 0x12
        return "#ff" + hex(g) + hex(b)
    }

    private fun hex(v: Int) = v.coerceIn(0, 255).toString(16).padStart(2, '0')
}
