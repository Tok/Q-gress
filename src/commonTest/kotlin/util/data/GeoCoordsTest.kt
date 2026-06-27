package util.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GeoCoordsTest {
    @Test
    fun diffsAndDistance() {
        val a = GeoCoords(3.0, 4.0)
        val b = GeoCoords(0.0, 0.0)
        assertEquals(3.0, a.lngDiff(b))
        assertEquals(4.0, a.latDiff(b))
        assertEquals(5.0, a.distanceTo(b), 1e-9) // 3-4-5
        assertEquals(0.0, a.distanceTo(a), "same point → 0")
    }

    @Test
    fun fromStringsParsesOrNull() {
        assertEquals(GeoCoords(9.37, 47.42), GeoCoords.fromStrings("9.37", "47.42"))
        assertNull(GeoCoords.fromStrings(null, "47.42"))
        assertNull(GeoCoords.fromStrings("9.37", null))
        assertNull(GeoCoords.fromStrings("notanumber", "47.42"))
        assertNull(GeoCoords.fromStrings("9.37", ""), "blank is not a number")
    }

    @Test
    fun stringFormat() {
        // Decimal values format identically on JS + JVM (whole numbers wouldn't — see ComplexTest).
        assertEquals("Geo-9.37:47.42", GeoCoords(9.37, 47.42).toString())
    }
}
