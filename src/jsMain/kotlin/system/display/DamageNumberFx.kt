package system.display

import external.Cannon
import external.FontLoader
import external.TextGeometry
import external.Three
import util.SoundUtil
import util.Util
import util.data.Pos
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Floating 3D **damage numbers** (extruded Coda digits + a black wire outline). On a hit the number is
 * **lerped straight up** off the top of the portal (~[RISE_HEIGHT] m, in code — *not* shoved by the
 * blast), held upright mid-air for a beat, then its digits **detach and fall individually** as cannon-es
 * rigid bodies — released right-to-left a few ms apart — to the ground before fading out. While falling,
 * a nearby explosion ([applyBlast]) flings the loose digits. Colour runs yellow→orange→red by the amount.
 * Own physics world + ground plane (like [ShatterFx]). [register] once; [spawn] per hit; [update] per frame.
 */
object DamageNumberFx {
    private const val FONT_URL = "fonts/Coda-ExtraBold.typeface.json"
    private const val SIZE = 3.4 // glyph em size (world units)
    private const val DEPTH = 0.8 // extrude depth
    private const val GAP = 0.6 // gap between digits
    private const val MASS = 0.5
    private const val GRAVITY = 20.0
    private const val RISE_HEIGHT = 7.5 // how far the connected number lerps up off the portal top (m)
    private const val DIGIT_OPACITY = 0.82 // a touch of transparency (glassy), still clearly readable
    private const val RISE_DUR = 0.55 // seconds for the rise
    private const val HANG_DUR = 1.4 // seconds it hangs upright before the digits drop
    private const val STAGGER = 0.1 // delay between digit releases (right-most first)
    private const val FALL_LIFE = 2.4 // seconds a digit lives after release
    private const val FADE = 0.7 // fade-out at the very end of a number's life
    private const val LAND_Z = 3.0 // a digit is "landed" once it drops to about here (ground ≈ 0)
    private const val LAND_VOLUME = 0.25 // soft glassy clink when the digits hit the ground

    // Volley damage spans orders of magnitude (a few hundred when heavily mitigated → tens of thousands
    // on a fresh portal), so the colour uses a LOG scale between these, not linear.
    private const val YELLOW_AT = 500.0 // ≤ this reads pure yellow
    private const val RED_AT = 70000.0 // ≥ this reads pure red
    private const val MAX_ACTIVE = 24 // cap concurrent numbers (drop the oldest beyond this)

    // Nearby-blast shove on already-falling digits (shared law with ShatterFx via BlastModel).
    private const val BLAST_SPEED = 16.0
    private const val BLAST_REF = 80.0
    private const val BLAST_FLOOR = 0.4
    private const val BLAST_UP = 0.5

    private var group: dynamic = null
    private var world: Cannon.World? = null
    private var font: dynamic = null

    private class Digit(val mesh: dynamic, val localX: Double, val hw: Double, val hh: Double, val release: Double) {
        var body: Cannon.Body? = null
        var landed = false // glassy clink plays once per digit, when it first hits the ground
    }

    private class DamageNum(
        val digits: List<Digit>,
        val origin: DoubleArray, // scene-space x, y, z (the flask top the number rises from)
        val loc: Pos, // sim position (for the positional landing sound)
        val fillMat: dynamic,
        val wireMat: dynamic,
        val totalLife: Double,
    ) {
        var age = 0.0
    }

    private val nums = mutableListOf<DamageNum>()

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

    fun hasActive() = nums.isNotEmpty()

    /** Add a static building box (scene-space centre + half-extents) so falling digits land on it. */
    fun addStaticBox(cx: Double, cy: Double, cz: Double, hx: Double, hy: Double, hz: Double) {
        val w = world ?: return
        val opts: dynamic = js("({ mass: 0 })")
        opts.position = Cannon.Vec3(cx, cy, cz)
        opts.shape = Cannon.Box(Cannon.Vec3(hx, hy, hz))
        w.addBody(Cannon.Body(opts))
    }

