package util.data

import World
import config.Dim
import config.Sim
import system.grid.GridFixture
import util.Rng
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The World-coupled [Pos] extensions in `PosExt.kt`: grid/screen bounds ([Pos.isOffGrid]/[Pos.isOffScreen]),
 * the shadow⇄sim conversions, the random-near-point jitter, the geo projection ([Pos.toGeo]), portal-proximity
 * checks ([Pos.hasClosePortal]/[Pos.hasClosePortalForClick]/[Pos.findClosestPortal]), passability, and the
 * multi-cell [Pos.isBuildable] gate. Sim is sized so a shadow cell is 1/10 of a sim px over a passable grid.
 */
class PosExtCoverageTest {

    private var savedWidth = 0
    private var savedHeight = 0
    private var savedRound = true

    @BeforeTest
    fun setup() {
        savedWidth = Sim.width
        savedHeight = Sim.height
        savedRound = Sim.roundField
        Sim.setExactSize(1800, 1200) // shadowW = 180, shadowH = 120 — matches the fixture grid
        Sim.roundField = false // rectangle field → isInsideField always true (keeps isBuildable about passability)
        World.grid = GridFixture("POSEXT", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        Rng.seed(31)
    }

    @AfterTest
    fun tidy() {
        Sim.setExactSize(savedWidth, savedHeight)
        Sim.roundField = savedRound
        World.grid = emptyMap()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
    }

    @Test
    fun offGridUsesShadowExtent() {
        assertFalse(Pos(50, 50).isOffGrid(), "a cell inside the 180×120 shadow grid is on-grid")
        assertTrue(Pos(200, 50).isOffGrid(), "x past shadowW is off-grid")
        assertTrue(Pos(50, 130).isOffGrid(), "y past shadowH is off-grid")
        assertTrue(Pos(-1, 50).isOffGrid(), "negative coords are off-grid")
    }

    @Test
    fun offScreenUsesTheDimExtent() {
        assertFalse(Pos(10, 10).isOffScreen(), "a point inside the screen is on-screen")
        assertTrue(Pos(-1, 10).isOffScreen(), "x<0 is off-screen")
        assertTrue(Pos(10, -1).isOffScreen(), "y<0 is off-screen")
        assertTrue(Pos(Dim.width, 10).isOffScreen(), "x==Dim.width is off-screen")
        assertTrue(Pos(10, Dim.height).isOffScreen(), "y==Dim.height is off-screen")
    }

    @Test
    fun shadowConversionsRoundTripByResolution() {
        assertEquals(Pos(10, 20), Pos(105, 205).toShadow(), "sim → shadow floors by res=10")
        assertEquals(Pos(100, 200), Pos(10, 20).fromShadow(), "shadow → sim multiplies by res=10")
    }

    @Test
    fun randomNearPointStaysWithinTheRadius() {
        val centre = Pos(500, 500)
        repeat(10) {
            val jittered = centre.randomNearPoint(100)
            assertTrue(centre.distanceTo(jittered) < 101.0, "a jittered point stays within the radius, was $jittered")
        }
    }

    @Test
    fun toGeoIsMonotonicInBothAxes() {
        val origin = Pos(0, 0).toGeo()
        assertTrue(Pos(1000, 0).toGeo().lat > origin.lat, "greater x → greater latitude")
        assertTrue(Pos(0, 1000).toGeo().lng < origin.lng, "greater y → smaller longitude")
    }

    @Test
    fun portalProximityChecks() {
        World.allPortals.add(portal.Portal.create(Pos(500, 500)))
        assertTrue(Pos(510, 500).hasClosePortal(), "10px away is within the min portal spacing (96)")
        assertFalse(Pos(500, 900).hasClosePortal(), "400px away is clear of any portal")
        assertTrue(Pos(505, 500).hasClosePortalForClick(), "5px away is within the click radius (16)")
        assertFalse(Pos(540, 500).hasClosePortalForClick(), "40px away is outside the click radius")
        assertEquals(Pos(500, 500), Pos(510, 500).findClosestPortal().location, "the nearby portal is found")
    }

    @Test
    fun isPassableReadsTheGridCell() {
        assertTrue(Pos(500, 500).isPassable(), "a cell over the passable grid is passable")
        assertFalse(Pos(50_000, 50_000).isPassable(), "a cell off the grid is not passable (null → false)")
    }

    @Test
    fun isBuildableNeedsPassabilityAndSpacing() {
        assertTrue(Pos(500, 500).isBuildable(), "a clear, passable, in-field spot is buildable")
        World.allPortals.add(portal.Portal.create(Pos(500, 500)))
        assertFalse(Pos(500, 500).isBuildable(), "a spot with a close portal is not buildable")
        assertFalse(Pos(50_000, 50_000).isBuildable(), "an off-grid spot is not buildable")
    }
}
