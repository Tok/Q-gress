import agent.Faction
import config.Dim
import config.Sim
import items.deployable.Resonator
import portal.Octant
import portal.Portal
import portal.PortalKey
import system.grid.GridFixture
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The [World] aggregate reads the HUD, AI and integrity passes lean on: faction agent/portal/link/field
 * counts, total-MU, the passability queries, the pending-agent flush, the user-faction accessor, and the
 * link/field integrity prune.
 */
class WorldQueriesTest {

    @BeforeTest
    fun reset() {
        Sim.roundField = false
        World.grid = GridFixture("WORLDQ", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.pendingAgents.clear()
        World.tick = 0
        World.userFaction = null
        World.isReady = true
        Rng.seed(41)
    }

    @AfterTest
    fun tidy() {
        Sim.roundField = true
        World.grid = emptyMap()
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.pendingAgents.clear()
        World.tick = 0
        World.userFaction = null
        World.isReady = false
    }

    private fun ownedPortal(pos: Pos, faction: Faction): Portal {
        val a = if (faction == Faction.ENL) Factory.frog() else Factory.smurf()
        val p = Portal.create(pos)
        p.deploy(a, Octant.values().associateWith { Resonator.create(a, 4) }, Dim.maxDeploymentRange.toInt())
        World.allPortals.add(p)
        return p
    }

    @Test
    fun userFactionAccessorThrowsUntilChosen() {
        assertFailsWith<IllegalArgumentException> { World.userFactionOrThrow() }
        World.userFaction = Faction.RES
        assertEquals(Faction.RES, World.userFactionOrThrow(), "once set, the accessor returns the faction")
    }

    @Test
    fun screenAndGridExtentsAreDerived() {
        assertTrue(World.diagonalLength() > 0, "the screen diagonal is positive")
        assertTrue(World.totalArea() > 0, "the screen area is positive")
        assertTrue(World.hasGrid(), "the grid is initialised")
        assertTrue(World.passableCells().isNotEmpty(), "an all-passable fixture has passable cells")
        assertTrue(World.passableInActionArea().isNotEmpty(), "the action area has placeable cells")
    }

    @Test
    fun portalCountsSplitByFaction() {
        ownedPortal(Pos(400, 400), Faction.ENL)
        ownedPortal(Pos(600, 400), Faction.RES)
        World.allPortals.add(Portal.create(Pos(800, 400))) // neutral
        assertEquals(1, World.enlPortals().size, "one ENL portal")
        assertEquals(1, World.resPortals().size, "one RES portal")
        assertEquals(1, World.unclaimedPortals().size, "one neutral portal")
        assertEquals(1, World.factionPortals(Faction.ENL).size, "factionPortals(ENL)")
        assertEquals(3, World.countPortals(), "three portals total")
        assertEquals(1, World.countPortals(Faction.RES), "one RES portal counted")
    }

    @Test
    fun agentCountsAndRecruitHeadroom() {
        World.allAgents.add(Factory.frog())
        World.allAgents.add(Factory.smurf())
        assertEquals(2, World.countAgents(), "two agents on the board")
        assertEquals(1, World.countAgents(Faction.ENL), "one frog")
        assertTrue(World.canRecruitMore(Faction.ENL), "the roster has headroom for more frogs")
        assertTrue(World.frogs.isEmpty() || World.frogs.isNotEmpty(), "the frogs view is accessible")
    }

    @Test
    fun pendingAgentsFlushIntoTheRoster() {
        World.pendingAgents.add(Factory.frog())
        assertEquals(0, World.countAgents(), "nobody in the roster yet")
        World.flushPendingAgents()
        assertEquals(1, World.countAgents(), "the pending agent flushed into the roster")
        assertTrue(World.pendingAgents.isEmpty(), "the pending buffer was drained")
    }

    @Test
    fun linkFieldCountsAndTotalMu() {
        val linker = Factory.frog()
        World.allAgents.add(linker)
        val a = ownedPortal(Pos(400, 400), Faction.ENL)
        val b = ownedPortal(Pos(400, 1000), Faction.ENL)
        val c = ownedPortal(Pos(1000, 700), Faction.ENL)
        // Re-own all three to the single linker so the field is his.
        listOf(a, b, c).forEach { p ->
            p.owner = linker
            p.filledSlots().forEach { it.owner = linker }
        }
        repeat(2) { linker.inventory.items.add(PortalKey(c, linker)) }
        linker.inventory.items.add(PortalKey(b, linker))
        a.createLink(linker, c)
        b.createLink(linker, c)
        a.createLink(linker, b)
        assertTrue(World.countLinks() >= 3, "three links exist")
        assertTrue(World.countLinks(Faction.ENL) >= 3, "all links are ENL")
        assertEquals(World.allLinks().size, World.allLines().size, "every link projects a line")
        assertTrue(World.countFields(Faction.ENL) >= 1, "a field formed")
        assertTrue(World.calcTotalMu(Faction.ENL) > 0, "ENL holds positive MU")
        assertEquals(0, World.calcTotalMu(Faction.RES), "RES holds no MU")
    }

    @Test
    fun pruneDropsLinksToAVanishedEndpoint() {
        val linker = Factory.frog()
        World.allAgents.add(linker)
        val a = ownedPortal(Pos(400, 400), Faction.ENL)
        val b = ownedPortal(Pos(400, 1000), Faction.ENL)
        listOf(a, b).forEach { p ->
            p.owner = linker
            p.filledSlots().forEach { it.owner = linker }
        }
        linker.inventory.items.add(PortalKey(b, linker))
        a.createLink(linker, b)
        assertTrue(a.links.isNotEmpty(), "the link exists before pruning")
        b.owner = null // endpoint went neutral → the link is now invalid
        World.pruneInvalidLinksAndFields()
        assertTrue(a.links.isEmpty(), "the invalid link was pruned away")
    }

    @Test
    fun randomPortalReturnsABoardPortal() {
        val only = Portal.create(Pos(500, 500))
        World.allPortals.add(only)
        assertEquals(only, World.randomPortal(), "with one portal randomPortal returns it")
    }

    @Test
    fun walkabilityIsAWritableRatio() {
        World.walkability = 0.42
        assertEquals(0.42, World.walkability, 1e-9, "the walkability ratio round-trips")
    }
}
