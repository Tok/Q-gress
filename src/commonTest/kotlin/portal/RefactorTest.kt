package portal

import Factory
import World
import agent.Agent
import agent.Faction
import agent.action.cond.Refactorer
import config.Dim
import config.Time
import items.deployable.Resonator
import items.deployable.Virus
import items.types.VirusType
import util.data.Pos
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The virus flip rules: the **item type** ([VirusType.flipsTo]) decides the result faction — JARVIS→ENL,
 * ADA→RES — independent of who uses it. Either faction may carry/use either item, only faction-owned
 * portals flip (never neutral), a portal can't flip to the colour it already is, and a flipped portal is
 * immune for [Portal] flip-immunity window. Resonators stay; links/fields are torn down ([LinkFieldIntegrityTest]).
 */
class RefactorTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
    }

    private fun portalAt(x: Int, y: Int): Portal = Portal.create(Pos(x.toDouble(), y.toDouble())).also { World.allPortals.add(it) }

    private fun owned(x: Int, y: Int, faction: Faction): Portal {
        val ownerAgent = Factory.agent(faction).also { World.allAgents.add(it) }
        return portalAt(x, y).also {
            it.owner = ownerAgent
            it.slots.getValue(Octant.N).deployReso(ownerAgent, Resonator.create(ownerAgent, 1), Dim.maxDeploymentRange.toInt())
        }
    }

    private fun agentAt(faction: Faction, portal: Portal, vararg viruses: VirusType): Agent {
        val agent = Factory.agent(faction).also { World.allAgents.add(it) }
        agent.actionPortal = portal
        viruses.forEach { agent.inventory.items.add(Virus(it, agent)) }
        return agent
    }

    @Test
    fun jarvisFlipsBluePortalToGreen() {
        val portal = owned(0, 0, Faction.RES)
        val frog = agentAt(Faction.ENL, portal, VirusType.JARVIS_VIRUS)
        assertTrue(Refactorer.isActionPossible(frog), "a JARVIS on an enemy RES portal is usable")
        Refactorer.performAction(frog)
        assertEquals(Faction.ENL, portal.owner?.faction, "JARVIS always flips a portal to ENL")
    }

    @Test
    fun adaFlipsGreenPortalToBlue() {
        val portal = owned(0, 0, Faction.ENL)
        val smurf = agentAt(Faction.RES, portal, VirusType.ADA_REFACTOR)
        assertTrue(Refactorer.isActionPossible(smurf), "an ADA on an enemy ENL portal is usable")
        Refactorer.performAction(smurf)
        assertEquals(Faction.RES, portal.owner?.faction, "ADA always flips a portal to RES")
    }

    @Test
    fun flipDirectionFollowsItemNotUser() {
        // A RES agent holding a JARVIS (the ENL item) flips an enemy ENL portal — but JARVIS→ENL, so it
        // can't flip a green portal at all; it CAN flip the blue one it doesn't own. Direction is the item's.
        val greenPortal = owned(0, 0, Faction.ENL)
        val smurfWithJarvis = agentAt(Faction.RES, greenPortal, VirusType.JARVIS_VIRUS)
        assertFalse(Refactorer.isActionPossible(smurfWithJarvis), "JARVIS can't flip an already-green portal")
    }

    @Test
    fun friendlyFlipHandsOwnPortalToTheEnemyFaction() {
        // An ENL agent ADA-flips its OWN green portal to RES (dual-use: e.g. to shed a blocking link). The
        // portal becomes RES-owned; a RES agent must exist on the board to take it.
        val portal = owned(0, 0, Faction.ENL)
        World.allAgents.add(Factory.agent(Faction.RES)) // a smurf to receive the flipped portal
        val frog = agentAt(Faction.ENL, portal, VirusType.ADA_REFACTOR)
        val resosBefore = portal.numberOfResosLeft()
        assertTrue(Refactorer.isActionPossible(frog), "an ENL agent's ADA can flip its OWN green portal")
        Refactorer.performAction(frog)
        assertEquals(Faction.RES, portal.owner?.faction, "the friendly-flipped portal becomes RES")
        assertEquals(resosBefore, portal.numberOfResosLeft(), "resonators survive a flip (only links/fields go)")
    }

    @Test
    fun neutralPortalIsNotFlippable() {
        val portal = portalAt(0, 0) // ownerless / neutral
        val frog = agentAt(Faction.ENL, portal, VirusType.JARVIS_VIRUS, VirusType.ADA_REFACTOR)
        assertFalse(Refactorer.isActionPossible(frog), "a neutral portal can't be flipped by any virus")
    }

    @Test
    fun cannotFlipToTheColourItAlreadyIs() {
        val portal = owned(0, 0, Faction.ENL)
        val frog = agentAt(Faction.ENL, portal, VirusType.JARVIS_VIRUS) // JARVIS→ENL, portal already ENL
        assertFalse(Refactorer.isActionPossible(frog), "no-op flip (green→green) is rejected")
    }

    @Test
    fun flipImmunityBlocksReflipUntilWindowElapses() {
        val portal = owned(0, 0, Faction.ENL)
        portal.refactor(Factory.smurf(), Faction.RES)
        assertFalse(portal.isFlippable(), "just-flipped portal is immune")
        World.tick += Time.secondsToTicks(3600) - 1
        assertFalse(portal.isFlippable(), "still immune one tick before the window closes")
        World.tick += 1
        assertTrue(portal.isFlippable(), "flippable again once the 1h immunity elapses")
    }
}
