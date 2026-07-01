package agent.action.cond

import Factory
import World
import agent.Agent
import agent.Faction
import config.Dim
import items.UltraStrike
import items.XmpBurster
import items.deployable.Resonator
import items.deployable.Shield
import items.level.UltraStrikeLevel
import items.types.ShieldType
import portal.ModSlot
import portal.Octant
import portal.Portal
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The actual assault ([Attacker.performAction]): the sustained volley loop ([fireVolley] — XM drain, resonator
 * damage, XMP consumption), the up-front [stripShields] Ultra-Strike salvo that knocks mods off, and the loop
 * terminating the instant the pinned target falls (all resonators destroyed). Complements [AttackerTest], which
 * only covers the gates ([isActionPossible]/[isTargetValid]).
 */
class AttackerAssaultTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.tick = 0
        Rng.seed(101)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.tick = 0
    }

    private val loc = Pos(500, 500)

    // A fully-deployed enemy portal at [loc], its 8 level-1 resonators at min deploy range so an attacker
    // standing on the portal centre has them all inside XMP range.
    private fun enemyPortal(attacker: Faction): Portal {
        val enemy = Factory.agent(attacker.enemy())
        val portal = Portal.create(loc)
        portal.deploy(enemy, Octant.values().associateWith { Resonator.create(enemy, 1) }, Dim.minDeploymentRange.toInt())
        World.allPortals.add(portal)
        return portal
    }

    // An attacker standing ON the portal centre, stocked with [xmps] level-1 XMPs and topped-up XM.
    private fun attackerOnPortal(faction: Faction, portal: Portal, xmps: Int): Agent {
        val agent = if (faction == Faction.ENL) Agent.createFrog(Factory.grid(), loc) else Agent.createSmurf(Factory.grid(), loc)
        agent.inventory.items.clear()
        repeat(xmps) { agent.inventory.items.add(XmpBurster.create(agent, 1)) }
        agent.addXm(agent.xmCapacity())
        agent.actionPortal = portal
        return agent
    }

    private fun totalResoEnergy(portal: Portal) = portal.filledSlots().sumOf { it.resonator?.energy ?: 0 }

    @Test
    fun assaultDamagesResosDrainsXmAndConsumesXmps() = with(Factory) {
        Faction.values().forEach { faction ->
            reset()
            val portal = enemyPortal(faction)
            val agent = attackerOnPortal(faction, portal, xmps = 40)
            val energyBefore = totalResoEnergy(portal)
            val xmpsBefore = agent.inventory.findXmps().count()
            val xmBefore = agent.xm

            Attacker.performAction(agent)

            assertTrue(totalResoEnergy(portal) < energyBefore, "the volleys drained resonator energy")
            assertTrue(agent.inventory.findXmps().count() < xmpsBefore, "firing consumed XMPs")
            assertTrue(agent.xm < xmBefore, "firing cost XM")
        }
    }

    @Test
    fun assaultFellsAWeakPortalAndThenStops() = with(Factory) {
        val portal = enemyPortal(Faction.ENL) // an ENL attacker vs a RES-held portal
        // Cripple every resonator so a single sustained assault finishes it — exercises the loop-termination path.
        portal.filledSlots().forEach { it.resonator?.energy = 5 }
        val agent = attackerOnPortal(Faction.ENL, portal, xmps = 40)

        Attacker.performAction(agent)

        assertEquals(0, portal.numberOfResosLeft(), "the crippled portal falls — every resonator destroyed")
        assertFalse(Attacker.isTargetValid(agent, portal), "a felled portal is no longer a valid target → loop stops")
        assertTrue(agent.inventory.findXmps().count() < 40, "some XMPs were spent taking it down")
    }

    @Test
    fun stripShieldsKnocksModsOffFirst() = with(Factory) {
        val portal = enemyPortal(Faction.ENL)
        val enemy = requireNotNull(portal.owner) { "the enemy portal has an owner" }
        // Four common (stickiness-0) shields — the up-front Ultra-Strike salvo should knock some off.
        ModSlot.values().forEach { portal.mods[it] = Shield(ShieldType.COMMON, enemy) }
        val modsBefore = portal.modCount()
        val agent = attackerOnPortal(Faction.ENL, portal, xmps = 40)
        repeat(12) { agent.inventory.items.add(UltraStrike(UltraStrikeLevel.ONE, agent)) }

        Attacker.performAction(agent)

        assertTrue(portal.modCount() < modsBefore, "the Ultra-Strike salvo knocked shields off the portal")
        assertTrue(agent.inventory.findUltraStrikes().count() < 12, "the salvo consumed Ultra-Strikes")
    }
}
