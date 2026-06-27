package ai.net

/**
 * A net **architecture** (PLAN Phase 6.2) — the experimentation substrate. Defines the hidden layers (any
 * number, any width — default **two layers of 16**), whether neurons carry a [bias], and the hidden
 * [activation]. The input/output sizes are the fixed net I/O contract ([INPUTS]/[OUTPUTS]). Pure + comparable,
 * so two archs can be pitted against each other and a genome's shape round-trips through serialization.
 */
data class NetArch(val hiddens: List<Int> = DEFAULT_HIDDENS, val bias: Boolean = true, val activation: Activation = Activation.TANH) {
    init {
        require(hiddens.isNotEmpty() && hiddens.all { it > 0 }) { "need at least one hidden layer, all widths > 0" }
    }

    val inputs: Int get() = INPUTS
    val outputs: Int get() = OUTPUTS
    private fun biasCount(): Int = if (bias) 1 else 0

    /** Every layer width in order: input, hidden…, output. */
    fun layerSizes(): List<Int> = buildList {
        add(inputs)
        addAll(hiddens)
        add(outputs)
    }

    /** Flat genome length: for each layer transition `out × (in + bias?1:0)`. */
    fun genomeSize(): Int = layerSizes().zipWithNext { from, to -> to * (from + biasCount()) }.sum()

    /** Weight offset within the flat genome where transition [layer] (`layerSizes[layer] → [layer+1]`) begins. */
    fun layerOffset(layer: Int): Int {
        val sizes = layerSizes()
        var offset = 0
        for (l in 0 until layer) offset += sizes[l + 1] * (sizes[l] + biasCount())
        return offset
    }

    /** Per-output stride within a transition's weight block (`in + bias`). */
    fun stride(fromSize: Int): Int = fromSize + biasCount()

    fun hasBias(): Boolean = bias

    /** A compact human label, e.g. `13 → 16 → 16 → 17`. */
    fun label(): String = layerSizes().joinToString(" → ")

    companion object {
        // The fixed net I/O contract. INPUTS mirrors ai.Observation.SIZE (the feature-vector length) and
        // OUTPUTS mirrors ai.SliderVector.SIZE (QActions + QDestinations = 10 + 7). Pinned here so the net
        // core stays platform-agnostic (those classes are World/DOM-coupled); NetContractTest guards drift.
        const val INPUTS = 13
        const val OUTPUTS = 17

        val DEFAULT_HIDDENS = listOf(16, 16) // two hidden layers of 16 — the default architecture
        val DEFAULT = NetArch()
    }
}
