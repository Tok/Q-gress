package agent.action.cond

import Factory
import World
import config.Dim
import items.deployable.Resonator
import portal.Octant
import portal.Portal
import portal.PortalKey
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [Linker] — links a held key's portal to the agent's own owned portal, preferring a target that closes a
 * triangle (a field, the only source of MU). Covers the [Linker.isActionPossible] gate (owned link-out portal +
 * a friendly key + a valid, non-crossing target) and that [Linker.performAction] actually creates the link.
 */
class LinkerTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        Rng.seed(13)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
    }

    private fun ownedPortalAt(pos: Pos, owner: agent.Agent): Portal {
        val p = Portal.create(pos)
        p.owner = owner
        p.slots.getValue(Octant.N).deployReso(owner, Resonator.create(owner, 1), Dim.maxDeploymentRange.toInt())
        return p
    }

    @Test
    fun aKeylessAgentCannotLink() {
        val agent = Factory.frog()
        agent.actionPortal = Factory.portal() // a neutral portal it doesn't even own
        assertFalse(Linker.isActionPossible(agent), "no owned link-out portal + no keys → no link")
    }

    @Test
    fun anOwnedPortalWithAFriendlyKeyCanLinkAndDoesSo() {
        val agent = Factory.frog()
        val home = ownedPortalAt(Pos(400, 400), agent)
        val target = ownedPortalAt(Pos(600, 400), agent)
        World.allPortals.add(home)
        World.allPortals.add(target)
        agent.actionPortal = home
        agent.inventory.items.add(PortalKey(target, agent)) // a friendly key to the target
        assertTrue(Linker.isActionPossible(agent), "owned portal + friendly key + a valid target → link is possible")
        Linker.performAction(agent)
        assertTrue(home.links.isNotEmpty(), "performing the link creates an outgoing link")
    }
}
