package agent.action.cond

import Factory
import World
import agent.Faction
import config.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Characterization tests (PLAN non-functional track, phase A) for [Recruiter]'s pure, anti-snowball
 * selection weighting: a fixed [Config.recruitWeight] base scaled by [agent.Balance.recruitFactor], so the
 * SMALLER roster weighs recruiting more heavily. The walk-up/meeting/[Recruiter.resolve] flow has effects
 * (RNG, sound, World mutation) and is left for the integration layer.
 */
class RecruiterTest {

    @BeforeTest
    @AfterTest
    fun clean() {
        World.allAgents.clear()
    }

    private fun addAgents(faction: Faction, n: Int) = repeat(n) { World.allAgents.add(Factory.agent(faction)) }

    @Test
    fun selectionWeightIsTheBaseWhenRostersAreEven() {
        addAgents(Faction.ENL, 3)
        addAgents(Faction.RES, 3)
        // recruitFactor == (theirs+1)/(mine+1) == 1.0 when even → weight == the base.
        assertEquals(Config.recruitWeight, Recruiter.selectionWeight(Faction.ENL), 1e-12)
        assertEquals(Config.recruitWeight, Recruiter.selectionWeight(Faction.RES), 1e-12)
    }

    @Test
    fun theSmallerRosterWeighsRecruitingMoreThanTheLargerOne() {
        addAgents(Faction.ENL, 5)
        addAgents(Faction.RES, 1)
        assertTrue(
            Recruiter.selectionWeight(Faction.RES) > Recruiter.selectionWeight(Faction.ENL),
            "the underdog recruits more eagerly than the leader",
        )
    }

    @Test
    fun selectionWeightTracksRecruitFactorExactly() {
        addAgents(Faction.ENL, 5)
        addAgents(Faction.RES, 1)
        Faction.values().forEach { faction ->
            assertEquals(
                Config.recruitWeight * agent.Balance.recruitFactor(faction),
                Recruiter.selectionWeight(faction),
                1e-12,
                "weight is exactly base × recruitFactor for $faction",
            )
        }
    }
}
