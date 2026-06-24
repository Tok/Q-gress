package ai.net

import ai.Observation
import ai.SliderVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NetTest {

    private fun genome(hidden: Int, fill: Double = 0.1) = DoubleArray(Net.genomeSize(hidden)) { fill }

    @Test
    fun genomeSizeMatchesTopology() {
        val hidden = 16
        assertEquals(hidden * (Observation.SIZE + 1) + SliderVector.SIZE * (hidden + 1), Net.genomeSize(hidden))
    }

    @Test
    fun forwardProducesOneBoundedValuePerSlider() {
        val net = Net.fromGenome(genome(8), hidden = 8)
        val out = net.forward(DoubleArray(Observation.SIZE) { 0.5 })

        assertEquals(SliderVector.SIZE, out.size, "one output per behaviour slider")
        assertTrue(out.all { it in 0.0..1.0 }, "sigmoid outputs are valid slider weights")
        // The output is a valid SliderVector (decode would otherwise throw on a bad length/range).
        SliderVector.decode(out)
    }

    @Test
    fun forwardIsDeterministic() {
        val net = Net.fromGenome(genome(8), hidden = 8)
        val input = DoubleArray(Observation.SIZE) { it * 0.05 }
        assertEquals(net.forward(input).toList(), net.forward(input).toList())
    }

    @Test
    fun rejectsAGenomeOfTheWrongLength() {
        assertFailsWith<IllegalArgumentException> { Net.fromGenome(DoubleArray(5), hidden = 8) }
    }

    @Test
    fun tracedForwardExposesEveryLayerAndAgreesWithForward() {
        val net = Net.fromGenome(DoubleArray(Net.genomeSize(8)) { it * 0.001 - 0.05 }, hidden = 8)
        val input = DoubleArray(Observation.SIZE) { 0.3 }
        val trace = net.forwardTraced(input)

        assertEquals(Observation.SIZE, trace.input.size)
        assertEquals(8, trace.hidden.size, "the hidden layer is captured")
        assertEquals(SliderVector.SIZE, trace.output.size)
        assertTrue(trace.hidden.all { it in -1.0..1.0 }, "tanh hidden activations")
        assertEquals(net.forward(input).toList(), trace.output.toList(), "the traced output matches forward()")
    }

    @Test
    fun weightAccessorsReadBackTheGenome() {
        // Genome layout: hidden neurons [bias, w_in0, w_in1, …] then output neurons [bias, w_h0, …].
        val genome = DoubleArray(Net.genomeSize(4)) { it.toDouble() }
        val net = Net.fromGenome(genome, hidden = 4)

        assertEquals(genome[0 * (Observation.SIZE + 1) + 1 + 2], net.inputWeight(i = 2, h = 0))
        val outBase = 4 * (Observation.SIZE + 1)
        assertEquals(genome[outBase + 5 * (4 + 1) + 1 + 3], net.hiddenWeight(h = 3, o = 5))
    }
}
