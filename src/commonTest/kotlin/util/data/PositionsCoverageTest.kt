package util.data

import World
import config.Sim
import system.grid.GridFixture
import util.Rng
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The spawn/candidate factories in [Positions]: the one-scan [Positions.portalCandidates] (with its deploy-margin
 * and portal-spacing filters), [Positions.hasPortalSpace], [Positions.createRandomForPortal] (candidate path +
 * the packed-grid fallback), and the headless [Positions.createRandomPassable] (a passable cell centre in sim
 * coords, Pos(0,0) on an empty grid). Headless: Platform.isBrowser() is false, so these take the packed branch.
 */
class PositionsCoverageTest {

    private var savedWidth = 0
    private var savedHeight = 0
    private var savedRound = true

    @BeforeTest
    fun setup() {
        savedWidth = Sim.width
        savedHeight = Sim.height
        savedRound = Sim.roundField
        Sim.setExactSize(1800, 1200)
        Sim.roundField = false
        World.grid = GridFixture("POS", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        Rng.seed(41)
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
    fun portalCandidatesAreInFieldPassableAndClearOfPortals() {
        val candidates = Positions.portalCandidates()
        assertTrue(candidates.isNotEmpty(), "an open passable grid yields spawn candidates")
        assertTrue(candidates.all { it.isPassable() }, "every candidate sits on a passable cell")
        assertTrue(candidates.none { it.hasClosePortal() }, "candidates avoid existing portals")
    }

    @Test
    fun hasPortalSpaceIsTrueHeadless() {
        // Headless Platform.isBrowser() is false → the short-circuit makes space always available.
        assertTrue(Positions.hasPortalSpace(), "the density system is never blocked headless")
    }

    @Test
    fun createRandomForPortalReturnsAPassableCandidate() {
        val spot = Positions.createRandomForPortal()
        assertTrue(spot.isPassable(), "a spawn spot lands on a passable cell")
    }

    @Test
    fun createRandomForPortalFallsBackWhenNoCandidatesFit() {
        // A tiny grid whose every cell is inside the max-deployment margin → portalCandidates() is empty, so
        // createRandomForPortal must fall back to createRandomPassable over the raw grid.
        val tiny = (0..2).flatMap { x -> (0..2).map { y -> Pos(x, y) } }
            .associateWith { Cell(it, true, 0) }
        World.grid = tiny
        val spot = Positions.createRandomForPortal()
        assertTrue(World.grid.containsKey(spot.toShadow()), "the fallback spot maps back onto a grid cell")
    }

    @Test
    fun createRandomPassableReturnsACellCentreOrOriginWhenEmpty() {
        val spot = Positions.createRandomPassable(World.grid)
        assertTrue(spot.isPassable(), "a random passable cell is passable")
        // The point is a cell centre (shadow-cell × res + res/2), so it maps back onto a grid cell.
        assertTrue(World.grid.containsKey(spot.toShadow()), "the point sits on a grid cell, was $spot")

        assertTrue(Positions.createRandomPassable(emptyMap()) == Pos(0, 0), "an empty grid yields the origin")
    }
}
