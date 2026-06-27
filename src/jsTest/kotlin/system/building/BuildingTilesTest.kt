package system.building

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pure bbox math behind the Overpass building query (the fetch/decode itself is the IO edge). */
class BuildingTilesTest {
    @Test
    fun bboxBracketsTheCentreSymmetrically() {
        val b = BuildingTiles.bbox(9.37, 47.42, 500.0, 400.0) // [south, west, north, east]
        assertTrue(b[0] < 47.42 && b[2] > 47.42, "lat brackets the centre")
        assertTrue(b[1] < 9.37 && b[3] > 9.37, "lng brackets the centre")
        assertEquals(47.42 - b[0], b[2] - 47.42, 1e-9, "symmetric in lat")
        assertEquals(9.37 - b[1], b[3] - 9.37, 1e-9, "symmetric in lng")
    }

    @Test
    fun bboxLatExtentIsHalfHeightPlusContext() {
        val halfH = 400.0
        val b = BuildingTiles.bbox(9.37, 47.42, 500.0, halfH)
        val expDLat = (halfH + BuildingTiles.CONTEXT_M) / 111_320.0
        assertEquals(expDLat, 47.42 - b[0], 1e-9)
    }
}
