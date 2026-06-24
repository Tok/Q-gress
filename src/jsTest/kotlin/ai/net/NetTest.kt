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
}
