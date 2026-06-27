package util

import config.Config
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

    // The loaded tiles carry thousands of named features; we only ever name [Config.startPortals] portals,
    // so keep just the nearest-to-centre ones (the rest are off-screen anyway) — scaled to the portal count
    // (a small map needs only ~20-30). Bounded so naming has variety yet stays cheap.
    private fun poiLimit() = (Config.startPortals * 4).coerceIn(24, 200)
    private fun streetLimit() = (Config.startPortals * 3).coerceIn(16, 120)

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
            val center = shadowMap.getCenter()
            val cx = center.lng as Double
            val cy = center.lat as Double
            collect(shadowMap, "poi", rawPois, cx, cy, poiLimit())
            collect(shadowMap, "transportation_name", rawStreets, cx, cy, streetLimit())
        } catch (e: Throwable) {
            console.log("PortalNames: query failed ($e)")
        }
        console.log("PortalNames: ${rawPois.size} POIs, ${rawStreets.size} streets (nearest to centre) from map data")
    }

    /** The name for a portal at [location], or null if nothing named is near enough. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException") // any failure → fall back to the generator
    fun nameFor(location: Pos): String? = try {
        val p = pois ?: project(rawPois).also { pois = it }
        val s = streets ?: project(rawStreets).also { streets = it }
        // Prefer a *close* named POI, then a close street; but rather than fall back to random
        // gibberish for portals in POI/street-sparse spots, adopt the nearest street (then nearest
        // POI) at any distance — so every portal gets a real map-derived name.
        nearest(p, location, POI_RADIUS)
            ?: nearest(s, location, STREET_RADIUS)
            ?: nearest(s, location, Double.POSITIVE_INFINITY)
            ?: nearest(p, location, Double.POSITIVE_INFINITY)
    } catch (e: Throwable) {
        null // Scene3D not anchored yet → fall back to the generator (and retry on the next portal)
    }

    private fun collect(
        map: MapLibre.Map,
        sourceLayer: String,
        out: MutableList<Triple<Double, Double, String>>,
        cx: Double,
        cy: Double,
        limit: Int,
    ) {
        val params: dynamic = js("({})")
        params.sourceLayer = sourceLayer
        val feats = map.querySourceFeatures("openmaptiles", params) ?: return
        val n = feats.length.unsafeCast<Int>()
        var i = 0
        val seen = mutableSetOf<String>()
        val all = mutableListOf<Triple<Double, Double, String>>()
        while (i < n) {
            val f = feats[i]
            i++
            val name = featureName(f)
            val ll = if (name != null) featureLngLat(f) else null
            if (name != null && ll != null && seen.add(name)) {
                all.add(Triple(ll[0], ll[1], name)) // dedup repeats across tiles
            }
        }
        // Keep only the nearest-to-centre features (lng/lat squared distance — fine over one play area).
        all.sortBy { (lng, lat, _) -> (lng - cx) * (lng - cx) + (lat - cy) * (lat - cy) }
        out.addAll(all.take(limit))
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

    private fun project(raw: List<Triple<Double, Double, String>>): List<Pair<Pos, String>> = raw.map { (lng, lat, name) ->
        Scene3D.lngLatToSimPos(lng, lat) to
            name
    }

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

    // --- Synthetic fallback name (when no real map name is near — see [nameFor]). Was PortalNames.generate. ---

    /** A generated portal name (letter-frequency gibberish + an optional place-type suffix). */
    fun generate(): String {
        val separator = if (Rng.random() < 0.3) "-" else " "
        val name = generateName(3, 5)
        val values = listOf(
            1.00 to "",
            0.15 to separator + "Portal",
            0.05 to separator + "Square",
            0.10 to separator + "Street",
            0.07 to separator + "Fountain",
            0.08 to separator + "Park",
            0.03 to separator + "Station",
            0.02 to separator + "House",
            0.01 to separator + "Memorial",
            0.01 to separator + "Museum",
        )
        return name + Rng.select(values, "")
    }

    private fun generateName(minLength: Int, maxLength: Int): String {
        val length = minLength + Rng.randomInt(maxLength - minLength)
        val firstLetter = Rng.select(generateFirstSelection(), ' ')
        val name = firstLetter + IntRange(1, length).map { Rng.select(generateSelection(), ' ') }.joinToString("")
        val temp = name.substring(0, 1).uppercase() + name.substring(1).lowercase()
        return if (temp.endsWith('-')) temp.dropLast(1) else temp
    }

    /** Relative letter frequency of English words (Wikipedia: Letter frequency). */
    private fun generateSelection(): List<Pair<Double, Char>> = listOf(
        12.702 to 'E', 9.056 to 'T', 8.167 to 'A', 7.507 to 'O', 6.966 to 'I', 6.749 to 'N',
        6.327 to 'S', 6.094 to 'H', 5.987 to 'R', 4.253 to 'D', 4.025 to 'L', 2.782 to 'C',
        2.758 to 'U', 2.406 to 'M', 2.360 to 'W', 2.228 to 'F', 2.015 to 'G', 1.974 to 'Y',
        1.929 to 'P', 1.492 to 'B', 0.978 to 'V', 0.772 to 'K', 0.153 to 'J', 0.150 to 'X',
        0.095 to 'Q', 0.074 to 'Z',
    )

    /** Relative frequency of the FIRST letter of an English word. */
    private fun generateFirstSelection(): List<Pair<Double, Char>> = listOf(
        15.978 to 'T', 11.682 to 'A', 7.631 to 'O', 7.294 to 'I', 6.686 to 'S', 5.497 to 'W',
        5.238 to 'C', 4.434 to 'B', 4.319 to 'P', 4.200 to 'H', 4.027 to 'F', 3.826 to 'M',
        3.174 to 'D', 2.826 to 'R', 2.799 to 'E', 2.415 to 'L', 2.284 to 'N', 1.642 to 'G',
        1.183 to 'U', 0.824 to 'V', 0.763 to 'Y', 0.511 to 'J', 0.456 to 'K', 0.222 to 'Q',
        0.045 to 'X', 0.045 to 'Z',
    )
}
