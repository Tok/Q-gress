package ai.net

import ai.Observation
import ai.SliderVector
import kotlin.math.exp
import kotlin.math.tanh

/**
 * A tiny fixed-topology MLP — the custom faction driver (PLAN Phase 6.2 / docs/NN.md). Maps an
 * [Observation] (length [Observation.SIZE]) through one hidden layer (tanh) to the [SliderVector.SIZE]
 * behaviour sliders (sigmoid → 0..1). All weights live in one flat genome array, so neuroevolution can
 * mutate and serialize it. Pure, deterministic forward pass — no state, no side effects.
 *
 * Layout of the genome (row-major, per neuron `bias` then one weight per input):
 * `[hidden × (INPUTS+1)]` for the hidden layer, then `[OUTPUTS × (hidden+1)]` for the output layer.
 */
class Net(val hidden: Int, private val weights: DoubleArray) {

    init {
        require(weights.size == genomeSize(hidden)) {
            "genome must be ${genomeSize(hidden)} long for hidden=$hidden, got ${weights.size}"
        }
    }

    /** A defensive copy of the flat genome (to mutate / serialize). */
    fun genome(): DoubleArray = weights.copyOf()

    /** Run the net: [input] (length [INPUTS]) → [OUTPUTS] values in 0..1. */
    fun forward(input: DoubleArray): DoubleArray {
        require(input.size == INPUTS) { "expected $INPUTS inputs, got ${input.size}" }
        val hiddenOut = dense(input, hidden, 0) { tanh(it) }
        return dense(hiddenOut, OUTPUTS, hidden * (INPUTS + 1)) { sigmoid(it) }
    }

    // One fully-connected layer reading genome weights starting at [from]: for each output neuron, a bias
    // then one weight per input, run through [activation].
    private fun dense(input: DoubleArray, outSize: Int, from: Int, activation: (Double) -> Double): DoubleArray {
        val out = DoubleArray(outSize)
        var offset = from
        for (o in 0 until outSize) {
            var sum = weights[offset++] // bias
            for (value in input) sum += weights[offset++] * value
            out[o] = activation(sum)
        }
        return out
    }

    companion object {
        val INPUTS = Observation.SIZE
        val OUTPUTS = SliderVector.SIZE
        const val DEFAULT_HIDDEN = 16

        /** Genome length for a [hidden]-unit net: `hidden·(INPUTS+1) + OUTPUTS·(hidden+1)`. */
        fun genomeSize(hidden: Int = DEFAULT_HIDDEN): Int = hidden * (INPUTS + 1) + OUTPUTS * (hidden + 1)

        /** A net from a flat genome (length must equal [genomeSize] for [hidden]). */
        fun fromGenome(weights: DoubleArray, hidden: Int = DEFAULT_HIDDEN): Net = Net(hidden, weights)

        private fun sigmoid(x: Double): Double = 1.0 / (1.0 + exp(-x))
    }
}
