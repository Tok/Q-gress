package util

import kotlin.math.hypot
import kotlin.math.sin

/**
 * Makes the MapLibre 3D buildings (`3d-buildings` fill-extrusion layer) **bob and settle** when an
 * XMP/ultra-strike goes off nearby — a global "the blast shook the city" mechanic that works in-game
 * and on the title (the `/#demo` sandbox uses the bare gray style with no buildings, so it no-ops).
 *
 * MapLibre owns the building geometry on the GPU, so we can't run a per-vertex deform shader. Instead
 * each shaken building gets a per-feature **`feature-state.shake`** (a vertical offset in metres) that
 * the layer's height/base expressions add (see [MapUtil] building layer + inflate); [update] decays it
 * back to 0 each frame so the building springs back. Amplitude scales with **blast level + proximity**
 * and is damped for **taller buildings** (more "mass"). Needs native vector-tile feature ids; if the
 * tiles lack them, [blast] simply finds nothing to shake (graceful no-op).
 */
object BuildingShake {
    const val LAYER = "3d-buildings"
    private const val SOURCE = "openmaptiles"
    private const val SOURCE_LAYER = "building"

    /** The expression fragment the building layer adds to height + base: a metre offset from feature-state. */
    const val SHAKE_TERM = """["coalesce", ["feature-state", "shake"], 0]"""

    private const val DURATION = 2.4 // seconds to settle back to rest
    private const val FREQ = 11.0 // wobble speed (rad/s)
    private const val BASE_AMP_M = 4.5 // peak bob (metres) — a small, point-blank building at L8
    private const val REF_HEIGHT_M = 12.0 // taller than this → progressively less bob ("more mass")
    private const val QUERY_R_BASE = 46.0 // blast query radius in screen px…
    private const val QUERY_R_PER_LEVEL = 7.0 // …growing with level (bigger blasts shake a wider area)
    private const val MAX_SHAKEN = 90 // safety cap on buildings animated at once

    private var map: dynamic = null
    private val active = mutableMapOf<String, Shake>() // feature id (as string) → live bob

    private class Shake(val rawId: dynamic, val end: Double, val amp: Double, val phase: Double)

    /** Bind to the base map that carries the building layer (from MapUtil.enable3D). */
    fun attach(baseMap: dynamic) {
        map = baseMap
    }

    /** An XMP of [level] detonated at ground [lng]/[lat]; bob nearby buildings. [now] = sim-clock seconds. */
    fun blast(lng: Double, lat: Double, level: Int, now: Double) {
        val m = map ?: return
        if (m.getLayer(LAYER) == null) return // no building layer (demo style, or not added yet)
        val lvl = level.coerceIn(1, 8)
        val origin: dynamic = js("[0.0, 0.0]")
        origin[0] = lng
        origin[1] = lat
        val pt = m.project(origin)
        val ox = pt.x as Double
        val oy = pt.y as Double
        val r = QUERY_R_BASE + QUERY_R_PER_LEVEL * lvl
        val feats = queryNear(m, ox, oy, r) ?: return
        val levelGain = 0.4 + 0.6 * (lvl / 8.0)
        val count = (feats.length as Int).coerceAtMost(MAX_SHAKEN)
        for (i in 0 until count) {
            val f = feats[i]
            val id = f.id
            if (id == null) continue // need a native feature id to drive feature-state
            val prox = proximity(m, f, ox, oy, r)
            val renderHeight = (f.properties?.render_height as? Double) ?: 8.0
            val mass = REF_HEIGHT_M / maxOf(renderHeight, REF_HEIGHT_M) // taller → smaller
            val amp = BASE_AMP_M * levelGain * prox * mass
            if (amp > 0.0) { // prox 0 (at the edge) → no bob
                val key = "$id"
                active[key] = Shake(id, now + DURATION, amp, (key.hashCode() % 628) / 100.0)
            }
        }
    }

    /** Advance every live bob (decaying wobble), writing feature-state; drop finished ones. [now] = seconds. */
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

    // Screen-space falloff (1 at the blast point → 0 at the query radius) from the feature's first vertex.
    private fun proximity(m: dynamic, f: dynamic, ox: Double, oy: Double, r: Double): Double {
        val ll = firstLngLat(f.geometry) ?: return 1.0
        val p = m.project(ll)
        val d = hypot((p.x as Double) - ox, (p.y as Double) - oy)
        return (1.0 - d / r).coerceIn(0.0, 1.0)
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
