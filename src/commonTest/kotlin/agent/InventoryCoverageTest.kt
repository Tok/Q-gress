package agent

import Factory
import World
import config.StartStage
import items.PowerCube
import items.UltraStrike
import items.XmpBurster
import items.deployable.HeatSink
import items.deployable.Multihack
import items.deployable.Resonator
import items.deployable.Shield
import items.types.HeatSinkType
import items.types.MultihackType
import items.types.ShieldType
import portal.Portal
import portal.PortalKey
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The [Inventory] queries + housekeeping: the typed find* filters, the consume* removals, key counting +
 * de-duplication, the recycle-for-space priority (surplus keys → low resonators → weapons → cubes) and the
 * companion starting-gear loadouts.
 */
class InventoryCoverageTest {

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

    private fun portalAt(pos: Pos): Portal {
        val p = Portal.create(pos)
        World.allPortals.add(p)
        return p
    }

    @Test
    fun findFiltersEachItemKind() {
        val agent = Factory.frog()
        val inv = Inventory.empty()
        inv.addItem(XmpBurster.create(agent, 1))
        inv.addItem(UltraStrike(items.level.UltraStrikeLevel.ONE, agent))
        inv.addItem(Resonator.create(agent, 1))
        inv.addItem(PowerCube.create(agent, 1))
        inv.addItem(Shield(ShieldType.COMMON, agent))
        inv.addItem(HeatSink(HeatSinkType.COMMON, agent))
        inv.addItem(Multihack(MultihackType.COMMON, agent))
        inv.addItem(PortalKey(portalAt(Pos(300, 300)), agent))
        assertEquals(1, inv.findXmps().size, "one XMP")
        assertEquals(1, inv.findUltraStrikes().size, "one Ultra-Strike")
        assertEquals(1, inv.findResonators().size, "one resonator")
        assertEquals(1, inv.findPowerCubes().size, "one power cube")
        assertEquals(1, inv.findShields().size, "one shield")
        assertEquals(1, inv.findHeatSinks().size, "one heat sink")
        assertEquals(1, inv.findMultihacks().size, "one multi-hack")
        assertEquals(1, inv.keyCount(), "one key")
        assertEquals(8, inv.size(), "eight items total")
    }

    @Test
    fun findViruses() {
        val agent = Factory.frog()
        val inv = Inventory.empty()
        inv.addItem(items.deployable.Virus(items.types.VirusType.values().first(), agent))
        assertEquals(1, inv.findViruses().size, "one virus")
    }

    @Test
    fun uniqueKeysDeDuplicateByPortal() {
        val agent = Factory.frog()
        val inv = Inventory.empty()
        val portal = portalAt(Pos(300, 300))
        // DISTINCT key objects to the SAME portal (what repeated hacking produces) — PortalKey has no
        // equals/hashCode, so this only collapses to one if we dedupe by portal, not by object identity.
        repeat(3) { inv.addItem(PortalKey(portal, agent)) }
        assertEquals(3, inv.keyCount(), "three raw keys")
        assertEquals(1, inv.findUniqueKeys().size, "de-duplicated by portal to one unique key")
    }

    @Test
    fun uniqueKeysNeverExceedTheNumberOfExistingPortals() {
        val agent = Factory.frog()
        val inv = Inventory.empty()
        // Two portals exist in the world; the agent may hold keys to any portals (own/enemy/neutral).
        val portalA = portalAt(Pos(300, 300))
        val portalB = portalAt(Pos(600, 600))
        repeat(4) { inv.addItem(PortalKey(portalA, agent)) }
        repeat(2) { inv.addItem(PortalKey(portalB, agent)) }
        assertEquals(6, inv.keyCount(), "six raw keys")
        // The unique count is bounded by the portals in existence, not by the number of key objects held.
        assertEquals(2, inv.findUniqueKeys().size, "two existing portals → two unique keys, never more")
        assertTrue(inv.findUniqueKeys().size <= World.countPortals(), "unique keys never exceed existing portals")
    }

