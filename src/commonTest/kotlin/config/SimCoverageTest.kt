package config

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The play-area sizing object [Sim]: area→side conversion, the sub-linear portal count, the bucketed
 * roster suggestions, area round-trip, the field-radius / membership delegates, and the clamping [Sim.setSize]
 * vs the exact [Sim.setExactSize]. Mutating dimensions/roundField is captured + restored so no other test leaks.
 */
class SimCoverageTest {

    private var savedWidth = 0
    private var savedHeight = 0
    private var savedRound = true

    @BeforeTest
    fun save() {
        savedWidth = Sim.width
        savedHeight = Sim.height
        savedRound = Sim.roundField
    }

    @AfterTest
    fun restore() {
        Sim.setExactSize(savedWidth, savedHeight)
        Sim.roundField = savedRound
    }

    @Test
    fun sideForAreaGrowsWithAreaAndIsPositive() {
        val small = Sim.sideForArea(Sim.SMALL_KM2)
        val large = Sim.sideForArea(Sim.LARGE_KM2)
        assertTrue(small > 0, "a positive area yields a positive side")
        assertTrue(large > small, "a bigger area yields a bigger side")
        // side = 2·sqrt(area·1e6/PI)/MPP_REF; a 1 km² inscribed circle spans a few thousand sim px.
        assertTrue(large in 1000..9000, "the large-preset side is in the expected px range, was $large")
    }

    @Test
    fun suggestedPortalsIsSubLinearAndFloored() {
        assertEquals(4, Sim.suggestedPortals(Sim.TINY_KM2), "tiny → ~4 portals")
        assertEquals(8, Sim.suggestedPortals(Sim.LARGE_KM2), "1 km² → 8 portals")
        assertEquals(3, Sim.suggestedPortals(0.001), "a sliver map floors at 3")
        assertTrue(
            Sim.suggestedPortals(Sim.GIANT_KM2) >= Sim.suggestedPortals(Sim.SMALL_KM2),
            "portal count never decreases with area",
        )
    }

    @Test
    fun suggestedAgentsBucketsBySize() {
        assertEquals(3, Sim.suggestedAgents(Sim.TINY_KM2))
        assertEquals(5, Sim.suggestedAgents(Sim.SMALL_KM2))
        assertEquals(8, Sim.suggestedAgents(Sim.MID_KM2))
        assertEquals(12, Sim.suggestedAgents(Sim.LARGE_KM2))
        assertEquals(16, Sim.suggestedAgents(Sim.GIANT_KM2))
    }

    @Test
    fun maxAgentsBucketsBySize() {
        assertEquals(8, Sim.maxAgents(Sim.TINY_KM2))
        assertEquals(16, Sim.maxAgents(Sim.SMALL_KM2))
        assertEquals(24, Sim.maxAgents(Sim.MID_KM2))
        assertEquals(28, Sim.maxAgents(Sim.LARGE_KM2))
        assertEquals(32, Sim.maxAgents(Sim.GIANT_KM2))
    }

    @Test
    fun areaKm2RoundTripsSideForArea() {
        val side = Sim.sideForArea(Sim.MID_KM2)
        Sim.setExactSize(side, side)
        assertEquals(Sim.MID_KM2, Sim.areaKm2(), 0.01, "area(sideForArea(k)) ≈ k for a square field")
    }

    @Test
    fun fieldRadiusIsHalfTheSmallerSide() {
        Sim.setExactSize(2000, 1000)
        assertEquals(500.0, Sim.fieldRadius(), 1e-9, "radius = min(w,h)/2")
    }

    @Test
    fun membershipDelegatesTrackRoundField() {
        Sim.setExactSize(1000, 1000)
        Sim.roundField = true
        assertTrue(Sim.isInsideField(500.0, 500.0), "the centre is inside the round field")
        assertFalse(Sim.isInsideField(1.0, 1.0), "a corner is outside the inscribed circle")
        assertTrue(Sim.isInPlayArea(500.0, 500.0), "the centre is a displayable play-area point")
        assertFalse(Sim.isInPlayArea(-1.0, 500.0), "off-screen coords are not in the play area")

        Sim.roundField = false
        assertTrue(Sim.isInsideField(1.0, 1.0), "a rectangle field admits every in-bounds point")
    }

    @Test
    fun setSizeClampsWhileSetExactSizeBypasses() {
        Sim.setSize(100, 100) // below the 600 floor
        assertEquals(600, Sim.width, "setSize clamps up to the 600 floor")
        assertEquals(600, Sim.height)
        Sim.setSize(99_999, 99_999) // above the 9000 ceiling
        assertEquals(9000, Sim.width, "setSize clamps down to the 9000 ceiling")

        Sim.setExactSize(250, 250) // below the floor, but the exact setter bypasses the clamp
        assertEquals(250, Sim.width, "setExactSize forces an unclamped extent")
        assertEquals(250, Sim.height)
    }

    @Test
    fun scaleIsTheLargerAxisRatioToTheScreen() {
        Sim.setExactSize(Dim.width * 2, Dim.height)
        assertEquals(2.0, Sim.scale, 1e-9, "scale is max(width/Dim.width, height/Dim.height)")
    }
}
