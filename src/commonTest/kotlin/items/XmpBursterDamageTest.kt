package items

import Factory
import World
import agent.Agent
import agent.Faction
import config.Dim
import items.deployable.Resonator
import items.deployable.Shield
import items.level.XmpLevel
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
import kotlin.test.assertTrue

/**
 * [XmpBurster.dealDamage] (per-hit resonator damage in range → summed total) and the [XmpBurster.knockMods]
 * companion (per-mod knock-out roll, [rng] injected for a deterministic outcome), plus the faction-agnostic
 * title blast ([XmpBurster.blastAt]).
 */
class XmpBursterDamageTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        Rng.seed(202)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
    }

    private val loc = Pos(400, 400)

    private fun enemyPortalAt(attacker: Faction): Portal {
        val enemy = Factory.agent(attacker.enemy())
        val portal = Portal.create(loc)
        portal.deploy(enemy, Octant.values().associateWith { Resonator.create(enemy, 1) }, Dim.minDeploymentRange.toInt())
        World.allPortals.add(portal)
        return portal
    }

    private fun attackerOnPortal(faction: Faction): Agent =
        if (faction == Faction.ENL) Agent.createFrog(Factory.grid(), loc) else Agent.createSmurf(Factory.grid(), loc)

    @Test
    fun dealDamageHitsEveryResoInRangeAndReturnsTheTotal() {
        val portal = enemyPortalAt(Faction.ENL)
        val agent = attackerOnPortal(Faction.ENL)
        val energyBefore = portal.filledSlots().sumOf { it.resonator?.energy ?: 0 }

        // A modest L1 burst: from the centre all 8 resos are in range but none is destroyed, so the energy
        // removed lines up exactly with the returned total (a full-drain hit would clamp at 0 and diverge).
        val burst = XmpBurster.create(agent, 1)
        val total = burst.dealDamage(agent)

        assertTrue(total > 0, "a burst on a portal full of resonators deals positive total damage")
        assertEquals(8, portal.numberOfResosLeft(), "an L1 burst chips the resonators but destroys none")
        val energyAfter = portal.filledSlots().sumOf { it.resonator?.energy ?: 0 }
        assertEquals(total, energyBefore - energyAfter, "the returned total equals the energy actually removed")
    }

    @Test
    fun dealDamageOutOfRangeDoesNothing() {
        enemyPortalAt(Faction.ENL)
        // The attacker stands far from the portal — no resonator is in range.
        val agent = Agent.createFrog(Factory.grid(), Pos(9000, 9000))
        val total = XmpBurster.create(agent, 1).dealDamage(agent)
        assertEquals(0, total, "no resonator in range → zero damage")
    }

    @Test
    fun knockModsStripsEveryModWhenTheRollAlwaysSucceeds() {
        val portal = enemyPortalAt(Faction.ENL)
        val enemy = requireNotNull(portal.owner)
        ModSlot.values().forEach { portal.mods[it] = Shield(ShieldType.COMMON, enemy) }
        // rng = { 0.0 } → the roll is always below any positive knock chance, so every point-blank mod pops out.
        val knocked = XmpBurster.knockMods(portal, loc, XmpLevel.EIGHT, ultra = true, enemy, rng = { 0.0 })
        assertEquals(ModSlot.values().size, knocked, "an always-succeeding roll knocks every mod off")
        assertEquals(0, portal.modCount(), "the portal is stripped of all mods")
    }

    @Test
    fun knockModsNeverStripsWhenTheRollAlwaysFails() {
        val portal = enemyPortalAt(Faction.ENL)
        val enemy = requireNotNull(portal.owner)
        ModSlot.values().forEach { portal.mods[it] = Shield(ShieldType.COMMON, enemy) }
        val knocked = XmpBurster.knockMods(portal, loc, XmpLevel.EIGHT, ultra = true, enemy, rng = { 0.999 })
        assertEquals(0, knocked, "an always-failing roll knocks nothing off")
        assertEquals(ModSlot.values().size, portal.modCount(), "every mod stays put")
    }

    @Test
    fun blastAtShattersResonatorsInRange() {
        val portal = enemyPortalAt(Faction.ENL)
        val agent = attackerOnPortal(Faction.ENL)
        val energyBefore = portal.filledSlots().sumOf { it.resonator?.energy ?: 0 }
        XmpBurster.blastAt(loc, XmpLevel.EIGHT, agent) // faction-agnostic title blast right on the portal
        val energyAfter = portal.filledSlots().sumOf { it.resonator?.energy ?: 0 }
        assertTrue(energyAfter < energyBefore, "the title blast damages every resonator within range")
    }
}
