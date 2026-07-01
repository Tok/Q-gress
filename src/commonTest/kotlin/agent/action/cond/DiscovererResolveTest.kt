package agent.action.cond

import Factory
import World
import config.Config
import config.Sim
import system.grid.GridFixture
import util.Rng
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [Discoverer.resolve] — the density-driven portal churn an agent rolls when it arrives from a discovery stroll:
 * a new portal is DISCOVERED or a random one found GONE, converging the board toward [Config.targetPortals].
 * These cover the disabled short-circuit and that repeated churn stays inside the [minPortals, maxPortals] band
 * (the pure churn curve itself lives in [system.ChurnMathTest]).
 */
class DiscovererResolveTest {

    @BeforeTest
    fun reset() {
        Sim.roundField = false
        World.grid = GridFixture("DISC", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.walkability = 0.5
        World.allPortals.clear()
        World.portalDiscoveryEnabled = true
        Rng.seed(21)
    }

    @AfterTest
    fun tidy() {
        Sim.roundField = true
        World.grid = emptyMap()
        World.walkability = 0.0
        World.allPortals.clear()
        World.portalDiscoveryEnabled = true
    }

    @Test
    fun discoveryDisabledLeavesTheBoardUntouched() {
        World.portalDiscoveryEnabled = false
        repeat(6) { World.allPortals.add(Factory.portal()) }
        val before = World.countPortals()
        repeat(20) { Discoverer.resolve(Factory.frog()) }
        assertEquals(before, World.countPortals(), "with discovery off the fixed board never churns")
    }

    @Test
    fun churnKeepsThePortalCountWithinBounds() {
        repeat(6) { World.allPortals.add(Factory.portal()) }
        val agent = Factory.frog()
        repeat(300) { Discoverer.resolve(agent) } // exercises both the create and remove branches
        assertTrue(World.countPortals() >= Config.minPortals, "churn never drops below the floor")
        assertTrue(World.countPortals() <= Config.maxPortals, "churn never exceeds the ceiling")
    }
}
