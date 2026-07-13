package portal

import Factory
import World
import config.Config
import config.Dim
import config.Sim
import items.deployable.HeatSink
import items.deployable.Multihack
import items.deployable.Resonator
import items.deployable.Shield
import items.types.HeatSinkType
import items.types.MultihackType
import items.types.ShieldType
import system.grid.GridFixture
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The [Portal] lifecycle the board runs on: linking + field creation, mod deploy/strip, retaliation, the
 * decay / dominance-erosion attrition paths, destroy / remove / removeReso teardown (with the actionPortal
 * reassignment), the resonator-allocation + leak queries, and the companion factories.
 */
class PortalLifecycleTest {

    @BeforeTest
    fun reset() {
        Sim.roundField = false
        World.grid = GridFixture("PORTALLIFE", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        World.isReady = true
        Rng.seed(53)
    }

    @AfterTest
    fun tidy() {
        Sim.roundField = true
        World.grid = emptyMap()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        World.isReady = false
    }

    // An [owner]-faction portal at [pos], fully deployed with 8 resonators (so it has a real level + health).
    private fun deployedPortal(pos: Pos, owner: agent.Agent): Portal {
        val portal = Portal.create(pos)
        portal.deploy(owner, Octant.values().associateWith { Resonator.create(owner, 4) }, Dim.minDeploymentRange.toInt())
        World.allPortals.add(portal)
        return portal
    }

    // --- linking + fields ----------------------------------------------------

    @Test
    fun createLinkFormsALinkAndConsumesAKey() {
        val linker = Factory.frog()
        World.allAgents.add(linker)
        val a = deployedPortal(Pos(400, 400), linker)
        val b = deployedPortal(Pos(400, 900), linker)
        linker.inventory.items.add(PortalKey(b, linker))
        val keysBefore = linker.inventory.keyCount()
        a.createLink(linker, b)
        assertTrue(a.links.any { it.destination == b }, "the link A→B was created")
        assertEquals(keysBefore - 1, linker.inventory.keyCount(), "creating the link consumed the destination key")
    }

    @Test
    fun createLinkClosesAFieldOverSharedAnchors() {
        val linker = Factory.frog()
        World.allAgents.add(linker)
        val a = deployedPortal(Pos(400, 400), linker)
        val b = deployedPortal(Pos(400, 1000), linker)
        val c = deployedPortal(Pos(1000, 700), linker)
        repeat(2) { linker.inventory.items.add(PortalKey(c, linker)) }
        linker.inventory.items.add(PortalKey(b, linker))
        a.createLink(linker, c) // A→C
        b.createLink(linker, c) // B→C  → now A and B share anchor C
        a.createLink(linker, b) // A→B closes the A-B-C triangle
        assertTrue(a.fields.isNotEmpty(), "the third link closes a control field over the shared anchor")
        assertTrue(a.fields.first().calculateMu() > 0, "the field encloses real area (MU)")
    }

    // --- mods ----------------------------------------------------------------

    @Test
    fun deployAndStripMods() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = deployedPortal(Pos(500, 500), owner)
        assertTrue(portal.hasFreeModSlot(), "a fresh portal has free mod slots")
        owner.addXm(owner.xmCapacity())
        val shield = Shield(ShieldType.COMMON, owner)
        owner.inventory.items.add(shield)
        portal.deployMod(owner, shield)
        assertEquals(1, portal.modCount(), "the shield occupied a mod slot")
        assertFalse(owner.inventory.items.contains(shield), "the deployed shield left the inventory")

        val stripped = portal.stripMod(ModSlot.values().first { portal.mods.containsKey(it) }, owner)
        assertNotNull(stripped, "stripping returns the removed mod")
        assertEquals(0, portal.modCount(), "the slot is free again after stripping")
        assertNull(portal.stripMod(ModSlot.values().first(), owner), "stripping an empty slot returns null")
    }

