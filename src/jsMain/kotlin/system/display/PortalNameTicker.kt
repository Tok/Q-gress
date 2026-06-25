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
 * Latin names spin **CW**. RTL scripts (Arabic / Hebrew) are **suppressed for now** — Coda can't draw them
 * (no shaping / glyphs); once an RTL-capable font is added they render and spin **CCW** (see [isRtl]).
 *
 * Selection-driven ([show]/[hide] from `Scene3D.refreshNameTicker`, off the `Scene3D.selected` setter +
 * each `sync`). The title never selects portals, so no names appear there. One portal at a time; while the
 * same portal stays selected the ring is kept (just re-positioned as it levels up) and keeps spinning.
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
    private val SPACE_W = SIZE * 0.5 // blank advance for a space or a glyph the (latin) Coda font lacks

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
        reqX = x
        reqY = y
        reqZ = z
        reqRadius = orbRadius
        if (id == currentId) { // same portal still selected → keep the ring, just track its position (level-ups)
            val g = group
            if (g != null && letters.isNotEmpty()) g.position.set(x, y, z)
            return
        }
        currentId = id
        // RTL scripts (Arabic/Hebrew) need shaping + a font Coda lacks, so show nothing for now (icebox: add
        // an RTL-capable typeface, then this becomes the CCW spin again).
        if (isRtl(name)) {
            reqName = ""
            needsBuild = false
            clearLetters()
            val g = group
            if (g != null) g.visible = false
            return
        }
        reqName = name
        spinSign = -1.0 // LTR → CW (RTL/CCW deferred with the font, see above)
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

    // One layout slot: a real glyph ([geo] set) or a blank advance ([geo] null — space / missing glyph).
    private class Slot(val geo: dynamic, val width: Double)

    // Lay the (truncated) name out around a horizontal circle, each letter upright + facing outward.
    private fun buildRing() {
        val g = group ?: return
        val f = font ?: return
        clearLetters()
        val text = if (reqName.length > NAME_MAX) reqName.take(NAME_MAX - 1) + "…" else reqName
        val slots = text.map { slotFor(f, it) }
        val totalL = slots.sumOf { it.width } + GAP * (slots.size - 1)
        val radius = maxOf(reqRadius + RADIUS_MARGIN, MIN_RADIUS, totalL / (2.0 * PI * MAX_ARC_FRAC))
        var cursor = 0.0
        for (slot in slots) {
            val centre = cursor + slot.width / 2.0 - totalL / 2.0 // arc-length centre, word centred at angle 0
            cursor += slot.width + GAP
            val geo = slot.geo ?: continue // blank advance (space / missing glyph)
            geo.rotateX(PI / 2.0) // stand the glyph upright: local Y (up) → world Z
            val angle = -spinSign * centre / radius // place against the spin so the word reads forwards, not mirrored
            val mesh = Three.Mesh(geo, fillMat)
            DamageNumberFx.addBoldWire(mesh, geo, wireMat)
            mesh.asDynamic().position.set(radius * cos(angle), radius * sin(angle), 0.0)
            mesh.asDynamic().rotation.set(0.0, 0.0, angle + PI / 2.0) // normal → radially outward (faces sideways)
            g.add(mesh)
            letters.add(mesh)
        }
        if (letters.isEmpty()) { // nothing renderable (e.g. a non-latin name the Coda font can't draw)
            g.visible = false
            return
        }
        g.position.set(reqX, reqY, reqZ)
        g.rotation.z = 0.0
        g.visible = true
    }

    // Build a slot for [ch]: whitespace and glyphs the font lacks (NaN/zero width) become blank advances.
    private fun slotFor(f: dynamic, ch: Char): Slot {
        if (ch.isWhitespace()) return Slot(null, SPACE_W)
        val geo = DamageNumberFx.glyphGeometry(f, ch.toString(), SIZE, DEPTH)
        val bb = geo.boundingBox
        val w = abs((bb.max.x as Double) - (bb.min.x as Double))
        if (!w.isFinite() || w <= 0.0) {
            geo.dispose() // missing glyph → empty geometry; drop it, advance a blank
            return Slot(null, SPACE_W)
        }
        return Slot(geo, w)
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