    /** Pop a damage number of [amount] above the portal top at scene-point ([x], [y], [z]); [loc] = the
     *  portal's sim position, for the positional landing clink. */
    fun spawn(x: Double, y: Double, z: Double, loc: Pos, amount: Int) {
        val f = font ?: return
        val g = group ?: return
        if (amount <= 0) return
        if (nums.size >= MAX_ACTIVE) drop(nums.first())
        val fillMat = fillMaterial(amount)
        val wireMat = wireMaterial()
        val digits = buildDigits(f, amount.toString(), fillMat, wireMat, g)
        val fallStart = RISE_DUR + HANG_DUR
        val maxRelease = fallStart + (digits.size - 1) * STAGGER
        nums.add(DamageNum(digits, doubleArrayOf(x, y, z), loc, fillMat, wireMat, maxRelease + FALL_LIFE))
    }

    /** A nearby explosion flings any already-falling digits (cloud-centre [origin], scene metres). */
    fun applyBlast(origin: DoubleArray, level: Int) {
        nums.forEach { num ->
            num.digits.forEach { d ->
                val b = d.body ?: return@forEach
                val p = b.asDynamic().position
                val imp = BlastModel.blastImpulse(
                    origin,
                    doubleArrayOf(p.x as Double, p.y as Double, p.z as Double),
                    level,
                    BLAST_SPEED,
                    BLAST_REF,
                    BLAST_FLOOR,
                )
                val v = b.asDynamic().velocity
                v.set((v.x as Double) + imp[0], (v.y as Double) + imp[1], (v.z as Double) + imp[2] * BLAST_UP)
            }
        }
    }

    /** Advance each number: lerp up, hang, release digits to physics (right→left), fade, drop finished. */
    fun update(dt: Double) {
        val w = world ?: return
        if (nums.isEmpty()) return
        w.step(1.0 / 60.0, dt, 3)
        val eye = GlassShader.eye() // the only observer is the camera → billboard every digit toward it
        val dead = mutableListOf<DamageNum>()
        nums.forEach { num ->
            num.age += dt
            val connectedZ = num.origin[2] + RISE_HEIGHT * easeOut(min(1.0, num.age / RISE_DUR))
            num.digits.forEach { d -> advanceDigit(num, d, connectedZ, eye) }
            if (num.age > num.totalLife - FADE) {
                val a = ((num.totalLife - num.age) / FADE).coerceIn(0.0, 1.0)
                num.fillMat.opacity = DIGIT_OPACITY * a
                num.wireMat.opacity = a
            }
            if (num.age >= num.totalLife) dead.add(num)
        }
        dead.forEach { drop(it) }
    }

    private fun advanceDigit(num: DamageNum, d: Digit, connectedZ: Double, eye: DoubleArray) {
        if (d.body == null && num.age >= d.release) { // detach this digit into a falling rigid body
            d.body = spawnBody(d, num.origin[0] + d.localX, num.origin[1], connectedZ).also { world?.addBody(it) }
        }
        val body = d.body
        val px: Double
        val py: Double
        val pz: Double
        if (body != null) { // physics drives POSITION (it falls / gets blast-flung); orientation is billboarded
            val bp = body.asDynamic().position
            px = bp.x as Double
            py = bp.y as Double
            pz = bp.z as Double
        } else { // still part of the connected, upright number lerping/hanging
            px = num.origin[0] + d.localX
            py = num.origin[1]
            pz = connectedZ
        }
        d.mesh.position.set(px, py, pz)
        // Stay FLAT over the portal (extrusion up); just yaw around Z so the number's base points at the
        // camera (reads upright from the angled view). Not a full face-the-camera tilt.
        var ax = px - eye[0]
        var ay = py - eye[1]
        val len = sqrt(ax * ax + ay * ay)
        if (len > 1e-6) {
            ax /= len
            ay /= len
        } else {
            ax = 0.0
            ay = 1.0
        }
        d.mesh.rotation.set(0.0, 0.0, atan2(-ax, ay))
        if (body != null && pz <= LAND_Z && !d.landed) { // each digit clinks once as it hits the ground
            d.landed = true
            SoundUtil.playGlassShatterSound(num.loc, 0.0, LAND_VOLUME)
        }
    }

