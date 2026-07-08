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

    @Test
    fun modTypeColoursComeFromRarityAndFallBackToWhite() {
        // The rarity-derived colour getter on each LeveledColor mod type.
        assertEquals(Rarity.COMMON.color, ShieldType.COMMON.color, "a shield's colour is its rarity colour")
        assertEquals(Rarity.VERY_RARE.color, MultihackType.VERY_RARE.color)

        // getColorForLevel → the matching entry's colour, or NO_MOD_COLOR when no level matches (both branches
        // of LeveledColor.colorForLevel).
        assertEquals(ShieldType.RARE.color, ShieldType.getColorForLevel(2), "level 2 → the RARE shield colour")
        assertEquals(NO_MOD_COLOR, ShieldType.getColorForLevel(99), "no such level → the white fallback")
        assertEquals(MultihackType.COMMON.color, MultihackType.getColorForLevel(1))
        assertEquals(NO_MOD_COLOR, MultihackType.getColorForLevel(0), "no such level → the white fallback")
    }
}
