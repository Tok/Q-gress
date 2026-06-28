package agent

import Factory
import World
import agent.action.ActionItem
import config.Sim
import system.grid.GridFixture
import util.Rng
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The "agents never idle" guarantee (the title-screen stuck-and-waiting fix): with portals on the board, an
 * agent with nothing portal-productive to do heads out to FIND a portal (a MOVE) — it never parks in WAIT or
 * aimlessly wanders (EXPLORE). (The board always has portals in practice — minPortals — so the bare-wander
 * last resort isn't exercised here.)
 */
class NoIdleTest {
    @BeforeTest
    fun setUp() {
        Sim.roundField = false
        World.grid = GridFixture("IDLE", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.allPortals.clear()
        World.allPortals.add(Factory.portal())
        World.allPortals.add(Factory.portal())
    }

    @AfterTest
    fun tearDown() {
        World.allPortals.clear()
        World.grid = emptyMap()
    }

    @Test
    fun idleAgentSeeksAPortalNeverWaits() {
        // Across many seeds the no-portal-work fallback always resolves to a portal-seeking MOVE — never the
        // idle WAIT, never an aimless EXPLORE.
        repeat(50) { i ->
            Rng.seed(i + 1)
            val item = Factory.agent().moveElsewhere().action.item
            assertTrue(item == ActionItem.MOVE, "idle agent heads for a portal (MOVE), not ${item.qName} (seed ${i + 1})")
        }
    }
}
