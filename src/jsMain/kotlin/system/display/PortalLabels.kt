package system.display

import external.Three
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Camera-facing **portal name + level** billboards floating above each orb. Built once per sim tick
 * ([beginSync] + [add] from [Scene3D.sync]) into our OWN group, then groomed every frame ([update]
 * from [Scene3D.render]) for the parts that track the moving camera:
 *
 *  - **Faction tint** — the name carries a faction-coloured halo (green/blue/neutral-grey) over a
 *    white fill with a dark contrast edge, so it reads as ENL/RES yet stays legible.
 *  - **Legible against buildings** — `depthTest = false`, so labels never hide behind 3D buildings.
 *  - **LOD / fade + cull** — opacity ramps down from [FADE_NEAR] to [FADE_FAR] (scene metres from the
 *    camera) and the label is dropped entirely past [FADE_FAR] or when behind the camera.
 *  - **De-clutter** — when labels crowd in screen space the higher-level (then nearer) portal wins;
 *    its neighbours within [DECLUTTER_PX] are hidden, so a dense cluster shows only its standouts.
 *
 * Lives in its own group (not `indicatorsGroup`, which `sync` clears *after* portals build — that's why
 * the old in-Scene3D labels never showed). Textures are cached by name|level|colour; the per-sprite
 * material is rebuilt each tick so fade/cull can drive each label's opacity independently.
 */
object PortalLabels {
    private const val LABEL_W = 22.0 // billboard width (scene metres); height follows the canvas aspect
    private const val CANVAS_W = 256 // label texture resolution (kept crisp)
    private const val CANVAS_H = 96

    // Distance fade (scene metres from the camera eye): full opacity up close, gone in the distance.
    private const val FADE_NEAR = 350.0 // ≤ this → full opacity
    private const val FADE_FAR = 1400.0 // ≥ this → culled (and opacity hits 0 just before)
    private const val MIN_OPACITY = 0.05 // below this just cull (nothing useful left to draw)

    private const val DECLUTTER_PX = 64.0 // hide a label within this screen box of a higher-priority one
    private const val OFF_SCREEN_NDC = 1.2 // cull once the anchor is this far outside the [-1,1] view box

    private var group: dynamic = null
    private val entries = mutableListOf<Label>()
    private val texCache = mutableMapOf<String, dynamic>() // CanvasTexture by "name|level|colour" (drawn once)

    // One label: its sprite + scene anchor + importance, plus per-frame scratch (screen pos / fade).
    private class Label(val sprite: dynamic, val x: Double, val y: Double, val z: Double, val level: Int) {
        var dist = 0.0
        var sx = 0.0
        var sy = 0.0
        var fade = 0.0
    }

    /** Register on a fresh scene (drops any state from a previous world). */
    fun register(scene: Three.Scene) {
        texCache.values.forEach { it.dispose() }
        texCache.clear()
        entries.clear()
        group = Three.Group().also { scene.add(it) }
    }

    /** Start a tick's rebuild: drop last tick's sprites (textures stay cached). */
    fun beginSync() {
        val g = group ?: return
        entries.forEach { it.sprite.material.dispose() }
        g.clear()
        entries.clear()
    }

    /** Add one portal's label at scene point ([x], [y], [z]); [color] is the faction hue (or neutral grey). */
    fun add(name: String, level: Int, x: Double, y: Double, z: Double, color: String) {
        val g = group ?: return
        val tex = texCache.getOrPut("$name|$level|$color") { Three.CanvasTexture(draw(name, level, color)) }
        val p: dynamic = js("({})")
        p.map = tex
        p.depthTest = false // always readable, even in front of buildings
        p.transparent = true
        p.opacity = 1.0
        val sprite = Three.Sprite(Three.SpriteMaterial(p)) // own material → per-label fade/cull
        sprite.asDynamic().position.set(x, y, z)
        sprite.asDynamic().scale.set(LABEL_W, LABEL_W * CANVAS_H / CANVAS_W, 1.0)
        g.add(sprite)
        entries.add(Label(sprite, x, y, z, level))
    }

