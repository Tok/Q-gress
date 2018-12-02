package agent

import Factory
import portal.PortalKey
import kotlin.test.*

class PortalKeyTest {

    @Test
    fun ownerId() = with(Factory) {
        val owner = owner()
        val key = PortalKey(portal(), owner)
        assertEquals(owner.key(), key.getOwnerId())
    }

    @Test
    fun noLevel() {
        assertFailsWith(NotImplementedError::class) {
            Factory.portalKey().getLevel()
        }
    }

    @Test
    fun friendlyKey() = with(Factory) {
        val owner = owner()
        val portal = portal().copy(owner = owner)
        val key = PortalKey(portal, owner)
        assertTrue(key.isFriendlyToOwner())
    }

    @Test
    fun neutralKey() = assertFalse(Factory.portalKey().isFriendlyToOwner())

    @Test
    fun enemyKey() = with(Factory) {
        val keyOwner = frog()
        val portalOwner = smurf()
        val portal = portal().copy(owner = portalOwner)
        val key = PortalKey(portal, keyOwner)
        assertFalse(key.isFriendlyToOwner())
    }
}
