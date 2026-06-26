package ai.net

import ai.MatchSetup
import ai.SimRunner
import util.GridFixture
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EvolutionSessionTest {

    private fun openGrid() = GridFixture("EVO", 30, 20, 2, GridFixture.rleEncode(List(30 * 20) { true })).toGrid()

    // Tiny but real: a few genomes, a few short matches — enough to exercise selection + mutation.
    private fun tinyConfig() = EvolutionConfig(
        populationSize = 6,
        generations = 3,
        elite = 2,
        arch = NetArch(listOf(6)),
        matchTicks = 301, // two checkpoints (tick 0 + 300)
        matchesPerEval = 1,
        setup = MatchSetup(npcs = 6),
    )

    @AfterTest
    fun tearDown() = SimRunner.reset()

    @Test
    fun steppingToCompletionMatchesTrain() {
        // Driving a Session one generation at a time must give exactly the same result as the all-at-once
        // train() loop — train() is literally a while-loop over step().
        val viaTrain = Evolution.train(openGrid(), seed = 7, config = tinyConfig())

        val session = Evolution.Session(openGrid(), seed = 7, config = tinyConfig())
        while (!session.done) session.step()
        val viaSession = session.result()

        assertEquals(viaTrain.bestFitness, viaSession.bestFitness, "same seed → same best fitness")
        assertContentEquals(viaTrain.bestGenome, viaSession.bestGenome, "same seed → same winning genome")
        assertContentEquals(
            viaTrain.bestPerGeneration.toDoubleArray(),
            viaSession.bestPerGeneration.toDoubleArray(),
            "same fitness curve",
        )
    }

    @Test
    fun progressIsObservableBetweenSteps() {
        val session = Evolution.Session(openGrid(), seed = 4, config = tinyConfig())

        assertEquals(0, session.generation, "no generations run yet")
        assertTrue(session.history().isEmpty(), "no fitness recorded yet")

        val first = session.step()
        assertEquals(1, session.generation, "one generation ran")
        assertEquals(listOf(first), session.history(), "history tracks the per-generation champion")
        assertEquals(first, session.bestFitness, "best fitness is the only champion so far")
    }

    @Test
    fun bestFitnessIsMonotonicAcrossSteps() {
        val session = Evolution.Session(openGrid(), seed = 5, config = tinyConfig())
        var prev = Double.NEGATIVE_INFINITY
        while (!session.done) {
            session.step()
            assertTrue(session.bestFitness >= prev - 1e-9, "best fitness never drops: ${session.bestFitness} < $prev")
            prev = session.bestFitness
        }
    }

    @Test
    fun steppingPastCompletionFails() {
        val session = Evolution.Session(openGrid(), seed = 1, config = tinyConfig())
        while (!session.done) session.step()
        assertFailsWith<IllegalStateException> { session.step() }
    }
}
