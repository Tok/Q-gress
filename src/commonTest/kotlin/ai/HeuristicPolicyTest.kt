package ai

import Factory
import World
import agent.Faction
import config.Config
import util.Rng
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The hand-written [HeuristicPolicy] AI driver: it maps the live [Observation] to a [SliderVector] once per
 * scoring checkpoint (caching between), so `weight`/`currentVector` return sane 0..1 slider weights and
 * re-evaluate when the checkpoint advances. The pure mapping itself is [HeuristicTune] (tested separately).
 */
class HeuristicPolicyTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        Rng.seed(9)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
    }

    @Test
    fun weightsAreSaneSliderValues() = with(Factory) {
        World.allAgents.add(frog())
        World.allPortals.add(portal(Faction.ENL))
        val policy = HeuristicPolicy(Faction.ENL)
        SliderVector.ORDER.forEach { q ->
            val w = policy.weight(q)
            assertTrue(w in 0.0..1.0, "heuristic weight for ${q.id} is a 0..1 slider value (was $w)")
        }
        assertTrue(SliderVector.ORDER.all { policy.currentVector()[it] in 0.0..1.0 }, "the whole vector is 0..1")
    }

    @Test
    fun reEvaluatesWhenTheCheckpointAdvances() = with(Factory) {
        World.allAgents.add(smurf())
        val policy = HeuristicPolicy(Faction.RES)
        assertTrue(SliderVector.ORDER.all { policy.currentVector()[it].isFinite() }, "first eval is valid")
        World.tick = Config.ticksPerCheckpoint * 3 // cross a checkpoint boundary → re-evaluate
        World.allPortals.add(portal(Faction.RES)) // change the observed state so the re-tune can differ
        assertTrue(SliderVector.ORDER.all { policy.currentVector()[it].isFinite() }, "re-eval stays valid")
    }
}
