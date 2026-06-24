package ai.net

import World
import agent.Faction
import agent.qvalue.QActions
import config.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetPolicyTest {

    private fun policy() = NetPolicy(Net.fromGenome(DoubleArray(Net.genomeSize(8)) { 0.2 }, hidden = 8), Faction.ENL)

    @BeforeTest
    fun setUp() {
        World.allAgents.clear()
        World.allPortals.clear()
        World.tick = 0
    }

    @AfterTest
    fun tearDown() {
        World.allAgents.clear()
        World.allPortals.clear()
        World.tick = 0
    }

    @Test
    fun weightsAreValidSliderValues() {
        val policy = policy()
        assertTrue(QActions.values().all { policy.weight(it) in 0.0..1.0 }, "net-derived weights are 0..1")
    }

    @Test
    fun reevaluatesOnlyOnCheckpointBoundaries() {
        val policy = policy()
        World.tick = 0
        val atStart = policy.weight(QActions.HACK)
        World.tick = 10 // same checkpoint (< ticksPerCheckpoint) → cached value, unchanged
        assertEquals(atStart, policy.weight(QActions.HACK), "stable within a checkpoint")

        World.tick = Config.ticksPerCheckpoint // next checkpoint → re-evaluated (still a valid weight)
        assertTrue(policy.weight(QActions.HACK) in 0.0..1.0)
    }
}
