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
}