    private fun buildDigits(f: dynamic, text: String, fillMat: dynamic, wireMat: dynamic, g: dynamic): List<Digit> {
        val geos = text.map { glyphGeometry(f, it.toString()) }
        val widths = geos.map { abs((it.boundingBox.max.x as Double) - (it.boundingBox.min.x as Double)) } // mirrored → abs
        val total = widths.sum() + GAP * (geos.size - 1)
        var cursor = -total / 2.0
        return geos.mapIndexed { i, geo ->
            val w = widths[i]
            val localX = cursor + w / 2.0
            cursor += w + GAP
            val bb = geo.boundingBox
            val hh = abs((bb.max.y as Double) - (bb.min.y as Double)) / 2.0
            val mesh = Three.Mesh(geo, fillMat)
            val wire = Three.LineSegments(Three.EdgesGeometry(geo), wireMat) // black outline
            wire.asDynamic().scale.set(1.03, 1.03, 1.03) // nudge out so the edges don't z-fight the fill
            mesh.asDynamic().add(wire)
            g.add(mesh)
            val release = RISE_DUR + HANG_DUR + (text.length - 1 - i) * STAGGER // right-most drops first
            Digit(mesh, localX, max(0.3, w / 2.0), max(0.3, hh), release)
        }
    }

    private fun glyphGeometry(f: dynamic, ch: String): dynamic {
        val params: dynamic = js("({})")
        params.font = f
        params.size = SIZE
        params.depth = DEPTH
        params.height = DEPTH
        params.curveSegments = 3
        params.bevelEnabled = true
        params.bevelThickness = 0.08
        params.bevelSize = 0.06
        params.bevelSegments = 1
        val geo = TextGeometry(ch, params)
        geo.computeBoundingBox()
        val bb = geo.boundingBox
        geo.asDynamic().translate(
            -((bb.min.x as Double) + (bb.max.x as Double)) / 2.0,
            -((bb.min.y as Double) + (bb.max.y as Double)) / 2.0,
            -DEPTH / 2.0,
        )
        // Mirror on X to counter the scene's -Y model-matrix flip, else the glyphs render reflected.
        geo.asDynamic().scale(-1.0, 1.0, 1.0)
        geo.computeBoundingBox() // refresh after centring + mirror (used for width/height)
        return geo
    }

    private fun spawnBody(d: Digit, x: Double, y: Double, z: Double): Cannon.Body {
        val opts: dynamic = js("({})")
        opts.mass = MASS
        opts.position = Cannon.Vec3(x, y, z)
        opts.shape = Cannon.Box(Cannon.Vec3(d.hw, d.hh, DEPTH / 2.0))
        opts.linearDamping = 0.05
        opts.angularDamping = 0.4
        val body = Cannon.Body(opts)
        body.asDynamic().velocity.set((Util.random() - 0.5) * 1.5, (Util.random() - 0.5) * 1.5, Util.random() * 1.5)
        body.asDynamic().angularVelocity.set((Util.random() - 0.5) * 2.0, (Util.random() - 0.5) * 2.0, (Util.random() - 0.5) * 2.0)
        return body
    }

    private fun fillMaterial(amount: Int): dynamic {
        val p: dynamic = js("({})")
        p.color = colorFor(amount)
        p.transparent = true
        p.opacity = DIGIT_OPACITY // slightly glassy
        p.depthWrite = true // self-occlude correctly — transparent defaults this off → we'd see inside the letters
        p.side = 2 // DoubleSide — the X-mirror flips winding; depthWrite still sorts so it doesn't read inside-out
        return Three.MeshBasicMaterial(p)
    }

    private fun wireMaterial(): dynamic {
        val p: dynamic = js("({})")
        p.color = "#000000"
        p.transparent = true
        return Three.LineBasicMaterial(p)
    }

    private fun drop(num: DamageNum) {
        num.digits.forEach { d ->
            d.body?.let { world?.removeBody(it) }
            group?.remove(d.mesh)
            d.mesh.geometry.dispose()
        }
        num.fillMat.dispose()
        num.wireMat.dispose()
        nums.remove(num)
    }

    private fun easeOut(t: Double) = 1.0 - (1.0 - t) * (1.0 - t) * (1.0 - t)

    // Yellow (small) → orange → red (big), by damage amount on a LOG scale. Red channel full; G + B drop.
    private fun colorFor(amount: Int): String {
        val t = ((ln(max(1.0, amount.toDouble())) - ln(YELLOW_AT)) / (ln(RED_AT) - ln(YELLOW_AT))).coerceIn(0.0, 1.0)
        return "#ff" + hex((218 - 184 * t).toInt()) + hex((54 - 36 * t).toInt())
    }

    private fun hex(v: Int) = v.coerceIn(0, 255).toString(16).padStart(2, '0')
}
