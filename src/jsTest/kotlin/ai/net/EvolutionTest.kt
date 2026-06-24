package ai.net

import ai.MatchSetup
import ai.SimRunner
import util.GridFixture
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvolutionTest {

    private fun openGrid() = GridFixture("EVO", 30, 20, 2, GridFixture.rleEncode(List(30 * 20) { true })).toGrid()

    // Tiny but real: a few genomes, a few short matches — enough to exercise selection + mutation.
    private fun tinyConfig() = EvolutionConfig(
        populationSize = 6,
        generations = 3,
        elite = 2,
        hidden = 6,
        matchTicks = 301, // two checkpoints (tick 0 + 300)
        matchesPerEval = 1,
        setup = MatchSetup(npcs = 6),
    )

    @AfterTest
    fun tearDown() = SimRunner.reset()

    @Test
    fun trainReturnsAResultPerGeneration() {
        val result = Evolution.train(openGrid(), seed = 3, config = tinyConfig())

        assertEquals(3, result.bestPerGeneration.size, "one champion fitness per generation")
        assertEquals(Net.genomeSize(6), result.bestGenome.size, "winning genome matches the topology")
    }

    @Test
    fun championFitnessIsMonotonicWithElitism() {
        // Elites survive unchanged and are re-evaluated on the same fixed match seeds, so the best fitness
        // can never drop generation to generation.
        val history = Evolution.train(openGrid(), seed = 5, config = tinyConfig()).bestPerGeneration
        assertTrue(
            history.zipWithNext().all { (prev, next) -> next >= prev - 1e-9 },
            "best-per-generation is non-decreasing: $history",
        )
    }

    @Test
    fun trainIsDeterministic() {
        val a = Evolution.train(openGrid(), seed = 9, config = tinyConfig())
        val b = Evolution.train(openGrid(), seed = 9, config = tinyConfig())

        assertEquals(a.bestFitness, b.bestFitness, "same seed → same best fitness")
        assertContentEquals(a.bestGenome, b.bestGenome, "same seed → same winning genome")
    }
}
