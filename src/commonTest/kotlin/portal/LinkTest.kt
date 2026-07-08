package portal

import Factory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinkTest {

    @Test
    fun toStringAndConnectivity() = with(Factory) {
        val (origin, destination) = portalPair()
        val link = requireNotNull(Link.create(origin, destination, linker())) { "the link forms" }
        assertTrue(link.isConnectedTo(origin), "connected to its origin")
        assertTrue(link.isConnectedTo(destination), "connected to its destination")
        assertFalse(link.isConnectedTo(Portal.createRandom()), "an unrelated portal is not connected")
        assertEquals("$origin --> $destination", link.toString(), "toString renders origin --> destination")
    }

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
