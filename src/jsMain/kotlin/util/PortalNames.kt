package util

import external.MapLibre
import kotlinx.browser.window
import system.display.Scene3D
import util.data.Pos

/**
 * Real portal names from the map's vector data, replacing the random gibberish. The shadow map
 * (which covers the whole play area) carries the OpenMapTiles `openmaptiles` source; we query its
 * `poi` and `transportation_name` source-layers — even though the shadow style doesn't *render*
 * them, `querySourceFeatures` returns features from the loaded tiles.
 *
 * [build] runs at grid time (tiles loaded, but Scene3D not yet anchored) so it only stashes raw
 * lng/lat + name; the lng/lat → sim-[Pos] projection is deferred to first [nameFor] call (by then
 * Scene3D is registered). A portal takes the nearest named POI, else the nearest street, else null
 * (caller falls back to the generator).
 */
object PortalNames {
    private const val POI_RADIUS = 90.0 // sim px: how close a POI must be to lend its name
    private const val STREET_RADIUS = 140.0

    private val rawPois = mutableListOf<Triple<Double, Double, String>>() // lng, lat, name
    private val rawStreets = mutableListOf<Triple<Double, Double, String>>()
    private var pois: List<Pair<Pos, String>>? = null // projected lazily
    private var streets: List<Pair<Pos, String>>? = null

    /** Query the shadow map's vector source for named POIs + streets (call once, after tiles load). */
    @Suppress("TooGenericExceptionCaught") // defensive: a map-data hiccup must never break world load
    fun build(shadowMap: MapLibre.Map) {
        rawPois.clear()
        rawStreets.clear()
        pois = null
        streets = null
        try { // never let a query hiccup break world init — just fall back to the generator
            collect(shadowMap, "poi", rawPois)
            collect(shadowMap, "transportation_name", rawStreets)
        } catch (e: Throwable) {
            console.log("PortalNames: query failed ($e)")
        }
        console.log("PortalNames: ${rawPois.size} POIs, ${rawStreets.size} streets from map data")
    }

    /** The name for a portal at [location], or null if nothing named is near enough. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException") // any failure → fall back to the generator
    fun nameFor(location: Pos): String? = try {
        val p = pois ?: project(rawPois).also { pois = it }
        val s = streets ?: project(rawStreets).also { streets = it }
        nearest(p, location, POI_RADIUS) ?: nearest(s, location, STREET_RADIUS)
    } catch (e: Throwable) {
        null // Scene3D not anchored yet → fall back to the generator (and retry on the next portal)
    }

    private fun collect(map: MapLibre.Map, sourceLayer: String, out: MutableList<Triple<Double, Double, String>>) {
        val params: dynamic = js("({})")
        params.sourceLayer = sourceLayer
        val feats = map.querySourceFeatures("openmaptiles", params) ?: return
        val n = feats.length.unsafeCast<Int>()
        var i = 0
        val seen = mutableSetOf<String>()
        while (i < n) {
            val f = feats[i]
            i++
            val name = featureName(f)
            val ll = if (name != null) featureLngLat(f) else null
            if (name != null && ll != null && seen.add(name)) {
                out.add(Triple(ll[0], ll[1], name)) // dedup repeats across tiles
            }
        }
    }

    private fun featureName(f: dynamic): String? {
        val props = f.properties ?: return null
        val raw = props.name ?: props["name:latin"] ?: props["name:en"] ?: return null
        val name = raw as? String ?: return null
        return if (name.isBlank()) null else name
    }

    private fun featureLngLat(f: dynamic): DoubleArray? {
        val geo = f.geometry ?: return null
        var c = geo.coordinates ?: return null
        var depth = 0
        // Descend nested arrays (Line/Multi*/Polygon) to the first [lng, lat] pair.
        while (depth < 4 && c[0] != null && c[0][0] != null) {
            c = c[0]
            depth++
        }
        val lng = c[0]
        val lat = c[1]
        return if (lng != null && lat != null) doubleArrayOf(lng as Double, lat as Double) else null
    }

    private fun project(raw: List<Triple<Double, Double, String>>): List<Pair<Pos, String>> = raw.map { (lng, lat, name) -> Scene3D.lngLatToSimPos(lng, lat) to name }

    private fun nearest(list: List<Pair<Pos, String>>, loc: Pos, radius: Double): String? {
        var best: String? = null
        var bestSq = radius * radius
        for ((pos, name) in list) {
            val dx = pos.x - loc.x
            val dy = pos.y - loc.y
            val d = dx * dx + dy * dy
            if (d < bestSq) {
                bestSq = d
                best = name
            }
        }
        return best
    }

    private val console get() = window.asDynamic().console
}
