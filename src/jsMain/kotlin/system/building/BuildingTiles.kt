package system.building

import kotlinx.browser.window
import kotlin.math.PI
import kotlin.math.cos

/**
 * Loads a COMPLETE set of play-area building footprints straight from OpenStreetMap via the Overpass
 * API. We tried OpenFreeMap's vector tiles first, but its `building` layer is heavily simplified at its
 * max zoom (14) — e.g. a St. Gallen tile carries only ~19 of the 1100 buildings OSM actually has there.
 * Overpass returns every building polygon in a bbox (with height tags), which is exactly what we want
 * for our own meshes (shadows, shake, colliders). One keyless GET (CORS-enabled, browser-cacheable);
 * Overpass has rate limits, so this is a per-world-gen query, not a hot path.
 *
 * Output features match the GeoJSON shape `OwnBuildings`/`Scene3D.buildBuildingColliders` consume:
 * `{ geometry: { type:'Polygon', coordinates:[ring] }, properties:{ render_height, render_min_height } }`.
 */
object BuildingTiles {
    // Overpass mirrors, tried in order: the main endpoint 504s / rate-limits under load, so we fall over to
    // the next mirror before finally giving up (→ empty array → caller keeps MapLibre's buildings). All are
    // keyless + CORS-enabled + browser-cacheable.
    private val OVERPASS_URLS = arrayOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.openstreetmap.fr/api/interpreter",
    )
    const val CONTEXT_M = 250.0 // also pull buildings this far OUTSIDE the play area (off-area shadow casters)
    private const val METERS_PER_DEG = 111_320.0
    private const val MAX_BUILDINGS = 2500 // perf cap on how many footprints we hand back
    private const val DEFAULT_HEIGHT = 8.0 // when a building has no height/levels tag
    private const val LEVEL_HEIGHT_M = 3.0 // metres per storey when only building:levels is tagged
    private const val MIN_RING = 3 // a polygon needs ≥3 points

    /** [south, west, north, east] degree bounds of the play area + context margin. Pure (unit-tested). */
    fun bbox(lng: Double, lat: Double, halfWM: Double, halfHM: Double): DoubleArray {
        val dLat = (halfHM + CONTEXT_M) / METERS_PER_DEG
        val dLng = (halfWM + CONTEXT_M) / (METERS_PER_DEG * cos(lat * PI / 180.0))
        return doubleArrayOf(lat - dLat, lng - dLng, lat + dLat, lng + dLng)
    }

    /** Query OSM for every building in the play area + context and deliver them as lng/lat GeoJSON
     *  Polygon features (with render_height) to [onComplete]. Always fires (empty array on failure). */
    fun load(lng: Double, lat: Double, halfWM: Double, halfHM: Double, onComplete: (dynamic) -> Unit) {
        val b = bbox(lng, lat, halfWM, halfHM)
        loadBBox(b[0], b[1], b[2], b[3], onComplete)
    }

    /** Query OSM buildings in an explicit [south]/[west]/[north]/[east] degree bbox (for streaming new
     *  regions as the camera flies elsewhere). Always fires [onComplete] (empty array on failure). */
    fun loadBBox(south: Double, west: Double, north: Double, east: Double, onComplete: (dynamic) -> Unit) {
        val q = "[out:json][timeout:25];way[\"building\"]($south,$west,$north,$east);out geom;"
        val data = "?data=" + (js("encodeURIComponent")(q) as String) // GET → browser-cacheable
        tryMirror(0, data, onComplete)
    }

    // Fetch from mirror [idx]; on a non-OK response (e.g. 504) or network error, fall over to the next one.
    // When every mirror is exhausted we hand back an empty array so the caller keeps MapLibre's buildings.
    private fun tryMirror(idx: Int, data: String, onComplete: (dynamic) -> Unit) {
        if (idx >= OVERPASS_URLS.size) {
            console.warn("BuildingTiles(OSM): all ${OVERPASS_URLS.size} Overpass mirrors failed → MapLibre fallback")
            onComplete(js("[]"))
            return
        }
        val next = { tryMirror(idx + 1, data, onComplete) }
        window.asDynamic().fetch(OVERPASS_URLS[idx] + data)
            .then { r: dynamic -> if (r.ok == true) r.json() else null }
            .then { j: dynamic -> if (j == null) next() else onComplete(toFeatures(j)) }
            .catch { _: dynamic -> next() }
    }

    private fun toFeatures(j: dynamic): dynamic {
        val out: dynamic = js("[]")
        val els = j?.elements ?: return out
        val n = (els.length as? Int) ?: return out
        var i = 0
        while (i < n && (out.length as Int) < MAX_BUILDINGS) {
            val f = wayToFeature(els[i])
            i++
            if (f != null) out.push(f)
        }
        console.log("BuildingTiles(OSM): $n ways → ${out.length} building features")
        return out
    }

    // One OSM building way → a GeoJSON Polygon feature. `out geom;` gives geometry as [{lat,lon},...].
    private fun wayToFeature(el: dynamic): dynamic? {
        val geom = el?.geometry ?: return null
        val nPts = (geom.length as? Int) ?: return null
        if (nPts < MIN_RING) return null
        val ring: dynamic = js("[]")
        var k = 0
        while (k < nPts) {
            val p = geom[k]
            k++
            val pt: dynamic = js("[]")
            pt.push(p.lon)
            pt.push(p.lat)
            ring.push(pt)
        }
        val feature: dynamic = js("({ type: 'Feature' })")
        val g: dynamic = js("({ type: 'Polygon' })")
        g.coordinates = js("[]")
        g.coordinates.push(ring)
        feature.geometry = g
        val props: dynamic = js("({})")
        props.render_height = heightOf(el.tags)
        props.render_min_height = 0
        feature.properties = props
        return feature
    }

    // OSM height in metres: explicit `height`, else `building:levels` × storey height, else a default.
    private fun heightOf(tags: dynamic): Double {
        if (tags == null) return DEFAULT_HEIGHT
        val h = numFrom(tags.height)
        if (h != null && h > 0.5) return h
        val levels = numFrom(tags["building:levels"])
        if (levels != null && levels > 0.0) return levels * LEVEL_HEIGHT_M
        return DEFAULT_HEIGHT
    }

    private fun numFrom(v: dynamic): Double? {
        if (v == null) return null
        val d = (js("parseFloat")("" + v) as? Double) ?: return null
        return if (js("isNaN")(d) as Boolean) null else d
    }
}
