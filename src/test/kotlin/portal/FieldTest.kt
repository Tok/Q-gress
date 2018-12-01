package portal

import agent.Agent
import agent.Faction
import util.data.Cell
import util.data.Coords
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FieldTest {
    fun testCoords() = Coords(0, 0)
    fun testCell() = Cell(testCoords(), true, 0)
    fun testGrid() = mapOf(testCoords() to testCell())
    fun testFrog() = Agent.createFrog(testGrid())
    fun testSmurf() = Agent.createSmurf(testGrid())

    @Test
    fun agentSwitchEquality() {
        val origin = Portal.createRandom()
        val primary = Portal.createRandom()
        val secondary = Portal.createRandom()
        val field = Field.create(origin, primary, secondary, testFrog())
        val switched = Field.create(origin, primary, secondary, testFrog())
        assertEquals(field, switched)
    }

    @Test
    fun factionSwitchEquality() {
        val origin = Portal.createRandom()
        val primary = Portal.createRandom()
        val secondary = Portal.createRandom()
        val field = Field.create(origin, primary, secondary, testFrog())
        val switched = Field.create(origin, primary, secondary, testSmurf())
        assertEquals(field, switched)
    }

    @Test
    fun anchorSwitchEquality() {
        val origin = Portal.createRandom()
        val primary = Portal.createRandom()
        val secondary = Portal.createRandom()
        val field = Field.create(origin, primary, secondary, testFrog())
        val switched = Field.create(origin, secondary, primary, testFrog())
        assertEquals(field, switched)
    }

    @Test
    fun originSwitchEquality() {
        val origin = Portal.createRandom()
        val primary = Portal.createRandom()
        val secondary = Portal.createRandom()
        val field = Field.create(origin, primary, secondary, testFrog())
        val switched = Field.create(primary, origin, secondary, testFrog())
        assertEquals(field, switched)
    }

    @Test
    fun anchorRotationEquality() {
        val origin = Portal.createRandom()
        val primary = Portal.createRandom()
        val secondary = Portal.createRandom()
        val field = Field.create(origin, primary, secondary, testFrog())
        val rotated = Field.create(secondary, origin, primary, testFrog())
        assertEquals(field, rotated)
    }

    @Test
    fun noDuplicatedPortalsInField() {
        val origin = Portal.createRandom()
        val primary = Portal.createRandom()
        val secondary = Portal.createRandom()
        val linker = testFrog()
        assertFailsWith(IllegalStateException::class) {
            Field.create(origin, origin, origin, linker)
        }
        assertFailsWith(IllegalStateException::class) {
            Field.create(origin, primary, primary, linker)
        }
        assertFailsWith(IllegalStateException::class) {
            Field.create(origin, secondary, secondary, linker)
        }
    }

    @Test
    fun fieldMustHaveFaction() {
        val origin = Portal.createRandom()
        val primary = Portal.createRandom()
        val secondary = Portal.createRandom()
        val linker = testFrog().copy(faction = Faction.NONE)
        assertFailsWith(IllegalStateException::class) {
            Field.create(origin, primary, secondary, linker)
        }
    }
}
