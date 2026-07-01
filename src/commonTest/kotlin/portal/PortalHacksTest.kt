package portal

import Factory
import World
import agent.Faction
import util.Rng
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The hack / glyph subsystem ([PortalHacks], reached via [Portal.canHack]/[Portal.tryHack]/[Portal.tryGlyph]):
 * drops on a fresh hack, the per-hack time cooldown, burnout after [Portal.MAX_HACKS] hacks in the window, and
 * the XM/AP cost split (enemy hack earns AP + costs more XM than hacking your own). Ticks == seconds
 * ([config.Time]), and a hack's cooldown clears once > ~290 ticks have passed.
 */
class PortalHacksTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        Rng.seed(1)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
    }

    // A RES-owned portal an ENL frog can hack as an enemy target.
    private fun enemyPortalAndHacker() = with(Factory) {
        val portal = portal(Faction.RES)
        val hacker = frog().also { it.actionPortal = portal }
        portal to hacker
    }

    @Test
    fun firstHackYieldsItemsAndNoCooldown() {
        val (portal, hacker) = enemyPortalAndHacker()
        assertTrue(portal.canHack(hacker), "a never-hacked portal is immediately hackable")
        val result = portal.tryHack(hacker)
        assertNull(result.cooldown, "a fresh hack isn't on cooldown")
        assertNotNull(result.items, "a fresh hack returns a (possibly empty) item list")
    }

    @Test
    fun immediateReHackIsOnCooldown() {
        val (portal, hacker) = enemyPortalAndHacker()
        portal.tryHack(hacker) // tick 0
        assertFalse(portal.canHack(hacker), "hacking again on the same tick is blocked by the cooldown")
        assertNotNull(portal.tryHack(hacker).cooldown, "the re-hack reports a cooldown, drops nothing")
    }

    @Test
    fun cooldownClearsAfterTheWindow() {
        val (portal, hacker) = enemyPortalAndHacker()
        portal.tryHack(hacker) // tick 0
        World.tick = 300 // the full 300s base cooldown has elapsed (valueOf(0) == NONE) → hackable again
        assertTrue(portal.canHack(hacker), "once the cooldown window passes the portal is hackable again")
    }

    @Test
    fun burnsOutAfterMaxHacksInTheWindow() {
        val (portal, hacker) = enemyPortalAndHacker()
        // Space the hacks 300 ticks apart so each clears the time-cooldown, but all stay inside the burnout window.
        repeat(Portal.MAX_HACKS) { i ->
            World.tick = i * 300
            assertTrue(portal.canHack(hacker), "hack ${i + 1} of ${Portal.MAX_HACKS} is allowed")
            portal.tryHack(hacker)
        }
        assertFalse(portal.canHack(hacker), "MAX_HACKS hacks inside the burnout window → burned out")
        assertEquals(Cooldown.BURNOUT, portal.tryHack(hacker).cooldown, "the over-hack reports BURNOUT")
    }

    @Test
    fun enemyHackEarnsApAndCostsXm() {
        val (portal, hacker) = enemyPortalAndHacker()
        hacker.addXm(hacker.xmCapacity()) // top up so the XM cost is visible (not clipped at 0)
        val apBefore = hacker.ap
        val xmBefore = hacker.xm
        portal.tryHack(hacker)
        assertTrue(hacker.ap > apBefore, "hacking an ENEMY portal earns AP")
        assertTrue(hacker.xm < xmBefore, "hacking costs XM")
    }

    @Test
    fun friendlyHackEarnsNoAp() {
        val portal = Factory.portal(Faction.ENL) // owned by our own faction
        val hacker = Factory.frog().also { it.actionPortal = portal }
        val apBefore = hacker.ap
        portal.tryHack(hacker)
        assertEquals(apBefore, hacker.ap, "hacking your OWN portal earns no AP")
    }

    @Test
    fun glyphYieldsAtLeastAsMuchAsAPlainHack() {
        // Same RNG seed + a fresh identical target for each, so the glyph's extra draw(s) can only add.
        Rng.seed(42)
        val (hackPortal, hacker) = enemyPortalAndHacker()
        val hackItems = requireNotNull(hackPortal.tryHack(hacker).items).size

        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        Rng.seed(42)
        val (glyphPortal, glypher) = enemyPortalAndHacker()
        val glyphItems = requireNotNull(glyphPortal.tryGlyph(glypher).items).size

        assertTrue(glyphItems >= hackItems, "a glyph hack draws extra loot, so never fewer items than a plain hack")
    }
}
