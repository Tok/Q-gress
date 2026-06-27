package agent

import Factory
import config.Config
import items.deployable.Resonator
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InventoryTest {

    @AfterTest
    fun restore() {
        Config.maxInventory = 2000
    }

    // --- inventory cap + recycle-for-space (the 2018-Ingress rule: a full inventory blocks hacking) ---

    @Test
    fun reportsFullAtTheCap() = with(Factory) {
        Config.maxInventory = 5
        val inv = inventory()
        repeat(4) { inv.addItem(Resonator.create(owner(), 1)) }
        assertFalse(inv.isFull(), "4 of 5 → not full")
        assertEquals(1, inv.freeSpace())
        inv.addItem(Resonator.create(owner(), 1))
        assertTrue(inv.isFull(), "at the cap → full")
        assertEquals(0, inv.freeSpace())
    }

    @Test
    fun recycleForSpaceFreesSlotsKeepsKeysAndReturnsXm() = with(Factory) {
        val inv = inventory()
        repeat(10) { inv.addItem(Resonator.create(owner(), 1)) }
        val key = portalKey()
        inv.addItem(key)
        val before = inv.size()
        val xm = inv.recycleForSpace(5)
        assertEquals(before - 5, inv.size(), "5 items recycled away")
        assertTrue(inv.findKeys().contains(key), "keys are kept (needed for linking)")
        assertTrue(xm > 0, "recycled resonators hand back some XM")
    }

    @Test
    fun countKeys() = with(Factory) {
        val inventory = inventory()
        val firstKey = portalKey()
        val secondKey = portalKey()
        inventory.addItem(firstKey)
        inventory.addItem(secondKey)
        inventory.addItem(secondKey)
        assertEquals(3, inventory.keyCount())
    }

    @Test
    fun countUniqueKeys() = with(Factory) {
        val inventory = inventory()
        val firstKey = portalKey()
        val secondKey = portalKey()
        inventory.addItem(firstKey)
        inventory.addItem(secondKey)
        inventory.addItem(secondKey)
        assertEquals(2, inventory.findUniqueKeys()?.count())
    }

    @Test
    fun findAndConsumeUltraStrikes() = with(Factory) {
        val inventory = inventory()
        val keptXmp = xmpBurster()
        val firstUs = ultraStrike()
        val secondUs = ultraStrike()
        inventory.addItem(keptXmp)
        inventory.addItem(firstUs)
        inventory.addItem(secondUs)
        assertEquals(2, inventory.findUltraStrikes().count())
        inventory.consumeUltraStrikes(listOf(firstUs))
        assertEquals(1, inventory.findUltraStrikes().count(), "only the consumed US is removed")
        assertEquals(1, inventory.findXmps().count(), "XMPs are untouched by consuming US")
    }
}
