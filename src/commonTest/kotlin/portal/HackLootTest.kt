package portal

import Factory
import util.Rng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression for the [HackLoot] drop table extracted out of Portal (PLAN phase B SoC split). The roll is a
 * pure function of the seeded [Rng] + the DropRates/Config tunables, so the same seed must reproduce the same
 * drops — and a high-level hack must actually yield loot.
 */
class HackLootTest {

    @Test
    fun rollDropsIsDeterministicForASeed() = with(Factory) {
        val hacker = frog()
        Rng.seed(42)
        val first = HackLoot.rollDrops(hacker, level = 8)
        Rng.seed(42)
        val second = HackLoot.rollDrops(hacker, level = 8)
        assertEquals(first.size, second.size, "same seed → identical drop count")
    }

    @Test
    fun highLevelHacksYieldLoot() = with(Factory) {
        val hacker = frog()
        // Robust against any single unlucky seed: a level-8 hack drops *something* across a handful of seeds.
        val total = (1..5).sumOf { seed ->
            Rng.seed(seed)
            HackLoot.rollDrops(hacker, level = 8).size
        }
        assertTrue(total > 0, "level-8 hacks should drop items")
    }
}
