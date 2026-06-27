package ai.net

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.tanh

/**
 * Hidden-layer activation functions (PLAN Phase 6.2) — a tuning knob for net experimentation. The output
 * layer is always sigmoid (the sliders must be 0..1); this picks how the hidden layers fire. [signed] marks
 * the ones that produce negative values (so the viz can two-tone them).
 */
enum class Activation {
    TANH,
    RELU,
    SIGMOID,
    LINEAR,
    ;

    fun apply(x: Double): Double = when (this) {
        TANH -> tanh(x)
        RELU -> max(0.0, x)
        SIGMOID -> 1.0 / (1.0 + exp(-x))
        LINEAR -> x
    }

    val signed: Boolean get() = this == TANH || this == LINEAR

    companion object {
        fun from(name: String?): Activation = entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: TANH
    }
}