    /** Per-frame grooming: distance fade + cull + screen-space de-clutter. [proj] = scene→clip matrix,
     *  [eye] = camera position in scene metres, [vw]/[vh] = canvas pixels. */
    fun update(proj: dynamic, eye: DoubleArray, vw: Double, vh: Double) {
        if (entries.isEmpty()) return
        val e = proj.elements // column-major: element(row r, col c) = e[c * 4 + r]
        val visible = mutableListOf<Label>()
        for (lb in entries) {
            lb.dist = sqrt(sq(lb.x - eye[0]) + sq(lb.y - eye[1]) + sq(lb.z - eye[2]))
            lb.fade = fadeFor(lb.dist)
            if (lb.fade < MIN_OPACITY || !project(lb, e, vw, vh)) {
                lb.sprite.visible = false
                continue
            }
            visible.add(lb)
        }
        declutter(visible)
    }

    /** Drop everything (teardown). */
    fun clear() {
        beginSync()
    }

    // Higher level wins; ties broken by nearer. Greedily keep, hiding any label too close on screen.
    private fun declutter(visible: MutableList<Label>) {
        visible.sortWith(compareByDescending<Label> { it.level }.thenBy { it.dist })
        val kept = mutableListOf<Label>()
        for (lb in visible) {
            val crowded = kept.any { abs(it.sx - lb.sx) < DECLUTTER_PX && abs(it.sy - lb.sy) < DECLUTTER_PX }
            if (crowded) {
                lb.sprite.visible = false
            } else {
                kept.add(lb)
                lb.sprite.visible = true
                lb.sprite.material.opacity = lb.fade
            }
        }
    }

    // Project the anchor to screen pixels (into lb.sx/lb.sy); false if behind the camera or off-screen.
    private fun project(lb: Label, e: dynamic, vw: Double, vh: Double): Boolean {
        val cw = num(e, 3) * lb.x + num(e, 7) * lb.y + num(e, 11) * lb.z + num(e, 15)
        if (cw <= 1e-6) return false // behind / on the camera plane
        val ndcX = (num(e, 0) * lb.x + num(e, 4) * lb.y + num(e, 8) * lb.z + num(e, 12)) / cw
        val ndcY = (num(e, 1) * lb.x + num(e, 5) * lb.y + num(e, 9) * lb.z + num(e, 13)) / cw
        if (offScreen(ndcX) || offScreen(ndcY)) return false
        lb.sx = (ndcX * 0.5 + 0.5) * vw
        lb.sy = (-ndcY * 0.5 + 0.5) * vh
        return true
    }

    private fun offScreen(ndc: Double) = ndc < -OFF_SCREEN_NDC || ndc > OFF_SCREEN_NDC

    private fun fadeFor(dist: Double): Double = when {
        dist <= FADE_NEAR -> 1.0
        dist >= FADE_FAR -> 0.0
        else -> 1.0 - (dist - FADE_NEAR) / (FADE_FAR - FADE_NEAR)
    }

    // Faction-tinted, dark-edged, white-filled text on a transparent canvas (drawn once per name|level|colour).
    private fun draw(name: String, level: Int, color: String): HTMLCanvasElement {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = CANVAS_W
        canvas.height = CANVAS_H
        val ctx = canvas.getContext("2d").asDynamic()
        ctx.textAlign = "center"
        ctx.textBaseline = "middle"
        ctx.lineJoin = "round"
        val cx = CANVAS_W / 2.0
        val shown = if (name.length > 18) name.take(17) + "…" else name
        fun line(text: String, y: Double, px: Int) {
            ctx.font = "600 ${px}px 'Chakra Petch', sans-serif"
            ctx.lineWidth = 6.0 // faction-coloured halo (the tint)
            ctx.strokeStyle = color
            ctx.strokeText(text, cx, y)
            ctx.lineWidth = 3.0 // dark contrast edge between halo and fill
            ctx.strokeStyle = "rgba(0, 0, 0, 0.85)"
            ctx.strokeText(text, cx, y)
            ctx.fillStyle = "#ffffff"
            ctx.fillText(text, cx, y)
        }
        line(shown, 30.0, 28)
        if (level > 0) line("L$level", 72.0, 34) // neutral portals (no resos) have no level
        return canvas
    }

    private fun num(e: dynamic, i: Int): Double = e[i] as Double

    private fun sq(v: Double) = v * v
}
