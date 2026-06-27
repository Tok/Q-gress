package ai.net

import kotlin.math.exp

/**
 * A feed-forward MLP of arbitrary depth/width — the custom faction driver (PLAN Phase 6.2 / docs/NN.md). Its
 * shape is a [NetArch] (hidden layers, bias, activation); the input is an observation vector ([INPUTS]
 * features) and the output is the [OUTPUTS] behaviour sliders (always sigmoid → 0..1). All weights live in one flat genome (layout:
 * per layer transition, per output neuron, an optional bias then one weight per input), so neuroevolution can
 * mutate + serialize it. Pure, deterministic forward pass.
 */
class Net(val arch: NetArch, private val weights: DoubleArray) {

    init {
        require(weights.size == arch.genomeSize()) {
            "genome must be ${arch.genomeSize()} long for ${arch.label()}, got ${weights.size}"
        }
    }

    /** A defensive copy of the flat genome (to mutate / serialize). */
    fun genome(): DoubleArray = weights.copyOf()

    /** A captured forward pass — every layer's activations (for the live activation visualization). */
    class Trace(val input: DoubleArray, val hiddens: List<DoubleArray>, val output: DoubleArray)

    /** Run the net: [input] (length [INPUTS]) → [OUTPUTS] values in 0..1. */
    fun forward(input: DoubleArray): DoubleArray = forwardTraced(input).output

    /** Like [forward] but keeps every hidden layer's activations too — what the activation viz renders. */
    fun forwardTraced(input: DoubleArray): Trace {
        require(input.size == arch.inputs) { "expected ${arch.inputs} inputs, got ${input.size}" }
        val sizes = arch.layerSizes()
        val hiddens = mutableListOf<DoubleArray>()
        var x = input
        var offset = 0
        for (l in 0 until sizes.size - 1) {
            val outSize = sizes[l + 1]
            val isOutput = l == sizes.size - 2
            val out = DoubleArray(outSize)
            for (o in 0 until outSize) {
                var sum = if (arch.hasBias()) weights[offset++] else 0.0
                for (value in x) sum += weights[offset++] * value
                out[o] = if (isOutput) sigmoid(sum) else arch.activation.apply(sum)
            }
            if (!isOutput) hiddens.add(out)
            x = out
        }
        return Trace(input, hiddens, x)
    }

    /** Weight on the edge from node [from] in layer [layer] to node [to] in layer `layer+1` (for the diagram). */
    fun weight(layer: Int, from: Int, to: Int): Double {
        val fromSize = arch.layerSizes()[layer]
        return weights[arch.layerOffset(layer) + to * arch.stride(fromSize) + (if (arch.hasBias()) 1 else 0) + from]
    }

    companion object {
        val INPUTS = NetArch.INPUTS
        val OUTPUTS = NetArch.OUTPUTS

        /** Genome length for [arch]. */
        fun genomeSize(arch: NetArch = NetArch.DEFAULT): Int = arch.genomeSize()

        /** Convenience: genome length for a single hidden layer of [hidden] units (default bias + tanh). */
        fun genomeSize(hidden: Int): Int = NetArch(listOf(hidden)).genomeSize()

        /** A net from a flat genome + [arch] (length must equal [arch].genomeSize). */
        fun fromGenome(weights: DoubleArray, arch: NetArch = NetArch.DEFAULT): Net = Net(arch, weights)

        /** Convenience: a single-hidden-layer net from a flat genome. */
        fun fromGenome(weights: DoubleArray, hidden: Int): Net = Net(NetArch(listOf(hidden)), weights)

        private fun sigmoid(x: Double): Double = 1.0 / (1.0 + exp(-x))
    }
}
