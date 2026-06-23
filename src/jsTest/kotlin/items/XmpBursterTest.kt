package items

import Factory
import items.deployable.Shield
import items.level.XmpLevel
import items.types.ShieldType
import portal.ModSlot
import util.data.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Knocking slotted items out of a portal (XMP + Ultra-Strike) + that shield mitigation registers. */
class XmpBursterTest {

    @Test
    fun ultraStrikeKnocksAModOut() = with(Factory) {
        val agent = agent()
        val portal = portal()
        portal.mods[ModSlot.values().first()] = Shield(ShieldType.COMMON, agent)
        // Blast at the portal centre with an rng that always succeeds → the mod is stripped.
        val knocked = XmpBurster.knockMods(portal, portal.location, XmpLevel.EIGHT, ultra = true, agent) { 0.0 }
        assertEquals(1, knocked)
        assertTrue(portal.mods.isEmpty(), "the shield should be knocked off")
    }

    @Test
    fun aHighRollLeavesTheModSlotted() = with(Factory) {
        val agent = agent()
        val portal = portal()
        portal.mods[ModSlot.values().first()] = Shield(ShieldType.AEGIS, agent)
        // rng above the (low, sticky AEGIS) chance → nothing knocked.
        val knocked = XmpBurster.knockMods(portal, portal.location, XmpLevel.EIGHT, ultra = true, agent) { 0.999 }
        assertEquals(0, knocked)
        assertEquals(1, portal.mods.size)
    }

    @Test
    fun outOfRangeKnocksNothing() = with(Factory) {
        val agent = agent()
        val portal = portal()
        portal.mods[ModSlot.values().first()] = Shield(ShieldType.COMMON, agent)
        val faraway = Pos(portal.location.x + 100_000, portal.location.y)
        val knocked = XmpBurster.knockMods(portal, faraway, XmpLevel.EIGHT, ultra = true, agent) { 0.0 }
        assertEquals(0, knocked)
        assertEquals(1, portal.mods.size)
    }

    @Test
    fun aShieldRegistersMitigation() = with(Factory) {
        val agent = agent()
        val portal = portal()
        assertEquals(0, portal.totalMitigation())
        portal.mods[ModSlot.values().first()] = Shield(ShieldType.COMMON, agent)
        assertEquals(ShieldType.COMMON.mitigation, portal.totalMitigation())
    }
}
