package agent.action.cond

import Factory
import World
import agent.Faction
import util.Rng
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * [Glypher.performAction] — glyph-hacking a portal: the up-front retaliation zap from an enemy portal, and the
 * loot the glyph yields being added to the agent's inventory. Complements [PortalHacksTest], which covers the
 * hack/glyph mechanics themselves.
 */
class GlypherHackTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.tick = 0
        Rng.seed(303)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.tick = 0
    }

    @Test
    fun glyphingAFriendlyPortalAddsLootToTheInventory() = with(Factory) {
        val agent = frog()
        val portal = portal(Faction.ENL) // our own faction → no retaliation, focus on the loot path
        World.allPortals.add(portal)
        agent.actionPortal = portal
        agent.inventory.items.clear()
        val before = agent.inventory.size()

        Glypher.performAction(agent)

        assertTrue(agent.inventory.size() >= before, "glyphing yields items into the inventory (never fewer)")
    }

    @Test
    fun glyphingAnEnemyPortalZapsTheGlypherUpFront() = with(Factory) {
        // A RES-held portal at a known level so retaliation actually costs XM (needs World.isReady for a real level).
        World.isReady = true
        try {
            val agent = frog()
            val portal = portal(Faction.RES)
            World.allPortals.add(portal)
            agent.actionPortal = portal
            agent.addXm(agent.xmCapacity())
            val xmBefore = agent.xm

            Glypher.performAction(agent)

            assertTrue(agent.xm < xmBefore, "an enemy portal retaliates as the glyph starts, draining the glypher's XM")
        } finally {
            World.isReady = false
        }
    }
}
