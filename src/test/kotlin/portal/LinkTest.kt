package portal

import agent.Agent
import agent.Faction
import util.data.Cell
import util.data.Coords
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LinkTest {
    fun testCoords() = Coords(0, 0)
    fun testCell() = Cell(testCoords(), true, 0)
    fun testGrid() = mapOf(testCoords() to testCell())
    fun testFrog() = Agent.createFrog(testGrid())
    fun testSmurf() = Agent.createSmurf(testGrid())

    @Test
    fun agentSwitchEquality() {
        val origin = Portal.createRandom()
        val destination = Portal.createRandom()
        val link = Link.create(origin, destination, testFrog())
        val switched = Link.create(origin, destination, testFrog())
        assertEquals(link, switched)
    }

    @Test
    fun factionSwitchEquality() {
        val origin = Portal.createRandom()
        val destination = Portal.createRandom()
        val link = Link.create(origin, destination, testFrog())
        val switched = Link.create(origin, destination, testSmurf())
        assertEquals(link, switched)
    }

    @Test
    fun originSwitchEquality() {
        val origin = Portal.createRandom()
        val destination = Portal.createRandom()
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
        val origin = Portal.createRandom()
        val destination = Portal.createRandom()
        val linker = testFrog().copy(faction = Faction.NONE)
        assertFailsWith(IllegalStateException::class) {
            Link.create(origin, destination, linker)
        }
    }
}
