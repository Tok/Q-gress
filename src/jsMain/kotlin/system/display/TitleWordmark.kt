package system.display

import external.FontLoader
import external.TextGeometry
import external.Three
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
    private const val STIFF = 70.0 // spring stiffness for the flash reaction
    private const val DAMP = 7.0 // spring damping (settles with a little overshoot)
    private const val IMPULSE = 9.0 // spin kick on an XMP flash

    private var group: dynamic = null
    private val letters = mutableListOf<dynamic>()
    private var loaded = false

    /** Load the font + build the letters into [scene]. [onReady] fires once they're in (e.g. to hide the DOM wordmark). */
    fun load(scene: dynamic, onReady: () -> Unit = {}) {
        if (loaded) return
        FontLoader().load(FONT_URL, { font ->
            build(scene, font)
            loaded = true
            onReady()
        })
    }

    private fun build(scene: dynamic, font: dynamic) {
        group = Three.Group()
        val capMat = standard("#f2f4f8", 0.35, 0.32, "#9aa0aa", 0.22) // lit off-white caps
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
            mesh.asDynamic().position.x = cursor + w / 2.0
            mesh.asDynamic().userData.angle = 0.0
            mesh.asDynamic().userData.vel = 0.0
            group.add(mesh) // group is already dynamic — no .asDynamic()
            letters.add(mesh)
            cursor += w + SPACING
        }
        val half = (cursor - SPACING) / 2.0
        letters.forEach { it.position.x = (it.position.x as Double) - half } // centre the word (it is dynamic)
        scene.add(group)
    }

    private fun standard(color: String, metalness: Double, roughness: Double, emissive: String, emissiveIntensity: Double): dynamic {
        val p: dynamic = js("({})")
        p.color = color
        p.metalness = metalness
        p.roughness = roughness
        p.emissive = emissive
        p.emissiveIntensity = emissiveIntensity
        return Three.MeshStandardMaterial(p)
    }

    /** Camera-lock the group in front of the camera + advance each letter's spring spin. */
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
        letters.forEach { letter ->
            // letter is dynamic — access directly, no .asDynamic()
            val angle = letter.userData.angle as Double
            val vel = letter.userData.vel as Double
            val nextVel = vel + (-STIFF * angle - DAMP * vel) * dt
            val nextAngle = angle + nextVel * dt
            letter.userData.vel = nextVel
            letter.userData.angle = nextAngle
            letter.rotation.y = nextAngle
        }
    }

    /** An XMP fired — kick every letter into a spin (randomised by index) that springs back to rest. */
    fun flash() {
        if (!loaded) return
        letters.forEachIndexed { i, letter ->
            // letter is dynamic — access directly
            val dir = if (i % 2 == 0) 1.0 else -1.0
            val jitter = 0.6 + (i % 3) * 0.3
            letter.userData.vel = (letter.userData.vel as Double) + IMPULSE * dir * jitter
        }
    }

    // --- vec3 helpers ---
    private fun cross(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])

    private fun add(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0] + b[0], a[1] + b[1], a[2] + b[2])
    private fun scale(a: DoubleArray, s: Double) = doubleArrayOf(a[0] * s, a[1] * s, a[2] * s)
    private fun norm(a: DoubleArray): DoubleArray {
        val l = sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
        return if (l < 1e-9) doubleArrayOf(0.0, 0.0, 1.0) else scale(a, 1.0 / l)
    }
}
