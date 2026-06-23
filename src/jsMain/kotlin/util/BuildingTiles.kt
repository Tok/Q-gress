package util

import external.Pbf
import external.VectorTileModule
import kotlinx.browser.window
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * Fetches the OpenFreeMap `.pbf` vector tiles covering the play area and decodes their `building`
 * layer ourselves (via pbf + @mapbox/vector-tile), yielding a COMPLETE set of footprints as lng/lat
 * GeoJSON features — MapLibre's own query APIs (`queryRenderedFeatures`/`querySourceFeatures`) only
 * ever return a small, far-flung fraction of the buildings it paints. The tiles are already in the
 * browser HTTP cache from MapLibre's own rendering, so our fetches are near-instant and add no real
 * network overhead. Decoding gives us the door to do far more with the data later (roads → pathfinding,
 * water, POI). Tile math is pure (unit-tested); the fetch/decode is the IO edge.
 */
object BuildingTiles {
    const val BUILDING_ZOOM = 14 // openmaptiles serves buildings up to z14
    const val CONTEXT_M = 250.0 // also keep buildings this far OUTSIDE the play area (off-area shadow casters)
    private const val SOURCE_LAYER = "building"
    private const val METERS_PER_DEG = 111_320.0
    private const val TILE_MARGIN_M = 200.0 // pad the tile cover so edge tiles are always included
    private const val MAX_NEST = 6 // GeoJSON coordinate nesting guard (Point→…→MultiPolygon)

    data class Tile(val x: Int, val y: Int, val z: Int)

    private var template: String? = null // resolved (versioned) {z}/{x}/{y} url, read once from the TileJSON

    /** Slippy-map tile containing [lng]/[lat] at zoom [z] (standard web-mercator tiling). */
    fun tileOf(lng: Double, lat: Double, z: Int): Tile {
        val n = (1 shl z).toDouble()
        val xt = (lng + 180.0) / 360.0 * n
        val latRad = lat * PI / 180.0
        val yt = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
        return Tile(floor(xt).toInt(), floor(yt).toInt(), z)
    }

    /** Every [BUILDING_ZOOM] tile covering the play area (centre ± half-extents in metres) + a margin. */
    fun tilesForBounds(lng: Double, lat: Double, halfWM: Double, halfHM: Double): List<Tile> {
        val dLat = (halfHM + TILE_MARGIN_M) / METERS_PER_DEG
        val dLng = (halfWM + TILE_MARGIN_M) / (METERS_PER_DEG * cos(lat * PI / 180.0))
        val nw = tileOf(lng - dLng, lat + dLat, BUILDING_ZOOM) // north-west → min x, min y
        val se = tileOf(lng + dLng, lat - dLat, BUILDING_ZOOM) // south-east → max x, max y
        val tiles = mutableListOf<Tile>()
        var x = nw.x
        while (x <= se.x) {
            var y = nw.y
            while (y <= se.y) {
                tiles.add(Tile(x, y, BUILDING_ZOOM))
                y++
            }
            x++
        }
        return tiles
    }

    // Per-load state (clip bounds + accumulator + completion), kept off the parameter lists.
    private class Job(val lng: Double, val lat: Double, val ctxW: Double, val ctxH: Double, val onComplete: (dynamic) -> Unit) {
        val all: dynamic = js("[]")
        var remaining = 0
    }

    /**
     * Resolve the tile template (once, from the openmaptiles TileJSON), fetch every covering tile,
     * decode its building features (clipped to the play area + [CONTEXT_M] margin), and deliver the
     * merged GeoJSON-feature array to [onComplete]. [halfWM]/[halfHM] are the play-area half-extents
     * in scene metres; [onComplete] always fires (empty array if nothing loads).
     */
    fun load(lng: Double, lat: Double, halfWM: Double, halfHM: Double, onComplete: (dynamic) -> Unit) {
        val job = Job(lng, lat, halfWM + CONTEXT_M, halfHM + CONTEXT_M, onComplete)
        val cached = template
        if (cached != null) {
            fetchAll(cached, lng, lat, halfWM, halfHM, job)
            return
        }
        val req = window.asDynamic().fetch(MapStyles.OPENMAPTILES_URL)
        req.then { r: dynamic -> r.json() }.then { j: dynamic ->
            val arr = j?.tiles // JS array → index, NOT .get() (that compiles to a non-existent method call)
            val tmpl = (if (arr != null) arr[0] else null) as? String
            if (tmpl == null) {
                onComplete(job.all)
            } else {
                template = tmpl
                fetchAll(tmpl, lng, lat, halfWM, halfHM, job)
            }
        }.catch { _: dynamic -> onComplete(job.all) }
    }

    private fun fetchAll(tmpl: String, lng: Double, lat: Double, halfWM: Double, halfHM: Double, job: Job) {
        val tiles = tilesForBounds(lng, lat, halfWM, halfHM)
        if (tiles.isEmpty()) {
            job.onComplete(job.all)
            return
        }
        job.remaining = tiles.size
        tiles.forEach { t ->
            val url = tmpl.replace("{z}", t.z.toString()).replace("{x}", t.x.toString()).replace("{y}", t.y.toString())
            fetchTile(url, t, job)
        }
    }

    private fun fetchTile(url: String, t: Tile, job: Job) {
        val req = window.asDynamic().fetch(url)
        req.then { r: dynamic -> if (r.ok == true) r.arrayBuffer() else null }
            .then { buf: dynamic ->
                if (buf != null) decodeInto(buf, t, job)
                tileDone(job)
            }
            .catch { _: dynamic -> tileDone(job) }
    }

    private fun tileDone(job: Job) {
        job.remaining--
        if (job.remaining <= 0) job.onComplete(job.all)
    }

    private fun decodeInto(buf: dynamic, t: Tile, job: Job) {
        val pbf = Pbf(buf) // pbf wraps an ArrayBuffer in a Uint8Array itself
        val tile: dynamic = VectorTileModule.VectorTile(pbf)
        val layer = tile.layers[SOURCE_LAYER] ?: return
        val n = (layer.length as? Int) ?: return
        var i = 0
        while (i < n) {
            val gj = layer.feature(i).toGeoJSON(t.x, t.y, t.z)
            i++
            if (withinContext(gj, job)) job.all.push(gj)
        }
    }

    // Cheap clip: keep features whose first vertex falls within the play area + context margin (metres).
    private fun withinContext(gj: dynamic, job: Job): Boolean {
        val ll = firstLngLat(gj?.geometry?.coordinates) ?: return false
        val dEast = (ll[0] - job.lng) * METERS_PER_DEG * cos(job.lat * PI / 180.0)
        val dNorth = (ll[1] - job.lat) * METERS_PER_DEG
        return abs(dEast) <= job.ctxW && abs(dNorth) <= job.ctxH
    }

    // Descend the GeoJSON coordinate nest to the first [lng, lat] pair.
    private fun firstLngLat(coords: dynamic): DoubleArray? {
        var c = coords ?: return null
        var guard = 0
        while (guard < MAX_NEST && isArr(c) && isArr(c[0])) {
            c = c[0]
            guard++
        }
        if (!isArr(c) || ((c.length as? Int) ?: 0) < 2) return null
        return doubleArrayOf(c[0] as Double, c[1] as Double)
    }

    private fun isArr(v: dynamic): Boolean = js("Array.isArray")(v) as Boolean
}