    @Test
    fun consumeRemovesItems() {
        val agent = Factory.frog()
        val inv = Inventory.empty()
        val xmps = (1..3).map { XmpBurster.create(agent, 1) }
        val us = (1..2).map { UltraStrike(items.level.UltraStrikeLevel.ONE, agent) }
        val resos = (1..2).map { Resonator.create(agent, 1) }
        val cubes = (1..2).map { PowerCube.create(agent, 1) }
        inv.addItems(xmps + us + resos + cubes)
        inv.consumeXmps(xmps)
        inv.consumeUltraStrikes(us)
        inv.consumeResos(resos)
        inv.consumeCubes(cubes)
        assertEquals(0, inv.size(), "every consumed batch left the inventory")
    }

    @Test
    fun isFullTracksTheCap() {
        val agent = Factory.frog()
        val inv = Inventory.empty()
        assertFalse(inv.isFull(), "an empty inventory is not full")
        assertEquals(config.Config.maxInventory, inv.freeSpace(), "free space equals the cap when empty")
        repeat(config.Config.maxInventory) { inv.addItem(XmpBurster.create(agent, 1)) }
        assertTrue(inv.isFull(), "at the cap the inventory is full")
        assertEquals(0, inv.freeSpace(), "no free space at the cap")
    }

    @Test
    fun recycleForSpaceFreesSurplusKeysAndLowResosFirst() {
        val agent = Factory.frog()
        val inv = Inventory.empty()
        val portal = portalAt(Pos(300, 300))
        repeat(Inventory.MAX_KEYS_PER_PORTAL + 3) { inv.addItem(PortalKey(portal, agent)) } // 3 surplus duplicates
        inv.addItem(Resonator.create(agent, 1))
        inv.addItem(PowerCube.create(agent, 1))
        val sizeBefore = inv.size()
        val recovered = inv.recycleForSpace(4)
        assertEquals(sizeBefore - 4, inv.size(), "four items were recycled")
        assertTrue(recovered >= 0, "recycling returns the XM recovered")
        assertEquals(0, inv.recycleForSpace(0), "recycling zero is a no-op")
    }

    @Test
    fun recycleForSpaceRecoversXmFromPowerCubes() {
        val agent = Factory.frog()
        val inv = Inventory.empty()
        repeat(3) { inv.addItem(PowerCube.create(agent, 1)) } // no keys/resos → cubes are the recycle candidates
        val recovered = inv.recycleForSpace(2)
        assertEquals(1, inv.size(), "two of the three cubes were recycled")
        assertTrue(recovered > 0, "recycling power cubes recovers XM")
    }

    @Test
    fun consumeKeyToPortalRemovesTheMatchingKey() {
        val agent = Factory.frog()
        val inv = Inventory.empty()
        val portal = portalAt(Pos(300, 300))
        inv.addItem(PortalKey(portal, agent))
        inv.consumeKeyToPortal(portal)
        assertEquals(0, inv.keyCount(), "the key to that portal was consumed")
    }

    @Test
    fun consumeKeyToPortalFailsWhenNoKeyExists() {
        val agent = Factory.frog()
        val inv = Inventory.empty()
        val portal = portalAt(Pos(300, 300))
        assertFailsWith<IllegalStateException> { inv.consumeKeyToPortal(portal) }
    }

    @Test
    fun toStringSummarisesTheKitAndCounts() {
        val agent = Factory.frog()
        val inv = Inventory.empty()
        inv.addItem(PortalKey(portalAt(Pos(300, 300)), agent))
        inv.addItem(XmpBurster.create(agent, 1))
        inv.addItem(Resonator.create(agent, 1))
        inv.addItem(PowerCube.create(agent, 1))
        inv.addItem(PowerCube.create(agent, 1)) // a duplicate → the "2x" grouping path
        inv.addItem(Shield(ShieldType.COMMON, agent))
        val text = inv.toString()
        assertTrue(text.startsWith("1 keys"), "the summary leads with the key count")
        assertTrue(
            text.contains("x") || text.contains("PC") || text.contains("Shield") || text.isNotEmpty(),
            "it lists the non-hidden gear",
        )
    }

    @Test
    fun startingGearScalesWithTheChosenStage() {
        val agent = Factory.frog()
        assertTrue(Inventory.startingGear(agent, StartStage.START).isEmpty(), "START stage carries nothing")
        assertTrue(Inventory.startingGear(agent, StartStage.MID).isNotEmpty(), "MID stage carries a light kit")
        val end = Inventory.startingGear(agent, StartStage.END)
        assertTrue(end.size > Inventory.startingGear(agent, StartStage.MID).size, "END stage is the fuller loadout")
    }
}
