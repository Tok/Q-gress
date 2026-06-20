package portal

import Factory
import agent.Faction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FieldTest {

    @Test
    fun agentSwitchEquality() = with(Factory) {
        val (origin, primary, secondary) = portalTriple()
        val field = Field.create(origin, primary, secondary, agent())
        val switched = Field.create(origin, primary, secondary, agent())
        assertEquals(field, switched)
    }

    @Test
    fun factionSwitchEquality() = with(Factory) {
        val (origin, primary, secondary) = portalTriple()
        val field = Field.create(origin, primary, secondary, frog())
        val switched = Field.create(origin, primary, secondary, smurf())
        assertEquals(field, switched)
    }

    @Test
    fun anchorSwitchEquality() = with(Factory) {
        val (origin, primary, secondary) = portalTriple()
        val field = Field.create(origin, primary, secondary, agent())
        val switched = Field.create(origin, secondary, primary, agent())
        assertEquals(field, switched)
    }

    @Test
    fun originSwitchEquality() = with(Factory) {
        val (origin, primary, secondary) = portalTriple()
        val field = Field.create(origin, primary, secondary, agent())
        val switched = Field.create(primary, origin, secondary, agent())
        assertEquals(field, switched)
    }

    @Test
    fun anchorRotationEquality() = with(Factory) {
        val (origin, primary, secondary) = portalTriple()
        val field = Field.create(origin, primary, secondary, agent())
        val rotated = Field.create(secondary, origin, primary, agent())
        assertEquals(field, rotated)
    }

    @Test
    fun noDuplicatedPortalsInField() = with(Factory) {
        val (origin, primary, secondary) = portalTriple()
        val linker = agent()
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
}
