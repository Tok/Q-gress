package system.display

import external.FontLoader
import external.TextGeometry
import external.Three
import util.Util
import kotlin.math.sqrt

/**
 * The Q-GRESS title wordmark as **real 3D extruded letters** (brand font, Chakra Petch) floating in
 * the title scene. The letter group is **camera-locked** — re-placed in front of the camera every frame
 * from the recovered eye/forward/up — so it flies with the cinematic camera over the orbiting world.
 * Each letter springs (rotate-in-place → back) when an XMP fires nearby ([flash]), qlippostasis-style.
 * Title-only: [load] from TitleSim; Scene3D drives [update] + [flash] and no-ops until loaded.
 */
object TitleWordmark {
    private const val TEXT = "Q-GRESS"
    private const val FONT_URL = "fonts/ChakraPetch-Bold.typeface.json"
    private const val SIZE = 7.0 // glyph em size (world units)
    private const val DEPTH = 2.2 // extrude depth (the 3D "thickness" / black backline)
    private const val SPACING = 1.6 // gap between letters
    private const val DIST = 64.0 // how far in front of the camera the wordmark floats
    private const val Y_OFFSET = 9.0 // raise it into the upper part of the frame (above the action), but in view
    private const val POS_STIFF = 80.0 // position spring: displaced letters pull back to rest
    private const val POS_DAMP = 9.0 // spring damping (quick return, slight overshoot)
    private const val PUSH = 42.0 // blast shove velocity (away from the XMP)
    private const val PUSH_REF = 250.0 // distance falloff — nearer XMPs shove the letters harder
    private const val LEVEL_FLOOR = 0.3 // L1 still nudges the letters, L8 = full shove (BlastModel.levelGain)
    private const val JITTER = 16.0 // per-letter random velocity so they don't all move identically
    private const val TILT = 0.1 // letters tilt with their horizontal displacement (liveliness)

    private var group: dynamic = null
    private var wordScene: dynamic = null // the wordmark renders in its OWN scene + pass (see renderOverlay)
    private val letters = mutableListOf<dynamic>()
    private var loaded = false

    // Latest camera-locked frame (set in update) so flash() can project the XMP into the wordmark plane.
    private var lastPos = doubleArrayOf(0.0, 0.0, 0.0)
    private var lastX = doubleArrayOf(1.0, 0.0, 0.0)
    private var lastY = doubleArrayOf(0.0, 1.0, 0.0)

    /** Show/hide the 3D wordmark — popped out once a faction is picked so it doesn't sit under the onboarding pane. */
    fun setVisible(visible: Boolean) {
        val g = group ?: return
        g.visible = visible
    }

    /** Whether the 3D letters are already in the scene (so a return to the title can re-show them). */
    fun isLoaded() = loaded

    /** Load the font + build the letters into the wordmark's own scene. [onReady] fires once they're in. */
    fun load(onReady: () -> Unit = {}) {
        if (loaded) return
        FontLoader().load(FONT_URL, { font ->
            build(font)
            loaded = true
            onReady()
        })
    }

    /** Draw the wordmark over the whole frame with normal depth (so it self-occludes): clear the depth
     *  buffer, then render its own scene. Called by [Scene3D] right after the main scene render. */
    fun renderOverlay(renderer: dynamic, cam: dynamic) {
        val s = wordScene ?: return
        val g = group ?: return
        if (!loaded || g.visible != true) return
        renderer.clearDepth()
        renderer.render(s, cam)
    }

    private fun build(font: dynamic) {
        group = Three.Group()
        val capMat = standard("#ffffff", 0.0, 0.5, "#ffffff", 0.4) // bright white caps (diffuse + white emissive)
        val sideMat = standard("#0a0a0a", 0.55, 0.5, "#000000", 0.0) // near-black extruded sides (the backline)
        var cursor = 0.0
        TEXT.forEach { ch ->
            val params: dynamic = js("({})")
            params.font = font
            params.size = SIZE
            params.depth = DEPTH // three r150+
            params.height = DEPTH // older three
            params.curveSegments = 5
            params.bevelEnabled = true
            params.bevelThickness = 0.18
            params.bevelSize = 0.12
            params.bevelSegments = 2
            val geo = TextGeometry(ch.toString(), params)
            geo.computeBoundingBox()
            val bb = geo.boundingBox
            val minX = bb.min.x as Double
            val maxX = bb.max.x as Double
            val minY = bb.min.y as Double
            val maxY = bb.max.y as Double
            val w = maxX - minX
            geo.asDynamic().translate(-(minX + maxX) / 2.0, -(minY + maxY) / 2.0, -DEPTH / 2.0) // centre the glyph
            val mesh = Three.Mesh(geo, arrayOf(capMat, sideMat))
            mesh.asDynamic().renderOrder = 99999 // draw last → in front of everything (portals, links, fields, bars…)
            mesh.asDynamic().position.x = cursor + w / 2.0
            mesh.asDynamic().userData.ox = 0.0 // displacement offset (x, y) + velocity, for the blast spring
            mesh.asDynamic().userData.oy = 0.0
            mesh.asDynamic().userData.vx = 0.0
            mesh.asDynamic().userData.vy = 0.0
            group.add(mesh) // group is already dynamic — no .asDynamic()
            letters.add(mesh)
            cursor += w + SPACING
        }
        val half = (cursor - SPACING) / 2.0
        letters.forEach {
            // centre the word + remember each letter's rest x (it is dynamic)
            val restX = (it.position.x as Double) - half
            it.position.x = restX
            it.userData.baseX = restX
        }
        // Own scene + a flat bright fill: the white caps read full-white (diffuse + emissive), the
        // near-black extruded sides stay dark (the 3D "backline"); no directional light, so the look is
        // identical from any camera angle as the wordmark orbits with the cinematic.
        val s: dynamic = Three.Scene()
        s.add(Three.AmbientLight("#ffffff", 1.0))
        s.add(group)
        wordScene = s
    }