    @Test
    fun heatSinkAndMultihackModsCostXm() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = deployedPortal(Pos(500, 500), owner)
        owner.addXm(owner.xmCapacity())
        val heatSink = HeatSink(HeatSinkType.COMMON, owner)
        val multihack = Multihack(MultihackType.COMMON, owner)
        owner.inventory.items.add(heatSink)
        owner.inventory.items.add(multihack)
        val xmBefore = owner.xm
        portal.deployMod(owner, heatSink) // clears the hack history on attach
        portal.deployMod(owner, multihack)
        assertEquals(2, portal.modCount(), "both mods slotted in")
        assertTrue(owner.xm < xmBefore, "deploying mods cost XM")
    }

    @Test
    fun heatSinkShortensTheCooldownFactor() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = deployedPortal(Pos(500, 500), owner)
        assertEquals(1.0, portal.cooldownFactor(), 1e-9, "no heat sink → full cooldown factor")
        owner.addXm(owner.xmCapacity())
        val heatSink = HeatSink(HeatSinkType.COMMON, owner)
        owner.inventory.items.add(heatSink)
        portal.deployMod(owner, heatSink)
        assertTrue(portal.cooldownFactor() < 1.0, "a heat sink reduces the hack-cooldown factor")
    }

    @Test
    fun linkingRangeAndStrongestResoQueries() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        // Level-1 resonators deploy 8-per-player, so all 8 octants fill → isFullyDeployed → a real linking range.
        val portal = Portal.create(Pos(500, 500))
        portal.deploy(owner, Octant.values().associateWith { Resonator.create(owner, 1) }, Dim.minDeploymentRange.toInt())
        World.allPortals.add(portal)
        val range = portal.calculateLinkingRangeInMeters().invoke()
        assertTrue(range > 0.0, "a fully-deployed portal has a positive linking range")
        assertNotNull(portal.findStrongestResoPos(), "a deployed portal reports a strongest-resonator position")
    }

    @Test
    fun decayToZeroDestroysThePortal() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        World.allPortals.add(Portal.create(Pos(900, 500))) // a survivor for the targeting reassignment
        val portal = Portal.create(Pos(500, 500))
        portal.deploy(owner, mapOf(Octant.N to Resonator.create(owner, 1)), Dim.minDeploymentRange.toInt())
        World.allPortals.add(portal)
        portal.filledSlots().forEach { it.resonator?.energy = 1 } // one hair from empty
        repeat(3) { portal.decay() } // decay drains it → the portal self-destructs
        assertNull(portal.owner, "a fully-decayed portal goes neutral")
        assertEquals(0, portal.numberOfResosLeft(), "no resonators remain")
    }

    @Test
    fun destroyDropsAnyDeployedMods() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        World.allPortals.add(Portal.create(Pos(900, 500))) // survivor for reassignment
        val portal = deployedPortal(Pos(500, 500), owner)
        owner.addXm(owner.xmCapacity())
        val shield = Shield(ShieldType.COMMON, owner)
        owner.inventory.items.add(shield)
        portal.deployMod(owner, shield)
        assertEquals(1, portal.modCount(), "a shield is deployed before destroy")
        portal.destroy()
        assertEquals(0, portal.modCount(), "destroy drops the deployed mods")
    }

    // --- retaliation ---------------------------------------------------------

    @Test
    fun anEnemyPortalRetaliatesAndDrainsXm() {
        val defender = Factory.smurf()
        World.allAgents.add(defender)
        val portal = deployedPortal(Pos(500, 500), defender)
        val attacker = Factory.frog()
        attacker.addXm(attacker.xmCapacity())
        val xmBefore = attacker.xm
        portal.retaliate(attacker)
        assertTrue(attacker.xm < xmBefore, "an enemy portal zaps the attacker for XM")
    }

    @Test
    fun aFriendlyPortalDoesNotRetaliate() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = deployedPortal(Pos(500, 500), owner)
        val ally = Factory.frog()
        ally.addXm(ally.xmCapacity())
        val xmBefore = ally.xm
        portal.retaliate(ally)
        assertEquals(xmBefore, ally.xm, "a same-faction portal never zaps an ally")
    }

    // --- attrition: decay + dominance erosion --------------------------------

    @Test
    fun decayBleedsResonatorEnergy() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = deployedPortal(Pos(500, 500), owner)
        val energyBefore = portal.filledSlots().sumOf { it.resonator?.energy ?: 0 }
        portal.decay()
        assertTrue(portal.filledSlots().sumOf { it.resonator?.energy ?: 0 } < energyBefore, "decay drains resonator energy")
    }

    @Test
    fun dominanceErosionHitsTheLeadingFaction() {
        val leader = Factory.frog()
        World.allAgents.add(leader)
        val portal = deployedPortal(Pos(500, 500), leader)
        // A field owned by ENL gives ENL all the MU → ENL leads → its own portals erode.
        val b = deployedPortal(Pos(500, 1000), leader)
        val c = deployedPortal(Pos(1100, 700), leader)
        val field = requireNotNull(Field.create(portal, b, c, leader)) { "field forms" }
        portal.fields.add(field)
        assertTrue(Config.dominanceDecay > 0.0, "the dominance lever is on")
        val energyBefore = portal.filledSlots().sumOf { it.resonator?.energy ?: 0 }
        portal.erodeByDominance()
        assertTrue(portal.filledSlots().sumOf { it.resonator?.energy ?: 0 } < energyBefore, "the runaway leader's resonators erode extra")
    }

    @Test
    fun aNeutralPortalDoesNotErode() {
        val portal = Portal.create(Pos(500, 500)) // no owner
        World.allPortals.add(portal)
        portal.erodeByDominance() // owner == null → early return, must not throw
        assertEquals(0, portal.numberOfResosLeft(), "a neutral portal stays neutral")
    }

    // --- teardown ------------------------------------------------------------

    @Test
    fun removeResoDownToZeroNeutralisesThePortal() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = Portal.create(Pos(500, 500))
        portal.deploy(owner, mapOf(Octant.N to Resonator.create(owner, 1)), Dim.minDeploymentRange.toInt())
        World.allPortals.add(portal)
        assertEquals(1, portal.numberOfResosLeft(), "one resonator deployed")
        portal.removeReso(Octant.N, owner)
        assertEquals(0, portal.numberOfResosLeft(), "removing the last resonator drops it")
        assertNull(portal.owner, "the portal decayed to neutral")
    }

    @Test
    fun destroyReassignsTargetingAgentsToARandomPortal() {
        val owner = Factory.smurf()
        World.allAgents.add(owner)
        // Survivors added first so the destroyed target sits at the last index — randomPortal (index in
        // [0, size-1)) can never re-pick it, so the reassignment provably lands on a live portal.
        World.allPortals.add(Portal.create(Pos(900, 500)))
        World.allPortals.add(Portal.create(Pos(1100, 700)))
        val target = deployedPortal(Pos(500, 500), owner)
        val agent = Factory.frog()
        agent.actionPortal = target
        World.allAgents.add(agent)
        target.destroy(Factory.frog())
        assertNull(target.owner, "a destroyed portal goes neutral")
        assertTrue(agent.actionPortal != target, "an agent that was targeting it is re-pointed at a live portal")
    }

    @Test
    fun removeShattersAndDropsKeysAndPullsItFromTheBoard() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = deployedPortal(Pos(500, 500), owner)
        World.allPortals.add(Portal.create(Pos(900, 500))) // a survivor so post-remove targeting has somewhere to go
        val holder = Factory.smurf()
        holder.inventory.items.add(PortalKey(portal, holder))
        World.allAgents.add(holder)
        portal.remove()
        assertFalse(World.allPortals.contains(portal), "remove pulls the portal off the board")
        assertTrue(holder.inventory.findKeys().none { it.portal == portal }, "keys to a removed portal are discarded")
    }

    // --- queries -------------------------------------------------------------

    @Test
    fun allowedResoLevelsEmptyForAnEnemyPortal() {
        val owner = Factory.smurf()
        World.allAgents.add(owner)
        val portal = deployedPortal(Pos(500, 500), owner)
        val frog = Factory.frog()
        assertTrue(portal.findAllowedResoLevels(frog).isEmpty(), "an enemy can't deploy → no allowed reso levels")
        assertTrue(portal.findAllowedResoLevels(owner).isNotEmpty(), "the owner has allowed reso levels")
    }

    @Test
    fun findChargeableMatchesHeldKeysToDamagedFriendlyPortals() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = deployedPortal(Pos(500, 500), owner)
        portal.filledSlots().forEach { it.resonator?.energy = 10 } // damaged → health ≤ 90
        val keys = listOf(PortalKey(portal, owner))
        assertTrue(Portal.findChargeable(owner, keys).contains(portal), "the badly-hurt friendly portal is chargeable via its key")
        val atPortal = owner.copy(pos = portal.location)
        atPortal.actionPortal = portal // standing at + working the portal — the keyless casual top-up
        assertTrue(
            Portal.findChargeable(atPortal, emptyList()).contains(portal),
            "an agent at its action portal needs no key to recharge it",
        )
    }

    @Test
    fun leakXmScalesWithLevel() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = deployedPortal(Pos(500, 500), owner)
        val (pos, leaked) = portal.leakXm()
        assertEquals(portal.location, pos, "the leak is at the portal")
        assertTrue(leaked > 0, "a levelled portal leaks XM")
    }

    @Test
    fun healthReflectsResonatorEnergy() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val full = deployedPortal(Pos(500, 500), owner)
        assertTrue(full.calcHealth() > 0, "a deployed portal has health")
        assertEquals(0, Portal.create(Pos(900, 900)).calcHealth(), "a neutral portal reads 0 health")
    }

    @Test
    fun findConnectedPortalsSpansLinksBothWays() {
        val linker = Factory.frog()
        World.allAgents.add(linker)
        val a = deployedPortal(Pos(400, 400), linker)
        val b = deployedPortal(Pos(400, 900), linker)
        linker.inventory.items.add(PortalKey(b, linker))
        a.createLink(linker, b)
        assertTrue(a.findConnectedPortals().contains(b), "A is connected to B (outgoing)")
        assertTrue(b.findConnectedPortals().contains(a), "B is connected to A (incoming)")
    }

    // --- companion factories -------------------------------------------------

    @Test
    fun nearestToPicksTheClosestPortal() {
        val near = Portal.create(Pos(400, 400))
        val far = Portal.create(Pos(1600, 400))
        World.allPortals.add(near)
        World.allPortals.add(far)
        assertEquals(near, Portal.nearestTo(Pos(300, 400)), "the nearest portal by distance is returned")
        World.allPortals.clear()
        assertNull(Portal.nearestTo(Pos(0, 0)), "no portals → null")
    }

    @Test
    fun createRandomPlacesAPortalOnTheGrid() {
        repeat(3) { World.allPortals.add(Portal.createRandom()) } // spread + best-candidate branches
        assertEquals(3, World.allPortals.size, "createRandom spreads several portals across the grid")
    }

    @Test
    fun uniqueNameAvoidsCollisionsAcrossTheBoard() {
        val a = Portal.create(Pos(300, 300))
        World.allPortals.add(a)
        val b = Portal.create(Pos(600, 600))
        World.allPortals.add(b)
        assertTrue(a.name != b.name, "two portals never share a name")
    }

    @Test
    fun coordinateAccessorsAndDeprecation() {
        val portal = Portal.create(Pos(320, 480))
        assertEquals(320.0, portal.x(), "x() reads the location")
        assertEquals(480.0, portal.y(), "y() reads the location")
        assertFalse(portal.isDeprecated(), "a portal with slots is not deprecated")
    }
}
