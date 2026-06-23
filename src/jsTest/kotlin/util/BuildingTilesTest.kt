package util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pure slippy-map tile math behind the vector-tile building loader (the fetch/decode is the IO edge). */
class BuildingTilesTest {
    @Test
    fun tileOfEquatorPrimeMeridianAtZoom1() {
        val t = BuildingTiles.tileOf(0.0, 0.0, 1) // origin sits at the 2×2 grid's centre corner
        assertEquals(BuildingTiles.Tile(1, 1, 1), t)
    }

    @Test
    fun tileOfWholeWorldAtZoom0() {
        val t = BuildingTiles.tileOf(-179.0, 84.0, 0) // a single tile covers the world at z0
        assertEquals(0, t.x)
        assertEquals(0, t.y)
    }

    @Test
    fun tilesForBoundsCoverCentreAndFormARectangle() {
        val lng = 9.37
        val lat = 47.42 // St. Gallen-ish
        val tiles = BuildingTiles.tilesForBounds(lng, lat, 500.0, 400.0)
        val centre = BuildingTiles.tileOf(lng, lat, BuildingTiles.BUILDING_ZOOM)
        assertTrue(tiles.contains(centre), "must cover the centre tile")
        assertTrue(tiles.all { it.z == BuildingTiles.BUILDING_ZOOM }, "all at the building zoom")
        // Contiguous rectangle: count == (x-span) × (y-span).
        val minX = requireNotNull(tiles.minOfOrNull { it.x })
        val maxX = requireNotNull(tiles.maxOfOrNull { it.x })
        val minY = requireNotNull(tiles.minOfOrNull { it.y })
        val maxY = requireNotNull(tiles.maxOfOrNull { it.y })
        assertEquals((maxX - minX + 1) * (maxY - minY + 1), tiles.size)
    }
}
