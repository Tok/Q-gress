package system.display

import external.FontLoader
import external.Three
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Every portal's name as a ring of extruded 3D letters that **circle the portal like a ticker**. Same glyph
 * style as the [DamageNumberFx] damage digits (shared Coda font + outlined glassy look, reused via
 * `DamageNumberFx.glyphGeometry`/`addBoldWire`) but **white** and **φ smaller**, sitting at the top of the
 * over-portal stack. Each letter stands upright and faces **radially outward** (sideways, not up), so the word
 * wraps a horizontal circle above the orb; the ring spins continuously.
 *
 * Driven by [sync] from `Scene3D.refreshNameTicker` (one [Ring] per portal, keyed by id; built once per name,
 * just re-positioned each frame as a portal levels up / the terrain resamples; removed when a portal is gone).
 * [enabled] gates the whole set — **off by default**, opt-in via the menu "Portal names" toggle (the title
 * keeps it off too). Latin names spin CW; RTL scripts (Arabic / Hebrew)
 * are suppressed for now — Coda can't draw them — until an RTL-capable font is added (see [isRtl]).
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

    // Right-to-left scripts (suppressed for now): Hebrew, Arabic + its supplements/presentation forms.
    private val RTL_RANGES = listOf(0x0590..0x05FF, 0x0600..0x06FF, 0x0750..0x077F, 0x08A0..0x08FF, 0xFB1D..0xFEFF)

    /** Master gate: OFF by default (opt-in via the menu "Portal names" toggle); the title keeps it off too. */
    var enabled = false
        private set

    private var scene: dynamic = null
    private var font: dynamic = null
    private var fillMat: dynamic = null
    private var wireMat: dynamic = null
    private val rings = mutableMapOf<String, Ring>()

    /** One portal's name ring: its own [group] (positioned + spun) and the laid-out letter meshes. */
    private class Ring(val group: dynamic) {
        var name = ""
        var orbRadius = 0.0
        var spinSign = -1.0 // CW for LTR (negative); RTL deferred
        var needsBuild = false
        val letters = mutableListOf<dynamic>()
    }

    /** What [sync] needs per portal: its id + name and where to centre the ring (scene metres). */
    class NameView(val id: String, val name: String, val x: Double, val y: Double, val z: Double, val orbRadius: Double)

    /** Register on a fresh scene (drops any prior world's rings). */
    fun register(scene: Three.Scene) {
        fillMat?.dispose()
        wireMat?.dispose()
        rings.clear() // the old rings' groups belonged to the previous scene
        this.scene = scene
        fillMat = whiteFillMaterial()
        wireMat = blackWireMaterial()
        FontLoader().load(FONT_URL, { f -> font = f }) // async; rings build in [update] once it lands
    }

    /** Menu toggle / title gate. Turning it off drops every ring immediately. */
    fun setEnabled(on: Boolean) {
        enabled = on
        if (!on) clearAll()
    }

    /**
     * Reconcile the visible rings with [views] (all portals to name): create/position rings for current
     * portals, (re)build any whose name changed, and drop rings for portals no longer present. A no-op clear
     * when [enabled] is off.
     */
    fun sync(views: List<NameView>) {
        val sc = scene ?: return
        if (!enabled) {
            clearAll()
            return
        }
        val ids = HashSet<String>()
        views.forEach { v ->
            ids.add(v.id)
            val ring = rings.getOrPut(v.id) {
                val g = Three.Group()
                g.asDynamic().visible = false
                sc.add(g)
                Ring(g)
            }
            ring.orbRadius = v.orbRadius
            ring.group.position.set(v.x, v.y, v.z)
            if (ring.name != v.name) { // new ring or a renamed portal → (re)build its letters
                ring.name = v.name
                ring.spinSign = -1.0
                ring.needsBuild = true
                clearLetters(ring)
            }
        }
        rings.keys.filter { it !in ids }.forEach { id -> rings.remove(id)?.let { disposeRing(it) } }
    }

    /** Per-frame: build any pending rings (once the font is ready), then spin them all. [dt] = sim-scaled s. */
    fun update(dt: Double) {
        val f = font
        if (f != null) rings.values.forEach { if (it.needsBuild) buildRing(it, f) }
        rings.values.forEach { it.group.rotation.z += it.spinSign * SPIN_SPEED * dt }
    }

    private fun clearAll() {
        rings.values.forEach { disposeRing(it) }
        rings.clear()
    }

    // One layout slot: a real glyph ([geo] set) or a blank advance ([geo] null — space / missing glyph).
    private class Slot(val geo: dynamic, val width: Double)

    // Lay a ring's (truncated) name out around a horizontal circle, each letter upright + facing outward.
    private fun buildRing(ring: Ring, f: dynamic) {
        ring.needsBuild = false
        clearLetters(ring)
        val g = ring.group
        if (isRtl(ring.name)) { // Coda can't shape RTL → show nothing for now (icebox: an RTL-capable font)
            g.visible = false
            return
        }
        val text = if (ring.name.length > NAME_MAX) ring.name.take(NAME_MAX - 1) + "…" else ring.name
        val slots = text.map { slotFor(f, it) }
        val totalL = slots.sumOf { it.width } + GAP * (slots.size - 1)
        val radius = maxOf(ring.orbRadius + RADIUS_MARGIN, MIN_RADIUS, totalL / (2.0 * PI * MAX_ARC_FRAC))
        var cursor = 0.0
        for (slot in slots) {
            val centre = cursor + slot.width / 2.0 - totalL / 2.0 // arc-length centre, word centred at angle 0
            cursor += slot.width + GAP
            val geo = slot.geo ?: continue // blank advance (space / missing glyph)
            geo.rotateX(PI / 2.0) // stand the glyph upright: local Y (up) → world Z
            val angle = -ring.spinSign * centre / radius // place against the spin so the word reads forwards
            val mesh = Three.Mesh(geo, fillMat)
            DamageNumberFx.addBoldWire(mesh, geo, wireMat)
            mesh.asDynamic().position.set(radius * cos(angle), radius * sin(angle), 0.0)
            mesh.asDynamic().rotation.set(0.0, 0.0, angle + PI / 2.0) // normal → radially outward (faces sideways)
            g.add(mesh)
            ring.letters.add(mesh)
        }
        g.visible = ring.letters.isNotEmpty()
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

    private fun clearLetters(ring: Ring) {
        ring.letters.forEach { m ->
            ring.group.remove(m)
            m.geometry.dispose()
        }
        ring.letters.clear()
    }

    private fun disposeRing(ring: Ring) {
        clearLetters(ring)
        scene?.remove(ring.group)
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
