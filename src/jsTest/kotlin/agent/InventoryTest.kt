package agent

import Factory
import kotlin.test.Test
import kotlin.test.assertEquals

class InventoryTest {

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