    private fun standard(color: String, metalness: Double, roughness: Double, emissive: String, emissiveIntensity: Double): dynamic {
        val p: dynamic = js("({})")
        p.color = color
        p.metalness = metalness
        p.roughness = roughness
        p.emissive = emissive
        p.emissiveIntensity = emissiveIntensity
        // Normal depth → the 3D letters self-occlude (no dark back/side faces bleeding through the white).
        // "In front of everything" is handled by rendering the wordmark in its own pass after a depth clear
        // (see [renderOverlay]), not by disabling the depth test.
        return Three.MeshStandardMaterial(p)
    }

    /** Camera-lock the group in front of the camera + advance each letter's displacement spring. */
    fun update(eye: DoubleArray, forward: DoubleArray, up: DoubleArray, dt: Double) {
        val g = group ?: return
        if (!loaded) return
        val f = norm(forward)
        val u = norm(up)
        val z = scale(f, -1.0) // text front (+Z) faces back toward the eye
        val x = norm(cross(u, z))
        val y = cross(z, x)
        val pos = add(add(eye, scale(f, DIST)), scale(u, Y_OFFSET))
        val m = Three.Matrix4()
        m.asDynamic().makeBasis(Three.Vector3(x[0], x[1], x[2]), Three.Vector3(y[0], y[1], y[2]), Three.Vector3(z[0], z[1], z[2]))
        g.quaternion.setFromRotationMatrix(m)
        g.position.set(pos[0], pos[1], pos[2])
        lastPos = pos
        lastX = x
        lastY = y
        letters.forEach { springLetter(it, dt) }
    }

    // Critically-ish-damped spring: pull each letter's (ox, oy) displacement back to rest; tilt with it.
    private fun springLetter(letter: dynamic, dt: Double) {
        val ud = letter.userData
        val ox = ud.ox as Double
        val oy = ud.oy as Double
        val nvx = (ud.vx as Double) + (-POS_STIFF * ox - POS_DAMP * (ud.vx as Double)) * dt
        val nvy = (ud.vy as Double) + (-POS_STIFF * oy - POS_DAMP * (ud.vy as Double)) * dt
        val nox = ox + nvx * dt
        val noy = oy + nvy * dt
        ud.vx = nvx
        ud.vy = nvy
        ud.ox = nox
        ud.oy = noy
        letter.position.x = (ud.baseX as Double) + nox
        letter.position.y = noy
        letter.rotation.z = -nox * TILT
    }

    /**
     * An XMP fired at [xmpWorld] (the mushroom-cloud centre, above the terrain) at [level]. Shove each
     * letter away from it in the wordmark plane — closer letters harder (per-letter falloff), bigger XMPs
     * harder (level) — then the spring lerps them back.
     */
    fun flash(xmpWorld: DoubleArray, level: Int) {
        if (!loaded) return
        letters.forEach { letter ->
            val ud = letter.userData
            val letterWorld = add(lastPos, scale(lastX, ud.baseX as Double)) // letter's rest world position
            // Shared blast law (level gain + 3D distance falloff, radial from the cloud centre)…
            val imp = BlastModel.blastImpulse(xmpWorld, letterWorld, level, PUSH, PUSH_REF, LEVEL_FLOOR)
            val strength = sqrt(dot(imp, imp)) // = PUSH · levelGain · falloff for this letter
            // …then projected into the wordmark plane (the letters only move in-plane).
            val px = dot(imp, lastX)
            val py = dot(imp, lastY)
            val plen = sqrt(px * px + py * py)
            val ux = if (plen > 1e-6) px / plen else 0.0
            val uy = if (plen > 1e-6) py / plen else 1.0
            ud.vx = (ud.vx as Double) + ux * strength + (Util.random() - 0.5) * JITTER
            ud.vy = (ud.vy as Double) + uy * strength + (Util.random() - 0.5) * JITTER
        }
    }

    // --- vec3 helpers ---
    private fun cross(a: DoubleArray, b: DoubleArray) = doubleArrayOf(
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    )

    private fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    private fun add(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0] + b[0], a[1] + b[1], a[2] + b[2])
    private fun scale(a: DoubleArray, s: Double) = doubleArrayOf(a[0] * s, a[1] * s, a[2] * s)
    private fun norm(a: DoubleArray): DoubleArray {
        val l = sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
        return if (l < 1e-9) doubleArrayOf(0.0, 0.0, 1.0) else scale(a, 1.0 / l)
    }
}
