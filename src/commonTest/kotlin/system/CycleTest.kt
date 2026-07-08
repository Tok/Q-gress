package system

import World
import config.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The checkpoint cadence in [Cycle.updateCheckpoints]: a checkpoint is recorded every
 * [Config.ticksPerCheckpoint] ticks, and the one at a scoring-cycle boundary is flagged [Checkpoint.isCycleEnd]
 * (which drives the deeper cycle sound). Runs headless on an empty world through the [system.audio.Snd] /
 * [system.effect.Fx] no-op sinks. The roster-churn branches (removeFrogs/removeSmurfs/factionChange) are gated
 * by the `const val 0.0` quit/defection rates — dead by design ("raise to re-enable"), so not exercised here.
 */
class CycleTest {

    @BeforeTest
    fun clean() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        Cycle.INSTANCE.checkpoints.clear()
        World.tick = 0
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        Cycle.INSTANCE.checkpoints.clear()
        World.tick = 0
    }

    @Test
    fun aMidCycleCheckpointIsRecordedButNotFlaggedAsCycleEnd() {
        val tick = Config.ticksPerCheckpoint // the first checkpoint — not a cycle boundary
        Cycle.updateCheckpoints(tick, enlMu = 100, resMu = 50)
        val cp = requireNotNull(Cycle.INSTANCE.checkpoints[tick]) { "a checkpoint was recorded" }
        assertEquals(100, cp.enlMu)
        assertEquals(50, cp.resMu)
        assertFalse(cp.isCycleEnd, "a mid-cycle checkpoint is not a cycle end")
    }

    @Test
    fun theScoringCycleEndCheckpointIsFlagged() {
        val tick = Config.checkpointsPerCycle * Config.ticksPerCheckpoint // the scoring-cycle boundary
        Cycle.updateCheckpoints(tick, enlMu = 80, resMu = 120)
        val cp = requireNotNull(Cycle.INSTANCE.checkpoints[tick]) { "the cycle-end checkpoint was recorded" }
        assertTrue(cp.isCycleEnd, "the scoring-cycle-end checkpoint is flagged (drives the cycle sound)")
    }

    @Test
    fun noCheckpointIsRecordedOffACheckpointTick() {
        Cycle.updateCheckpoints(1, enlMu = 0, resMu = 0) // only the stuck-recovery sweep runs; must not crash
        assertTrue(Cycle.INSTANCE.checkpoints.isEmpty(), "no checkpoint off a checkpoint tick")
    }
}
