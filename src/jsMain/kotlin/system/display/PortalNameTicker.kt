package system.display

import external.FontLoader
import external.Three
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The **hovered** portal's name as a ring of extruded 3D letters that **circle the portal like a ticker**.
 * Same glyph style as the [DamageNumberFx] damage digits (shared Coda font + outlined glassy look, reused
 * via `DamageNumberFx.glyphGeometry`/`addBoldWire`) but **white** and **φ smaller**, sitting at the top of
 * the over-portal stack. Each letter stands upright and faces **radially outward** (sideways, not up), so
 * the word wraps around a horizontal circle above the orb; the whole ring spins continuously.
 *
 * **Spin direction follows the script**: Arabic / Hebrew names spin **CCW**, everything else **CW**.
 *
 * Hover-driven only ([show]/[hide] from `Scene3D.setHoveredPortal`, wired off the in-game map `mousemove`).
 * The title never wires hover, so no names appear there. One portal at a time; [show] is a no-op while the
 * same portal stays hovered (the ring just keeps spinning).
 */
object PortalNameTicker {
    private const val FONT_URL = "fonts/Coda-ExtraBold.typeface.json"
    private val PHI = (1.0 + sqrt(5.0)) / 2.0
    private val SIZE = DamageNumberFx.SIZE / PHI // φ smaller than the damage digits
    private val DEPTH = DamageNumberFx.DEPTH / PHI
    private val GAP = DamageNumberFx.GAP / PHI
    private val FILL_OPACITY = DamageNumberFx.DIGIT_OPACITY

    private const val RADIUS_MARGIN = 3.0 // ring radius = orb radius + this (letters circle just outside the orb)
    private const val MIN_RADIUS = 9.0 // …but never tighter than this
    private const val MAX_ARC_FRAC = 0.8 // a long name auto-widens the ring so it never wraps past this of the circle
    private const val SPIN_SPEED = 0.6 // ticker rotation (rad/s, sim-scaled)
    private const val NAME_MAX = 22 // truncate longer names (…)

    // Right-to-left scripts (→ spin CCW): Hebrew, Arabic + its supplements/presentation forms.
    private val RTL_RANGES = listOf(0x0590..0x05FF, 0x0600..0x06FF, 0x0750..0x077F, 0x08A0..0x08FF, 0xFB1D..0xFEFF)

    private var group: dynamic = null
    private var font: dynamic = null
    private var fillMat: dynamic = null
    private var wireMat: dynamic = null
    private val letters = mutableListOf<dynamic>()

    private var currentId: String? = null
    private var spinSign = -1.0 // CW for LTR (negative), CCW for RTL (positive)
    private var needsBuild = false
    private var reqName = ""
    private var reqX = 0.0
    private var reqY = 0.0
    private var reqZ = 0.0
    private var reqRadius = 0.0

    /** Register on a fresh scene (drops any prior world's state). */
    fun register(scene: Three.Scene) {
        fillMat?.dispose()
        wireMat?.dispose()
        letters.clear()
        currentId = null
        needsBuild = false
        group = Three.Group().also {
            scene.add(it)
            it.asDynamic().visible = false
        }
        fillMat = whiteFillMaterial()
        wireMat = blackWireMaterial()
        FontLoader().load(FONT_URL, { f -> font = f }) // async; a hover before it lands just waits in [update]
    }

    /** Show the ticker for portal [id] ([name]) centred at scene ([x], [y], [z]); [orbRadius] sizes the ring. */
    fun show(id: String, name: String, x: Double, y: Double, z: Double, orbRadius: Double) {
        if (id == currentId) return // same portal still hovered → keep spinning the existing ring
        currentId = id
        reqName = name
        reqX = x
        reqY = y
        reqZ = z
        reqRadius = orbRadius
        spinSign = if (isRtl(name)) 1.0 else -1.0
        needsBuild = true
    }

    /** Clear the ticker (no portal hovered). */
    fun hide() {
        if (currentId == null && !needsBuild) return
        currentId = null
        needsBuild = false
        clearLetters()
        val g = group
        if (g != null) g.visible = false
    }

    /** Per-frame: build the ring once the font is ready, then spin it. [dt] = sim-scaled seconds. */
    fun update(dt: Double) {
        val g = group ?: return
        if (needsBuild && font != null) {
            buildRing()
            needsBuild = false
        }
        if (currentId == null) return
        g.rotation.z += spinSign * SPIN_SPEED * dt
    }

    // Lay the (truncated) name out around a horizontal circle, each letter upright + facing outward.
    private fun buildRing() {
        val g = group ?: return
        val f = font ?: return
        clearLetters()
        val text = if (reqName.length > NAME_MAX) reqName.take(NAME_MAX - 1) + "…" else reqName
        if (text.isEmpty()) {
            g.visible = false
            return
        }
        val geos = text.map { DamageNumberFx.glyphGeometry(f, it.toString(), SIZE, DEPTH) }
        val widths = geos.map {
            val bb = it.boundingBox
            abs((bb.max.x as Double) - (bb.min.x as Double))
        }
        val totalL = widths.sum() + GAP * (widths.size - 1)
        val radius = maxOf(reqRadius + RADIUS_MARGIN, MIN_RADIUS, totalL / (2.0 * PI * MAX_ARC_FRAC))
        var cursor = 0.0
        geos.forEachIndexed { i, geo ->
            geo.rotateX(PI / 2.0) // stand the glyph upright: local Y (up) → world Z
            val centre = cursor + widths[i] / 2.0 - totalL / 2.0 // arc-length centre, word centred at angle 0
            cursor += widths[i] + GAP
            val angle = spinSign * centre / radius // placement dir tracks the spin so it reads under rotation
            val mesh = Three.Mesh(geo, fillMat)
            DamageNumberFx.addBoldWire(mesh, geo, wireMat)
            mesh.asDynamic().position.set(radius * cos(angle), radius * sin(angle), 0.0)
            mesh.asDynamic().rotation.set(0.0, 0.0, angle + PI / 2.0) // normal → radially outward (faces sideways)
            g.add(mesh)
            letters.add(mesh)
        }
        g.position.set(reqX, reqY, reqZ)
        g.rotation.z = 0.0
        g.visible = true
    }

    private fun clearLetters() {
        val g = group ?: return
        letters.forEach { m ->
            g.remove(m)
            m.geometry.dispose()
        }
        letters.clear()
    }

    private fun isRtl(s: String) = s.any { c -> RTL_RANGES.any { c.code in it } }

    private fun whiteFillMaterial(): dynamic {
        val p: dynamic = js("({})")
        p.color = "#ffffff"
        p.transparent = true
        p.opacity = FILL_OPACITY // glassy, like the damage digits
        p.depthWrite = true // self-occlude the relief correctly
        p.side = 2 // DoubleSide — readable as it spins around
        return Three.MeshBasicMaterial(p)
    }

    private fun blackWireMaterial(): dynamic {
        val p: dynamic = js("({})")
        p.color = "#000000"
        p.transparent = true
        return Three.LineBasicMaterial(p)
    }
}
