package items.types

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The two mod-catalogue enums: [ModType] (the display labels used in the HUD) and [LinkAmpType] (the
 * inactive-but-defined link-amp tiers with their level / abbreviation / rarity). Each value's real fields
 * are asserted, not merely touched.
 */
class ModTypeLinkAmpTypeTest {

    @Test
    fun modTypeLabelsAreTheHudStrings() {
        assertEquals("Shield", ModType.RES_SHIELD.label)
        assertEquals("Multi-hack", ModType.MULTIHACK.label)
        assertEquals("Force Amp", ModType.FORCE_AMP.label)
        assertEquals("Heat Sink", ModType.HEATSINK.label)
        assertEquals("Turret", ModType.TURRET.label)
        assertEquals("Link Amp", ModType.LINK_AMPLIFIER.label)
        assertEquals(6, ModType.values().size, "six mod types are catalogued")
        assertEquals(ModType.TURRET, ModType.valueOf("TURRET"), "valueOf round-trips")
    }

    @Test
    fun linkAmpTypesCarryLevelAbbrAndRarity() {
        assertEquals(1, LinkAmpType.RARE.level)
        assertEquals("LA", LinkAmpType.RARE.abbr)
        assertEquals(Rarity.RARE, LinkAmpType.RARE.rarity)

        assertEquals(2, LinkAmpType.VERY_RARE.level)
        assertEquals("VRLA", LinkAmpType.VERY_RARE.abbr)
        assertEquals(Rarity.VERY_RARE, LinkAmpType.VERY_RARE.rarity)

        assertEquals(3, LinkAmpType.SBUL.level)
        assertEquals("SBUL", LinkAmpType.SBUL.abbr)
        assertEquals(Rarity.VERY_RARE, LinkAmpType.SBUL.rarity)

        // Levels strictly ascend across the three tiers.
        val levels = LinkAmpType.values().map { it.level }
        assertEquals(levels.sorted(), levels, "link-amp levels ascend with rarity")
        assertTrue(LinkAmpType.values().all { it.abbr.isNotBlank() }, "every tier has an abbreviation")
    }
}
