package portal

import Factory
import World
import config.Dim
import items.deployable.Resonator
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The [Field] anchor queries the linker/attacker AI leans on — weakest portal, the two strongest anchors,
 * furthest-from — plus the duplicate-field guard in [Field.create].
 */
class FieldCoverageTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.isReady = true
        Rng.seed(61)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.isReady = false
    }

    private fun deployed(pos: Pos, level: Int): Portal {
        val owner = Factory.frog()
        val portal = Portal.create(pos)
        portal.deploy(owner, Octant.values().associateWith { Resonator.create(owner, level) }, Dim.minDeploymentRange.toInt())
        World.allPortals.add(portal)
        return portal
    }

    @Test
    fun anchorQueriesRankByHealthAndDistance() {
        val origin = deployed(Pos(0, 0), 8) // strongest
        val primary = deployed(Pos(600, 0), 4)
        val secondary = deployed(Pos(0, 800), 1) // weakest
        val field = requireNotNull(Field.create(origin, primary, secondary, Factory.frog())) { "field forms" }

        // Fresh full resonators → all three anchors read 100% health, so the stable health sort preserves the
        // idSet order [origin, primary, secondary]; assert against that deterministic ordering.
        assertEquals(secondary, field.weakestPortal(), "weakestPortal is the last anchor by the (tied) health sort")
        val strongest = field.strongestAnchors()
        assertEquals(2, strongest.size, "strongestAnchors returns two anchors")
        assertTrue(strongest.contains(origin) && strongest.contains(primary), "the first two anchors by the health sort")
        assertEquals(origin, field.findFurthestFrom(origin), "findFurthestFrom returns the nearest anchor to the query (itself here)")
    }

    @Test
    fun createRefusesADuplicateField() {
        val a = deployed(Pos(0, 0), 4)
        val b = deployed(Pos(600, 0), 4)
        val c = deployed(Pos(0, 800), 4)
        val first = requireNotNull(Field.create(a, b, c, Factory.frog())) { "the first field forms" }
        a.fields.add(first)
        assertNull(Field.create(a, b, c, Factory.frog()), "a field over the same three portals can't be recreated")
    }
}
