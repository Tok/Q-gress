package ai.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NetTest {

    private fun genome(hidden: Int, fill: Double = 0.1) = DoubleArray(Net.genomeSize(hidden)) { fill }

    @Test
    fun genomeSizeMatchesTopology() {
        val hidden = 16
        assertEquals(hidden * (NetArch.INPUTS + 1) + NetArch.OUTPUTS * (hidden + 1), Net.genomeSize(hidden))
    }

    @Test
    fun forwardProducesOneBoundedValuePerSlider() {
        val net = Net.fromGenome(genome(8), hidden = 8)
        val out = net.forward(DoubleArray(NetArch.INPUTS) { 0.5 })

        assertEquals(NetArch.OUTPUTS, out.size, "one output per behaviour slider")
        assertTrue(out.all { it in 0.0..1.0 }, "sigmoid outputs are valid slider weights")
    }

    @Test
    fun forwardIsDeterministic() {
        val net = Net.fromGenome(genome(8), hidden = 8)
        val input = DoubleArray(NetArch.INPUTS) { it * 0.05 }
        assertEquals(net.forward(input).toList(), net.forward(input).toList())
    }

    @Test
    fun rejectsAGenomeOfTheWrongLength() {
        assertFailsWith<IllegalArgumentException> { Net.fromGenome(DoubleArray(5), hidden = 8) }
    }

    @Test
    fun tracedForwardExposesEveryLayerAndAgreesWithForward() {
        val net = Net.fromGenome(DoubleArray(Net.genomeSize(8)) { it * 0.001 - 0.05 }, hidden = 8)
        val input = DoubleArray(NetArch.INPUTS) { 0.3 }
        val trace = net.forwardTraced(input)

        assertEquals(NetArch.INPUTS, trace.input.size)
        assertEquals(1, trace.hiddens.size, "a single-hidden-layer net captures one hidden layer")
        assertEquals(8, trace.hiddens[0].size, "of the right width")
        assertEquals(NetArch.OUTPUTS, trace.output.size)
        assertTrue(trace.hiddens[0].all { it in -1.0..1.0 }, "tanh hidden activations")
        assertEquals(net.forward(input).toList(), trace.output.toList(), "the traced output matches forward()")
    }

    @Test
    fun tracesEveryLayerOfADeepNet() {
        val arch = NetArch(hiddens = listOf(8, 6))
        val net = Net.fromGenome(DoubleArray(arch.genomeSize()) { 0.05 }, arch)
        val trace = net.forwardTraced(DoubleArray(NetArch.INPUTS) { 0.4 })

        assertEquals(listOf(8, 6), trace.hiddens.map { it.size }, "both hidden layers captured, in order")
        assertEquals(NetArch.OUTPUTS, trace.output.size)
    }

    @Test
    fun weightAccessorReadsBackTheGenome() {
        // Genome layout per transition: each output neuron is [bias, w_in0, w_in1, …]. Layer 0 = input→hidden.
        val genome = DoubleArray(Net.genomeSize(4)) { it.toDouble() }
        val net = Net.fromGenome(genome, hidden = 4)

        // input 2 → hidden 0: hidden neuron 0's block, skip its bias, then input index 2.
        assertEquals(genome[0 * (NetArch.INPUTS + 1) + 1 + 2], net.weight(layer = 0, from = 2, to = 0))
        // hidden 3 → output 5: layer 1 starts after the input→hidden block; output neuron 5, skip bias, hidden 3.
        val outBase = 4 * (NetArch.INPUTS + 1)
        assertEquals(genome[outBase + 5 * (4 + 1) + 1 + 3], net.weight(layer = 1, from = 3, to = 5))
    }

    @Test
    fun genomeReturnsADefensiveCopy() {
        val g = DoubleArray(Net.genomeSize(4)) { it * 0.1 }
        val net = Net.fromGenome(g, hidden = 4)
        val copy = net.genome()
        assertEquals(g.toList(), copy.toList())
        copy[0] = 999.0 // mutating the copy must not touch the net's weights
        assertEquals(g[0], net.genome()[0])
    }

    @Test
    fun rejectsWrongInputSize() {
        val net = Net.fromGenome(genome(8), hidden = 8)
        assertFailsWith<IllegalArgumentException> { net.forward(DoubleArray(NetArch.INPUTS + 1)) }
    }

    @Test
    fun biaslessNetSkipsTheBiasWeight() {
        val arch = NetArch(hiddens = listOf(4), bias = false)
        val net = Net.fromGenome(DoubleArray(arch.genomeSize()) { 0.1 }, arch)
        assertEquals(NetArch.OUTPUTS, net.forward(DoubleArray(NetArch.INPUTS) { 0.5 }).size)
        assertTrue(arch.genomeSize() < NetArch(hiddens = listOf(4), bias = true).genomeSize(), "no bias → smaller genome")
    }

    @Test
    fun genomeSizeForArchOverloadAndDefault() {
        assertEquals(NetArch.DEFAULT.genomeSize(), Net.genomeSize(NetArch.DEFAULT))
        assertEquals(NetArch.DEFAULT.genomeSize(), Net.genomeSize()) // default arg
    }

    @Test
    fun archRejectsInvalidHiddens() {
        assertFailsWith<IllegalArgumentException> { NetArch(hiddens = emptyList()) }
        assertFailsWith<IllegalArgumentException> { NetArch(hiddens = listOf(4, 0)) } // widths must be > 0
    }
}
