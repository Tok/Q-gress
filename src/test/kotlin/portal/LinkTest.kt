package portal

import Factory
import agent.Faction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LinkTest {

    @Test
    fun agentSwitchEquality() = with(Factory) {
        val (origin, destination) = portalPair()
        val link = Link.create(origin, destination, linker())
        val switched = Link.create(origin, destination, linker())
        assertEquals(link, switched)
    }

    @Test
    fun factionSwitchEquality() = with(Factory) {
        val (origin, destination) = portalPair()
        val link = Link.create(origin, destination, frog())
        val switched = Link.create(origin, destination, smurf())
        assertEquals(link, switched)
    }

    @Test
    fun originSwitchEquality() = with(Factory) {
        val (origin, destination) = portalPair()
        val link = Link.create(origin, destination, linker())
        val switched = Link.create(destination, origin, linker())
        assertEquals(link, switched)
    }

    @Test
    fun noLinkingToOrigin() = with(Factory) {
        val origin = Portal.createRandom()
        assertFailsWith(IllegalStateException::class) {
            Link.create(origin, origin, linker())
        }
    }

    @Test
    fun linkMustHaveFaction() = with(Factory) {
        val (origin, destination) = portalPair()
        val linker = linker().copy(faction = Faction.NONE)
        assertFailsWith(IllegalStateException::class) {
            Link.create(origin, destination, linker)
        }
    }

    @Test
    fun linkConnectionToOrigin() = with(Factory) {
        val (origin, destination) = portalPair()
        val link = Link.create(origin, destination, linker())
        assertTrue(link?.isConnectedTo(origin) ?: false)
    }

    @Test
    fun linkConnectionToDestination() = with(Factory) {
        val (origin, destination) = portalPair()
        val link = Link.create(origin, destination, linker())
        assertTrue(link?.isConnectedTo(destination) ?: false)
    }
}
