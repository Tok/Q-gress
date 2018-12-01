package portal

import agent.Agent
import agent.Faction
import util.data.Cell
import util.data.Coords
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LinkTest {
    private fun testCoords() = Coords(0, 0)
    private fun testCell() = Cell(testCoords(), true, 0)
    private fun testGrid() = mapOf(testCoords() to testCell())
    private fun testFrog() = Agent.createFrog(testGrid())
    private fun testSmurf() = Agent.createSmurf(testGrid())
    private fun testPortals() = Portal.createRandom() to Portal.createRandom()

    @Test
    fun agentSwitchEquality() {
        val (origin, destination) = testPortals()
        val link = Link.create(origin, destination, testFrog())
        val switched = Link.create(origin, destination, testFrog())
        assertEquals(link, switched)
    }

    @Test
    fun factionSwitchEquality() {
        val (origin, destination) = testPortals()
        val link = Link.create(origin, destination, testFrog())
        val switched = Link.create(origin, destination, testSmurf())
        assertEquals(link, switched)
    }

    @Test
    fun originSwitchEquality() {
        val (origin, destination) = testPortals()
        val link = Link.create(origin, destination, testFrog())
        val switched = Link.create(destination, origin, testFrog())
        assertEquals(link, switched)
    }

    @Test
    fun noLinkingToOrigin() {
        val origin = Portal.createRandom()
        assertFailsWith(IllegalStateException::class) {
            Link.create(origin, origin, testFrog())
        }
    }

    @Test
    fun linkMustHaveFaction() {
        val (origin, destination) = testPortals()
        val linker = testFrog().copy(faction = Faction.NONE)
        assertFailsWith(IllegalStateException::class) {
            Link.create(origin, destination, linker)
        }
    }
}
