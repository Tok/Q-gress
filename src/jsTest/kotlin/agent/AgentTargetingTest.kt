package agent

import Factory
import portal.Portal
import util.data.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [Agent.enemyPortalsInRange] — the pure attack-targeting filter extracted from
 * `findPortalsInAttackRange` (PLAN non-functional track, phase B). It now takes the portal list as a
 * parameter (not the `World` singleton): non-friendly portals within range, nearest first. Friendly portals
 * are excluded; neutral (unowned) ones are attackable (matching the original `owner?.faction != faction`).
 */
class AgentTargetingTest {

    private fun portalAt(x: Int, y: Int, owner: Faction?): Portal =
        Portal.create(Pos(x, y)).also { p -> owner?.let { p.owner = Factory.agent(it) } }

    @Test
    fun returnsNonFriendlyPortalsInRangeNearestFirst() {
        val from = Pos(0, 0)
        val friendly = portalAt(0, 0, Faction.ENL) // excluded: same faction
        val neutralNear = portalAt(5, 0, null) // included: unowned is attackable
        val enemyNear = portalAt(10, 0, Faction.RES) // included
        val enemyFar = portalAt(10000, 0, Faction.RES) // excluded: out of range
        val portals = listOf(friendly, enemyNear, enemyFar, neutralNear)

        val inRange = Agent.enemyPortalsInRange(portals, Faction.ENL, from, attackDistance = 100.0)

        assertEquals(listOf(neutralNear, enemyNear), inRange, "nearest first, friendly + far excluded")
        assertTrue(!inRange.contains(friendly), "never targets a friendly portal")
        assertTrue(!inRange.contains(enemyFar), "never targets out of range")
    }

    @Test
    fun emptyWhenNothingIsInRange() {
        val far = portalAt(10000, 10000, Faction.RES)
        assertTrue(Agent.enemyPortalsInRange(listOf(far), Faction.ENL, Pos(0, 0), 50.0).isEmpty())
    }
}
