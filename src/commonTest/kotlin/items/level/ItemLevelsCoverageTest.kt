package items.level

import portal.Quality
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The weapon level enums [UltraStrikeLevel] and [XmpLevel]: the `valueOf(Int)` / `find(level, quality)` lookups
 * (both clip into 1..8), the per-level fields (damage / range / xmCost), the recycle-XM formula, and the
 * Ultra-Strike crit helpers.
 */
class ItemLevelsCoverageTest {

    @Test
    fun ultraStrikeValueOfClipsAndCarriesFields() {
        assertEquals(UltraStrikeLevel.ONE, UltraStrikeLevel.valueOf(0), "below 1 clips up to L1")
        assertEquals(UltraStrikeLevel.EIGHT, UltraStrikeLevel.valueOf(99), "above 8 clips down to L8")
        assertEquals(UltraStrikeLevel.FOUR, UltraStrikeLevel.valueOf(4))

        val l8 = UltraStrikeLevel.EIGHT
        assertEquals(8, l8.level)
        assertEquals(8, l8.toInt())
        assertEquals(2700, l8.damage)
        assertEquals(30, l8.rangeM)
        assertEquals(640, l8.xmCost)
        assertEquals(160, l8.calculateRecycleXm(), "recycle XM = level × 20")
        assertEquals(0.05, l8.critRate(), 1e-12)
        assertEquals(2700 * 3, l8.critDamage(), "crit damage triples base damage")
        assertTrue(l8.getColor().startsWith("#"), "each level renders a hex colour")
    }

    @Test
    fun ultraStrikeFindShiftsLevelByQuality() {
        // GOOD adds 0, BEST adds +2 (clipped at 8), MORE adds -1 (clipped at 1).
        assertEquals(UltraStrikeLevel.THREE, UltraStrikeLevel.find(3, Quality.GOOD))
        assertEquals(UltraStrikeLevel.EIGHT, UltraStrikeLevel.find(8, Quality.BEST), "already-max stays clipped at 8")
        assertEquals(UltraStrikeLevel.ONE, UltraStrikeLevel.find(1, Quality.MORE), "already-min stays clipped at 1")
        assertEquals(UltraStrikeLevel.FIVE, UltraStrikeLevel.find(4, Quality.TOP), "TOP adds +1")
    }

    @Test
    fun xmpValueOfClipsAndCarriesFields() {
        assertEquals(XmpLevel.ONE, XmpLevel.valueOf(-5), "below 1 clips up to L1")
        assertEquals(XmpLevel.EIGHT, XmpLevel.valueOf(1000), "above 8 clips down to L8")

        val l1 = XmpLevel.ONE
        assertEquals(1, l1.level)
        assertEquals(1, l1.toInt())
        assertEquals(150, l1.damage)
        assertEquals(42, l1.rangeM)
        assertEquals(50, l1.xmCost)
        assertEquals(20, l1.calculateRecycleXm(), "recycle XM = level × 20")
        assertTrue(l1.getColor().startsWith("#"))
    }

    @Test
    fun xmpFindShiftsLevelByQuality() {
        assertEquals(XmpLevel.SIX, XmpLevel.find(4, Quality.BEST), "BEST adds +2")
        assertEquals(XmpLevel.ONE, XmpLevel.find(2, Quality.MORE), "MORE adds -1")
        assertEquals(XmpLevel.EIGHT, XmpLevel.find(8, Quality.TOP), "TOP over 8 clips to 8")
    }

    @Test
    fun powerCubeLevelLookupsAndAccessors() {
        assertEquals(PowerCubeLevel.ONE, PowerCubeLevel.valueOf(0), "below 1 clips up to L1")
        assertEquals(PowerCubeLevel.EIGHT, PowerCubeLevel.valueOf(99), "above 8 clips down to L8")
        assertEquals(PowerCubeLevel.FIVE, PowerCubeLevel.find(4, Quality.TOP), "TOP adds +1")
        val l3 = PowerCubeLevel.THREE
        assertEquals(3, l3.toInt())
        assertEquals(3000, l3.calculateRecycleXm(), "recycle XM = the cube's XM value")
        assertTrue(l3.getColor().startsWith("#"), "each level renders a hex colour")
    }

    @Test
    fun resonatorLevelLookupsAndAccessors() {
        assertEquals(ResonatorLevel.ONE, ResonatorLevel.valueOf(-1), "below 1 clips up to L1")
        assertEquals(ResonatorLevel.EIGHT, ResonatorLevel.valueOf(99), "above 8 clips down to L8")
        assertEquals(ResonatorLevel.SIX, ResonatorLevel.find(4, Quality.BEST), "BEST adds +2")
        val l7 = ResonatorLevel.SEVEN
        assertEquals(7, l7.toInt())
        assertEquals(140, l7.calculateRecycleXm(), "recycle XM = level × 20")
        assertTrue(l7.getColor().startsWith("#"))
    }

    @Test
    fun portalLevelColourAndLookup() {
        assertEquals(PortalLevel.FOUR, PortalLevel.findByValue(4))
        assertEquals(4, PortalLevel.FOUR.toInt())
        assertTrue(PortalLevel.EIGHT.getColor().startsWith("#"), "a mapped level renders its hex colour")
        assertEquals("#FFFFFF", PortalLevel.ZERO.getColor(), "L0 has no map entry → the white fallback")
        // findByValue does NOT clip (unlike the weapon levels), so an out-of-range value hits the error path.
        assertFailsWith<IllegalStateException> { PortalLevel.findByValue(99) }
    }
}
