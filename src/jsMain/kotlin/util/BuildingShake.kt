package util

import config.Config
import items.Combat
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Per-building bob when an XMP/US goes off nearby: only buildings **within the XMP's blast range** of
 * the detonation shake (so a blast doesn't shudder the whole plane), each via MapLibre `feature-state`
 * added to the extrusion height/base (see MapUtil's building layer + inflate). Amplitude uses the same
 * stepped-quintile range falloff as the weapon, is damped for **taller** buildings, clamped to a
 * fraction of each building's height (so a short one isn't bobbed into the ground), and scaled by the
 * menu's [Config.buildingShakeMultiplier]; [update] decays it back to rest.
 *
 * Caveat: feature-state needs the vector tiles to carry **feature ids** — buildings whose tiles don't
 * can't be addressed, so they won't shake. (The alternative, a whole-layer screen shudder, needs no ids
 * but moves *every* building, which reads as the whole plane shaking.) Works in-game + on the title; the
 * `/#demo` gray style has no buildings, so it no-ops.
 */
object BuildingShake {
    const val LAYER = "3d-buildings"
    private const val SOURCE = "openmaptiles"
    private const val SOURCE_LAYER = "building"

    /** Metre offset the building layer's height/base expressions add (from feature-state). */
    const val SHAKE_TERM = """["coalesce", ["feature-state", "shake"], 0]"""

    private const val DURATION = 2.0 // seconds to settle back to rest
    private const val FREQ = 12.0 // wobble speed (rad/s)
    private const val BASE_AMP_M = 5.0 // peak bob (m) at point-blank (clamped to the building's own height)
    private const val REF_HEIGHT_M = 12.0 // taller than this → progressively less bob ("more mass")
    private const val BOB_MAX_FRAC = 0.3 // never bob more than this × the building's height (no sinking)
    private const val RANGE_MULT = 1.0 // shake reaches the XMP's real blast range (cosmetic; tune freely)
    private const val ULTRA_SHAKE_MULT = 3.0 // an Ultra-Strike rocks buildings WAY harder than an XMP
    private const val ULTRA_BOB_MAX_FRAC = 0.85 // …and is allowed to bob them much further (up to ~building height)
    private const val MAX_SHAKEN = 120 // safety cap on buildings animated at once

    private var map: dynamic = null
    private val active = mutableMapOf<String, Shake>() // feature id (as string) → live bob

    private class Shake(val rawId: dynamic, val end: Double, val amp: Double, val phase: Double)

    /** Bind to the base map that carries the building layer (from MapUtil.enable3D). */
    fun attach(baseMap: dynamic) {
        map = baseMap
    }

    /** An XMP (or [ultra]-strike) of [level], real blast [rangeM] m, at ground [lng]/[lat]; bob buildings
     *  within range — an ultra-strike rocks them far harder. */
    fun blast(lng: Double, lat: Double, rangeM: Int, level: Int, ultra: Boolean, now: Double) {
        val m = map ?: return
        if (m.getLayer(LAYER) == null) return // no building layer (demo style, or not added yet)
        val origin: dynamic = js("[0.0, 0.0]")
        origin[0] = lng
        origin[1] = lat
        val pt = m.project(origin)
        val ox = pt.x as Double
        val oy = pt.y as Double
        val radiusPx = metersToPx(m, lng, lat, rangeM * RANGE_MULT)
        if (radiusPx <= 1.0) return
        val feats = queryNear(m, ox, oy, radiusPx) ?: return
        val levelGain = 0.4 + 0.6 * (level.coerceIn(1, 8) / 8.0)
        val intensity = if (ultra) ULTRA_SHAKE_MULT else 1.0
        val clampFrac = if (ultra) ULTRA_BOB_MAX_FRAC else BOB_MAX_FRAC
        val count = (feats.length as Int).coerceAtMost(MAX_SHAKEN)
        for (i in 0 until count) {
            val f = feats[i]
            val id = f.id
            if (id == null) continue // need a native feature id to drive feature-state
            val falloff = Combat.rangeFalloff(featureDistPx(m, f, ox, oy) / radiusPx) // quintile; 0 beyond range
            val renderHeight = (f.properties?.render_height as? Double) ?: 8.0
            val mass = REF_HEIGHT_M / maxOf(renderHeight, REF_HEIGHT_M) // taller → smaller bob
            val amp = minOf(
                BASE_AMP_M * levelGain * falloff * mass * intensity * Config.buildingShakeMultiplier,
                renderHeight * clampFrac,
            )
            if (amp > 0.0) { // out of range (falloff 0) → no bob
                val key = "$id"
                active[key] = Shake(id, now + DURATION, amp, (key.hashCode() % 628) / 100.0)
            }
        }
    }

    /** Advance every live bob (decaying wobble), writing feature-state; drop finished ones. */
    fun update(now: Double) {
        val m = map ?: return
        if (active.isEmpty()) return
        val done = mutableListOf<String>()
        active.forEach { (key, s) ->
            val remain = s.end - now
            if (remain <= 0.0) {
                setShake(m, s.rawId, 0.0)
                done.add(key)
            } else {
                val env = remain / DURATION // 1 → 0
                setShake(m, s.rawId, s.amp * env * env * sin(now * FREQ + s.phase))
            }
        }
        done.forEach { active.remove(it) }
    }

    // Screen-px distance from a feature (its first vertex) to the blast point — for the falloff.
    private fun featureDistPx(m: dynamic, f: dynamic, ox: Double, oy: Double): Double {
        val ll = firstLngLat(f.geometry) ?: return 0.0
        val p = m.project(ll)
        return hypot((p.x as Double) - ox, (p.y as Double) - oy)
    }

    // Convert a real-world [meters] length at [lng]/[lat] to screen px (so the radius tracks map zoom).
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

    private fun queryNear(m: dynamic, x: Double, y: Double, r: Double): dynamic {
        val box: dynamic = js("[[0.0, 0.0], [0.0, 0.0]]")
        box[0][0] = x - r
        box[0][1] = y - r
        box[1][0] = x + r
        box[1][1] = y + r
        val opts: dynamic = js("({})")
        opts.layers = arrayOf(LAYER)
        return m.queryRenderedFeatures(box, opts)
    }

    // Descend the GeoJSON coordinate nest until we reach a single [lng, lat] pair.
    private fun firstLngLat(geom: dynamic): dynamic {
        var c = geom?.coordinates ?: return null
        var guard = 0
        while (guard < 6 && isArray(c) && isArray(c[0])) {
            c = c[0]
            guard++
        }
        return c
    }

    private fun isArray(v: dynamic): Boolean = js("Array.isArray")(v) as Boolean

    private fun setShake(m: dynamic, rawId: dynamic, value: Double) {
        val key: dynamic = js("({})")
        key.source = SOURCE
        key.sourceLayer = SOURCE_LAYER
        key.id = rawId
        val state: dynamic = js("({})")
        state.shake = value
        m.setFeatureState(key, state)
    }
}
