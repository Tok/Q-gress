package system.building

import config.Styles
import external.MapLibre
import system.display.OwnBuildings
import system.display.Scene3D
import kotlin.math.floor

/**
 * Streams OSM buildings for wherever the camera goes (auto-cam drift, title orbit, manual pan), so the
 * world isn't bare once you leave the play area. On every map `idle`, if the view has settled over a
 * not-yet-loaded area, it pulls that region's buildings from Overpass ([BuildingTiles]) and meshes them
 * ([OwnBuildings], which dedups by footprint). A coarse cell grid tracks what's covered; a query box a
 * bit larger than the view keeps coverage running ahead of the camera. Far buildings are visual only
 * (no debris colliders — debris only spawns at play-area portals). [attach] once after gen.
 */
object BuildingStream {
    private const val CELL_DEG = 0.008 // ~600 m dedup grid
    private const val HALF_DEG = 0.012 // ~1.3 km half-box around the view centre → ~2.6 km query

    private val loadedCells = mutableSetOf<String>()
    private var busy = false // one Overpass query in flight at a time (natural throttle)

    /** Hook the base map's idle so settled views stream their region's buildings. */
    fun attach(map: MapLibre.Map) {
        map.asDynamic().on("idle", fun() {
            tick(map)
        })
    }

    /** Drop coverage state (a fresh world re-streams from scratch). */
    fun reset() {
        loadedCells.clear()
        busy = false
    }

    private fun tick(map: MapLibre.Map) {
        if (busy || !Styles.use3DBuildings || !OwnBuildings.REPLACE_BUILDINGS) return
        if (OwnBuildings.isFull()) return // meshed enough nearby buildings → stop hammering Overpass for far regions
        if (!Scene3D.terrainReady()) return
        val c = map.asDynamic().getCenter()
        val clng = c.lng as Double
        val clat = c.lat as Double
        if (cellKey(clng, clat) in loadedCells) return // already covered here
        val s = clat - HALF_DEG
        val n = clat + HALF_DEG
        val w = clng - HALF_DEG
        val e = clng + HALF_DEG
        markCells(s, w, n, e) // mark optimistically so a settle mid-fetch doesn't re-query
        busy = true
        BuildingTiles.loadBBox(s, w, n, e) { feats ->
            busy = false
            OwnBuildings.addFeatures(feats)
        }
    }

    private fun cellKey(lng: Double, lat: Double) = "${floor(lng / CELL_DEG).toInt()},${floor(lat / CELL_DEG).toInt()}"

    private fun markCells(south: Double, west: Double, north: Double, east: Double) {
        var j = floor(south / CELL_DEG).toInt()
        val jMax = floor(north / CELL_DEG).toInt()
        while (j <= jMax) {
            var i = floor(west / CELL_DEG).toInt()
            val iMax = floor(east / CELL_DEG).toInt()
            while (i <= iMax) {
                loadedCells.add("$i,$j")
                i++
            }
            j++
        }
    }
}
