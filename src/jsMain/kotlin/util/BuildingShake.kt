package util

import config.Config
import items.Combat
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Shakes the MapLibre 3D buildings (`3d-buildings` fill-extrusion layer) when an XMP/ultra-strike goes
 * off — a "the blast shook the city" effect, in-game + on the title (the `/#demo` gray style has no
 * buildings, so it no-ops).
 *
 * It shudders the **whole layer in screen space** via `fill-extrusion-translate`. We tried per-building
 * `feature-state` (so only nearby buildings bob), but the play-area vector tiles don't carry feature
 * ids — feature-state can't address them — so it only ever moved distant buildings. A global screen
 * shudder needs no ids and reliably moves every building. Intensity scales with **blast level** and the
 * blast's **proximity to the view centre**, then decays back to rest. Driven by [Scene3D.render]'s loop.
 */
object BuildingShake {
    const val LAYER = "3d-buildings"

    /** Harmless leftover so the building layer's height/base expressions still compile (feature-state
     *  shake is unused now — see the class note); it coalesces to 0, i.e. no effect. */
    const val SHAKE_TERM = """["coalesce", ["feature-state", "shake"], 0]"""

    private const val DURATION = 1.1 // seconds the shudder lasts
    private const val FREQ_X = 27.0 // fast horizontal shudder (rad/s)
    private const val FREQ_Y = 36.0 // …and a different vertical rate so it doesn't slide on one axis
    private const val BASE_AMP_PX = 7.0 // peak screen-px shudder at L8 with the blast at the view centre
    private const val EFFECT_RANGE_MULT = 2.0 // shake reaches 2× the XMP's real range (cosmetic, so we can be generous)

    private var map: dynamic = null
    private var end = -1.0 // sim-clock time the current shudder ends
    private var amp = 0.0 // current shudder amplitude (px)
    private var phase = 0.0 // varies per blast so repeats don't look identical
    private var applied = false // whether a non-zero translate is currently set (so we reset once)

    /** Bind to the base map that carries the building layer (from MapUtil.enable3D). */
    fun attach(baseMap: dynamic) {
        map = baseMap
    }

    /** An XMP of [level] (real blast [rangeM] m) detonated at ground [lng]/[lat]; shudder the building
     *  layer, **damped by how far the blast is from the view centre** (quintile falloff over 2× the XMP
     *  range) so distant XMPs no longer shake the whole plane. [now] = seconds. */
    fun blast(lng: Double, lat: Double, rangeM: Int, level: Int, now: Double) {
        val m = map ?: return
        if (m.getLayer(LAYER) == null) return // no building layer (demo style, or not added yet)
        val effectPx = metersToPx(m, lng, lat, EFFECT_RANGE_MULT * rangeM)
        if (effectPx <= 1.0) return
        // Same stepped-quintile curve as the weapon damage, but over the doubled cosmetic range.
        val falloff = Combat.rangeFalloff(viewDistancePx(m, lng, lat) / effectPx)
        if (falloff <= 0.0) return // blast too far from the view → don't shudder
        val levelGain = 0.4 + 0.6 * (level.coerceIn(1, 8) / 8.0)
        val a = BASE_AMP_PX * levelGain * falloff * Config.buildingShakeMultiplier
        amp = if (now < end) maxOf(amp, a) else a // overlapping blasts keep the strongest
        end = now + DURATION
        phase = (lng - lat) % 6.28
    }

    /** Animate the decaying shudder each frame (set the layer translate); reset to rest when it ends. */
    fun update(now: Double) {
        val m = map ?: return
        if (m.getLayer(LAYER) == null) return
        if (now >= end) {
            if (applied) {
                setTranslate(m, 0.0, 0.0)
                applied = false
            }
            return
        }
        val env = (end - now) / DURATION // 1 → 0
        val a = amp * env * env // ease-out so it settles smoothly
        setTranslate(m, a * sin(now * FREQ_X + phase), a * sin(now * FREQ_Y + phase + 1.7))
        applied = true
    }

    // Screen-px distance from the blast to the view centre (the falloff is measured against this).
    private fun viewDistancePx(m: dynamic, lng: Double, lat: Double): Double {
        val ll: dynamic = js("[0.0, 0.0]")
        ll[0] = lng
        ll[1] = lat
        val p = m.project(ll)
        val canvas = m.getCanvas()
        return hypot((p.x as Double) - canvas.clientWidth as Double / 2.0, (p.y as Double) - canvas.clientHeight as Double / 2.0)
    }

    // Convert a real-world [meters] length at [lng]/[lat] to screen px (project the point + one [meters] north).
    private fun metersToPx(m: dynamic, lng: Double, lat: Double, meters: Double): Double {
        val a: dynamic = js("[0.0, 0.0]")
        a[0] = lng
        a[1] = lat
        val b: dynamic = js("[0.0, 0.0]")
        b[0] = lng
        b[1] = lat + meters / 111_320.0
        val pa = m.project(a)
        val pb = m.project(b)
        return hypot((pa.x as Double) - (pb.x as Double), (pa.y as Double) - (pb.y as Double))
    }

    private fun setTranslate(m: dynamic, x: Double, y: Double) {
        val t: dynamic = js("[0.0, 0.0]")
        t[0] = x
        t[1] = y
        m.setPaintProperty(LAYER, "fill-extrusion-translate", t)
    }
}
