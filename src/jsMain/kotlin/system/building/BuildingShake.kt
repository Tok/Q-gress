package system.building

import config.Config
import items.Combat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Per-building bob when an XMP/US goes off nearby: only buildings **within the XMP's blast range** of
 * the detonation shake (so a blast doesn't shudder the whole plane), each via MapLibre `feature-state`
 * added to the extrusion height/base (see MapController's building layer + inflate). Amplitude uses the same
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
    private const val BASE_AMP_M = 7.0 // peak bob (m) at point-blank (clamped to the building's own height)
    private const val REF_HEIGHT_M = 12.0 // taller than this → progressively less bob ("more mass")
    private const val BOB_MAX_FRAC = 0.5 // never bob more than this × the building's height (no sinking)
    private const val RANGE_MULT = 1.0 // shake reaches the XMP's real blast range (cosmetic; tune freely)
    private const val ULTRA_SHAKE_MULT = 3.0 // an Ultra-Strike rocks buildings WAY harder than an XMP
    private const val ULTRA_BOB_MAX_FRAC = 0.85 // …and is allowed to bob them much further (up to ~building height)
    private const val MAX_SHAKEN = 120 // safety cap on buildings animated at once

    private var map: dynamic = null
    private val active = mutableMapOf<String, Shake>() // feature id (as string) → live bob

    private class Shake(val rawId: dynamic, val end: Double, val amp: Double, val phase: Double)

    /** Bind to the base map that carries the building layer (from MapController.enable3D). */
    fun attach(baseMap: dynamic) {
        map = baseMap
    }

    /** An XMP (or [ultra]-strike) of [level], real blast [rangeM] m, at ground [lng]/[lat]; bob buildings
     *  within range — an ultra-strike rocks them far harder. */
    fun blast(lng: Double, lat: Double, rangeM: Int, level: Int, ultra: Boolean, now: Double) {
        val m = map ?: return
        if (m.getLayer(LAYER) == null) return // no building layer (demo style, or not added yet)
        // querySourceFeatures (NOT queryRenderedFeatures): in MapLibre 5 the rendered-feature query frequently
        // omits the generateId feature id, so feature-state can't address the building → it never shakes. The
        // SOURCE query reliably carries the id. Distance is real-world metres (zoom-independent) via the quintile falloff.
        val params: dynamic = js("({})")
        params.sourceLayer = SOURCE_LAYER
        val feats = m.querySourceFeatures(SOURCE, params) ?: return
        val n = (feats.length as? Int) ?: return
        val rangeRef = rangeM * RANGE_MULT
        val levelGain = 0.4 + 0.6 * (level.coerceIn(1, 8) / 8.0)
        val intensity = if (ultra) ULTRA_SHAKE_MULT else 1.0
        val clampFrac = if (ultra) ULTRA_BOB_MAX_FRAC else BOB_MAX_FRAC
        var added = 0
        var i = 0
        while (i < n && added < MAX_SHAKEN) {
            val f = feats[i]
            i++
            val id = f.id
            val ll = firstLngLat(f.geometry)
            // Need a (generateId) feature id to drive feature-state, and an in-range footprint (falloff > 0).
            if (id != null && ll != null) {
                val falloff = Combat.rangeFalloff(metresBetween(lng, lat, ll[0] as Double, ll[1] as Double) / rangeRef)
                val renderHeight = (f.properties?.render_height as? Double) ?: 8.0
                val mass = REF_HEIGHT_M / maxOf(renderHeight, REF_HEIGHT_M) // taller → smaller bob
                val amp = minOf(
                    BASE_AMP_M * levelGain * falloff * mass * intensity * Config.buildingShakeMultiplier,
                    renderHeight * clampFrac,
                )
                if (amp > 0.0) { // in range → bob (keyed by id, so the same building isn't doubled)
                    active["$id"] = Shake(id, now + DURATION, amp, ("$id".hashCode() % 628) / 100.0)
                    added++
                }
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

    // Approx real-world metres between two lng/lat points (equirectangular — plenty accurate at building scale).
    private fun metresBetween(lng1: Double, lat1: Double, lng2: Double, lat2: Double): Double {
        val mPerDeg = 111_320.0
        val dx = (lng2 - lng1) * mPerDeg * cos(lat1 * PI / 180.0)
        val dy = (lat2 - lat1) * mPerDeg
        return hypot(dx, dy)
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
