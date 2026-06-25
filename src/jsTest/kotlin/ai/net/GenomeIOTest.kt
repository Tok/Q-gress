package ai.net

import ai.Observation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Round-trips a net genome through JSON and proves [GenomeIO] rejects mismatched/garbled genomes. */
class GenomeIOTest {

    private fun sampleGenome(hidden: Int = 4) = DoubleArray(Net.genomeSize(hidden)) { it * 0.013 - 0.2 }

    @Test
    fun roundTripsAGenomeBitForBit() {
        val genome = sampleGenome()
        val net = GenomeIO.decode(GenomeIO.encode(genome, hidden = 4, fitness = 123.5))

        assertEquals(listOf(4), net.arch.hiddens)
        assertTrue(genome.contentEquals(net.genome()), "weights survive the JSON round-trip")
    }

    @Test
    fun roundTripsADeepArchitecture() {
        val arch = NetArch(hiddens = listOf(16, 16), bias = true, activation = Activation.RELU)
        val genome = DoubleArray(arch.genomeSize()) { it * 0.001 }
        val net = GenomeIO.decode(GenomeIO.encode(genome, arch, fitness = 9.0))

        assertEquals(listOf(16, 16), net.arch.hiddens, "the layer shape survives")
        assertEquals(Activation.RELU, net.arch.activation, "the activation survives")
        assertTrue(genome.contentEquals(net.genome()))
    }

    @Test
    fun readsLegacySingleHiddenGenomes() {
        // Pre-multilayer format: a bare "hidden":4 (no "arch") must still decode as one hidden layer.
        val genome = sampleGenome(4)
        val legacy = """{"v":1,"hidden":4,"inputs":${Net.INPUTS},"outputs":${Net.OUTPUTS},"weights":[${genome.joinToString(",")}]}"""
        assertEquals(listOf(4), GenomeIO.decode(legacy).arch.hiddens)
    }

    @Test
    fun producesSameForwardPass() {
        val net = Net.fromGenome(sampleGenome(8), hidden = 8)
        val restored = GenomeIO.decode(GenomeIO.encode(net))
        val input = DoubleArray(Observation.SIZE) { 0.5 }

        assertTrue(net.forward(input).contentEquals(restored.forward(input)), "decoded net behaves identically")
    }

    @Test
    fun keepsTheFitnessTag() {
        assertEquals(987.0, GenomeIO.fitnessOf(GenomeIO.encode(sampleGenome(), hidden = 4, fitness = 987.0)))
    }

    @Test
    fun rejectsAWrongLengthGenome() {
        // inputs/outputs match the build but the weights array is too short for hidden=4 → Net.init rejects it.
        val bad = """{"v":1,"hidden":4,"inputs":${Net.INPUTS},"outputs":${Net.OUTPUTS},"weights":[0.1,0.2,0.3]}"""
        assertFailsWith<IllegalArgumentException> { GenomeIO.decode(bad) }
    }

    @Test
    fun rejectsAGenomeFromADifferentLayout() {
        val genome = sampleGenome()
        val staleInputs = GenomeIO.encode(genome, hidden = 4).replace("\"inputs\":${Net.INPUTS}", "\"inputs\":${Net.INPUTS + 1}")
        assertFailsWith<IllegalArgumentException> { GenomeIO.decode(staleInputs) }
    }
}
