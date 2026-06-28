package agent.action.cond

import Factory
import World
import agent.Faction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Recruiting is a faction-NEUTRAL system process now (not an agent Q-action) — [Recruiter.expectedRecruits] is the
 * per-checkpoint rate that [system.Cycle] drives. These cover the anti-snowball shaping: the rate scales with the
 * smaller-roster [agent.Balance.recruitFactor] and falls to 0 as the roster fills toward its (size-scaled) cap.
 * The pure rate formula is unit-tested in [agent.BalanceMathTest].
 */
class RecruiterTest {

    @BeforeTest
    @AfterTest
    fun clean() {
        World.allAgents.clear()
    }

    private fun addAgents(faction: Faction, n: Int) = repeat(n) { World.allAgents.add(Factory.agent(faction)) }

    @Test
    fun theSmallerRosterRecruitsFaster() {
        addAgents(Faction.ENL, 5)
        addAgents(Faction.RES, 1)
        assertTrue(
            Recruiter.expectedRecruits(Faction.RES) > Recruiter.expectedRecruits(Faction.ENL),
            "the underdog's anti-snowball recruiting rate is higher than the leader's",
        )
    }

    @Test
    fun evenRostersRecruitAtTheSameRate() {
        addAgents(Faction.ENL, 4)
        addAgents(Faction.RES, 4)
        assertEquals(
            Recruiter.expectedRecruits(Faction.ENL),
            Recruiter.expectedRecruits(Faction.RES),
            1e-12,
            "even rosters → identical rate (recruitFactor 1.0 both sides)",
        )
    }
}
