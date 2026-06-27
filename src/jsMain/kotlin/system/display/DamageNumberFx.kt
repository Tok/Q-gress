package system.display

import external.Cannon
import external.FontLoader
import external.TextGeometry
import external.Three
import system.audio.SoundUtil
import system.display.shader.GlassShader
import util.Util
import util.data.Pos
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
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
    internal const val SIZE = 3.4 // glyph em size (world units) — shared with PortalNameTicker (same style)
    internal const val DEPTH = 0.8 // extrude depth
    internal const val GAP = 0.6 // gap between digits
    private const val MASS = 0.5
    private const val GRAVITY = 20.0
    private const val RISE_HEIGHT = 7.5 // how far the connected number lerps up off the portal top (m)
    internal const val DIGIT_OPACITY = 0.82 // a touch of transparency (glassy), still clearly readable
    internal const val WIRE_COPIES = 3 // WebGL ignores LineBasicMaterial.linewidth → concentric copies fake a bolder stroke
    internal const val WIRE_STEP = 0.03 // scale increment between copies (outward halo = apparent line thickness)
    private const val RISE_DUR = 0.55 // seconds for the rise
    private const val HANG_DUR = 1.4 // seconds it hangs upright before the digits drop
    private const val STAGGER = 0.1 // delay between digit releases (right-most first)
    private const val FALL_LIFE = 4.5 // seconds a digit lives (visible) after release, before it sinks away
    private const val SINK_DUR = 1.2 // then it no-clips down through the ground/buildings + despawns while invisible
    private const val RESTITUTION = 0.45 // digit bounciness on landing (ground / roofs / poles)
    private const val LAND_CLEARANCE = 3.0 // a digit is "landed" once it drops to about this far above the floor
    private const val LAND_VOLUME = 0.25 // soft glassy clink when the digits hit the ground
    private const val STACK_STEP = DEPTH * 3.0 // a new number presses earlier ones at the same portal up by this

    /** Menu toggle: damage-number animations on/off (on by default). */
    var enabled = true

    // Volley damage spans orders of magnitude (a few hundred when heavily mitigated → tens of thousands
    // on a fresh portal), so the colour uses a LOG scale between these, not linear.
    private const val YELLOW_AT = 500.0 // ≤ this reads pure yellow
    private const val RED_AT = 20000.0 // ≥ this reads deep dark red (lowered: the top end was rarely reached)
    private const val MAX_ACTIVE = 24 // cap concurrent numbers (drop the oldest beyond this)

    // Nearby-blast shove on already-falling digits (shared law with ShatterFx via BlastModel).
    private const val BLAST_SPEED = 16.0
    private const val BLAST_REF = 80.0
    private const val BLAST_FLOOR = 0.4
    private const val BLAST_UP = 0.5

    private var group: dynamic = null
    private var world: Cannon.World? = null
    private var font: dynamic = null
    private var groundBody: Cannon.Body? = null
    private var groundZ = 0.0 // the physics floor's elevation = terrain height under the play area (set after gen)

    /** Lift the physics floor to the terrain elevation [z] so digits land on the ground (not 884 m below
     *  it at sea level). Without this they'd fall straight through the visible terrain. */
    fun setGroundZ(z: Double) {
        groundZ = z
        val gb = groundBody ?: return
        gb.asDynamic().position.set(0.0, 0.0, z)
    }

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
        val totalLife: Double, // when the number starts sinking (it's then removed [SINK_DUR] later)
    ) {
        var age = 0.0
        var sinking = false // past totalLife: digits no-clip down through ground/buildings instead of fading
    }

    private val nums = mutableListOf<DamageNum>()

    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
        val w = Cannon.World()
        w.asDynamic().gravity.set(0.0, 0.0, -GRAVITY)
        w.asDynamic().defaultContactMaterial.restitution = RESTITUTION // digits bounce a little when they land
        val groundOpts: dynamic = js("({ mass: 0 })")
        groundOpts.shape = Cannon.Plane()
        val gb = Cannon.Body(groundOpts) // infinite floor, normal +Z; lifted to the terrain via setGroundZ
        w.addBody(gb)
        groundBody = gb
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

    private val poleBodies = mutableListOf<Cannon.Body>()

    /** Replace the portal-pole colliders so falling digits hit the poles instead of passing through.
     *  [specs] = [cx, cy, baseZ, topZ, radius] per pole; rebuilt each sync (poles grow with level / sink). */
    fun setPoleColliders(specs: Array<DoubleArray>) {
        val w = world ?: return
        poleBodies.forEach { w.removeBody(it) }
        poleBodies.clear()
        specs.forEach { s ->
            val h = s[3] - s[2]
            if (h > 0.5) {
                val opts: dynamic = js("({ mass: 0 })")
                opts.position = Cannon.Vec3(s[0], s[1], s[2] + h / 2.0)
                opts.shape = Cannon.Box(Cannon.Vec3(s[4], s[4], h / 2.0)) // square box ≈ the round pole
                Cannon.Body(opts).also {
                    w.addBody(it)
                    poleBodies.add(it)
                }
            }
        }
    }

    /** Pop a damage number of [amount] above the portal top at scene-point ([x], [y], [z]); [loc] = the
     *  portal's sim position, for the positional landing clink. */
    fun spawn(x: Double, y: Double, z: Double, loc: Pos, amount: Int) {
        if (!enabled) return
        val f = font ?: return
        val g = group ?: return
        if (amount <= 0) return
        if (nums.size >= MAX_ACTIVE) drop(nums.first())
        // Stack from below: press any numbers already over this portal up so a fresh hit doesn't clip them.
        nums.forEach { if (it.loc == loc) it.origin[2] += STACK_STEP }
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
        val eye = GlassShader.eye() // the only observer is the camera → yaw the WHOLE number toward it
        val dead = mutableListOf<DamageNum>()
        nums.forEach { num ->
            num.age += dt
            val connectedZ = num.origin[2] + RISE_HEIGHT * easeOut(min(1.0, num.age / RISE_DUR))
            val yaw = numberYaw(num, eye) // one shared yaw for the whole number (its base points at the camera)
            num.digits.forEach { d -> advanceDigit(num, d, connectedZ, yaw) }
            if (!num.sinking && num.age >= num.totalLife) startSinking(num) // no-clip down (no fade)
            if (num.age >= num.totalLife + SINK_DUR) dead.add(num) // by now they've sunk out of sight
        }
        dead.forEach { drop(it) }
    }

    // End of life: let each digit no-clip straight down through the ground (and any building/roof it's
    // resting on) under gravity, sinking out of sight — the terrain/buildings occlude it, so it just
    // disappears downward instead of dimming out. Removed [SINK_DUR] later once it's well below.
    private fun startSinking(num: DamageNum) {
        num.sinking = true
        num.digits.forEach { d ->
            val b = d.body
            if (b != null) b.asDynamic().collisionResponse = false
        }
    }

    private fun advanceDigit(num: DamageNum, d: Digit, connectedZ: Double, yaw: Double) {
        // The digit's spot in the connected, flat number — laid out along the number's yawed local X.
        val lx = num.origin[0] + cos(yaw) * d.localX
        val ly = num.origin[1] + sin(yaw) * d.localX
        if (d.body == null && num.age >= d.release) { // detach this digit into a falling rigid body
            d.body = spawnBody(d, lx, ly, connectedZ, yaw).also { world?.addBody(it) }
        }
        val body = d.body
        val px: Double
        val py: Double
        val pz: Double
        if (body != null) { // physics fully drives POSITION + ORIENTATION (it falls / tumbles / gets flung)
            val bp = body.asDynamic().position
            px = bp.x as Double
            py = bp.y as Double
            pz = bp.z as Double
            val q = body.asDynamic().quaternion
            d.mesh.quaternion.set(q.x as Double, q.y as Double, q.z as Double, q.w as Double)
        } else { // still part of the connected number lerping/hanging: flat, shared yaw
            px = lx
            py = ly
            pz = connectedZ
            d.mesh.rotation.set(0.0, 0.0, yaw)
        }
        d.mesh.position.set(px, py, pz)
        if (body != null && pz <= groundZ + LAND_CLEARANCE && !d.landed) { // each digit clinks once as it hits the ground
            d.landed = true
            SoundUtil.playGlassShatterSound(num.loc, 0.0, LAND_VOLUME)
        }
    }

    // One yaw for the whole number so its base points at the camera (flat over the portal, reads upright).
    private fun numberYaw(num: DamageNum, eye: DoubleArray): Double {
        val ax = num.origin[0] - eye[0]
        val ay = num.origin[1] - eye[1]
        val len = sqrt(ax * ax + ay * ay)
        // Pure yaw (a proper rotation — NO in-plane 180° and NO glyph mirror): turns the whole number's
        // up-axis to point away from the camera, so its base faces the camera and it reads in order.
        return if (len > 1e-6) atan2(-ax / len, ay / len) else 0.0
    }

    private fun buildDigits(f: dynamic, text: String, fillMat: dynamic, wireMat: dynamic, g: dynamic): List<Digit> {
        val geos = text.map { glyphGeometry(f, it.toString(), SIZE, DEPTH) }
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
            addBoldWire(mesh, geo, wireMat)
            g.add(mesh)
            val release = RISE_DUR + HANG_DUR + (text.length - 1 - i) * STAGGER // right-most drops first
            Digit(mesh, localX, max(0.3, w / 2.0), max(0.3, hh), release)
        }
    }

    // Black edge outline, ~3× bolder: concentric LineSegments copies (1.03/1.06/1.09×) form a thicker
    // black band around the glyph, since WebGL won't honour a fat linewidth. Copies share one EdgesGeometry.
    // Shared with PortalNameTicker (same outlined look).
    internal fun addBoldWire(mesh: dynamic, geo: dynamic, wireMat: dynamic) {
        val edges = Three.EdgesGeometry(geo)
        for (k in 1..WIRE_COPIES) {
            val wire = Three.LineSegments(edges, wireMat)
            val s = 1.0 + WIRE_STEP * k // nudge each copy out (also keeps edges off the fill → no z-fight)
            wire.asDynamic().scale.set(s, s, s)
            mesh.add(wire)
        }
    }

    // One centred, extruded glyph at [size]/[depth]. Shared with PortalNameTicker (same Coda style, φ-scaled).
    internal fun glyphGeometry(f: dynamic, ch: String, size: Double, depth: Double): dynamic {
        val params: dynamic = js("({})")
        params.font = f
        params.size = size
        params.depth = depth
        params.height = depth
        params.curveSegments = 3
        params.bevelEnabled = true
        params.bevelThickness = 0.08
        params.bevelSize = 0.06
        params.bevelSegments = 1
        val geo = TextGeometry(ch, params)
        // A space or a glyph the font lacks yields ZERO vertices → computeBoundingBox() returns NaN (and three
        // logs a warning), and translating by NaN poisons the geometry. Detect the empty case and hand back a
        // zero-extent bounding box instead, which callers read as a blank advance (width 0).
        val vertexCount = (geo.asDynamic().attributes?.position?.count as? Int) ?: 0
        if (vertexCount == 0) {
            geo.asDynamic().boundingBox = js("({ min: { x: 0, y: 0, z: 0 }, max: { x: 0, y: 0, z: 0 } })")
            return geo
        }
        geo.computeBoundingBox()
        val bb = geo.boundingBox
        geo.asDynamic().translate(
            -((bb.min.x as Double) + (bb.max.x as Double)) / 2.0,
            -((bb.min.y as Double) + (bb.max.y as Double)) / 2.0,
            -depth / 2.0,
        )
        geo.computeBoundingBox() // refresh after centring (used for width/height)
        return geo
    }

    private fun spawnBody(d: Digit, x: Double, y: Double, z: Double, yaw: Double): Cannon.Body {
        val opts: dynamic = js("({})")
        opts.mass = MASS
        opts.position = Cannon.Vec3(x, y, z)
        opts.shape = Cannon.Box(Cannon.Vec3(d.hw, d.hh, DEPTH / 2.0))
        opts.linearDamping = 0.05
        opts.angularDamping = 0.15 // low → the digit tumbles freely as it falls
        val body = Cannon.Body(opts)
        body.asDynamic().quaternion.setFromEuler(0.0, 0.0, yaw) // detach at the connected orientation (no pop)
        body.asDynamic().velocity.set((Util.random() - 0.5) * 1.5, (Util.random() - 0.5) * 1.5, Util.random() * 1.5)
        body.asDynamic().angularVelocity.set((Util.random() - 0.5) * 3.0, (Util.random() - 0.5) * 3.0, (Util.random() - 0.5) * 3.0)
        return body
    }

    private fun fillMaterial(amount: Int): dynamic {
        val p: dynamic = js("({})")
        p.color = colorFor(amount)
        p.transparent = true
        p.opacity = DIGIT_OPACITY // slightly glassy
        p.depthWrite = true // self-occlude correctly — transparent defaults this off → we'd see inside the letters
        p.side = 2 // DoubleSide — flat number readable from either side; depthWrite still sorts the relief
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

    // Yellow (small) → orange → DARK red (big), by damage amount on a LOG scale. All three channels ramp
    // so the top end lands on a deep #a50000 (not the old bright #ff2212) — a real "max damage" red.
    private fun colorFor(amount: Int): String {
        val t = ((ln(max(1.0, amount.toDouble())) - ln(YELLOW_AT)) / (ln(RED_AT) - ln(YELLOW_AT))).coerceIn(0.0, 1.0)
        return "#" + hex((255 - 90 * t).toInt()) + hex((218 - 218 * t).toInt()) + hex((54 - 54 * t).toInt())
    }

    private fun hex(v: Int) = v.coerceIn(0, 255).toString(16).padStart(2, '0')
}
