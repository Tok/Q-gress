package ai

import agent.qvalue.QActions
import agent.qvalue.QDestinations
import agent.qvalue.QValue

/**
 * The behaviour sliders as one flat, ordered, faction-agnostic vector — the substrate an AI driver writes
 * (PLAN Phase 6.0). One slot per [QValue] in a STABLE order ([ORDER] = every [QActions] then every
 * [QDestinations]), so a net genome / LLM JSON encode/decode round-trips with a fixed layout. Pure +
 * immutable; each slot is a 0..1 weighting (pre-[QValue.weight]). Wrap one in a [SliderVectorPolicy] to
 * drive a faction with it.
 */
class SliderVector private constructor(private val slots: DoubleArray) {

    /** The 0..1 weighting for [value]. */
    operator fun get(value: QValue): Double = slots[indexOf(value)]

    /** A defensive copy of the raw slots, in [ORDER] (e.g. to serialize a genome). */
    fun toArray(): DoubleArray = slots.copyOf()

    /** A copy with [value]'s slot set to [weight] (clamped 0..1); the receiver is unchanged. */
    fun with(value: QValue, weight: Double): SliderVector {
        val copy = slots.copyOf()
        copy[indexOf(value)] = weight.coerceIn(0.0, 1.0)
        return SliderVector(copy)
    }

    companion object {
        /** The stable slot order: every action slider, then every destination slider. */
        val ORDER: List<QValue> = QActions.values() + QDestinations.values()

        /** Number of slots (currently 19 = 12 actions + 7 destinations). */
        val SIZE: Int = ORDER.size

        private val indexById: Map<String, Int> = ORDER.withIndex().associate { (i, q) -> q.id to i }

        fun indexOf(value: QValue): Int = indexById[value.id] ?: error("QValue '${value.id}' is not a known slider slot")

        /** Every slot set to [weight] (default = the tuning-slider default, so it mirrors a fresh UI). */
        fun uniform(weight: Double = DomSliderPolicy.DEFAULT_WEIGHT): SliderVector =
            SliderVector(DoubleArray(SIZE) { weight.coerceIn(0.0, 1.0) })

        /** Build from a raw array in [ORDER] (length must equal [SIZE]); each value clamped to 0..1. */
        fun decode(array: DoubleArray): SliderVector {
            require(array.size == SIZE) { "SliderVector needs $SIZE values, got ${array.size}" }
            return SliderVector(DoubleArray(SIZE) { array[it].coerceIn(0.0, 1.0) })
        }
    }
}

/**
 * A [FactionPolicy] backed by a [SliderVector] — what an AI driver installs via [FactionPolicies.set]. The
 * [vector] is swappable, so the driver re-tunes at checkpoint cadence without rebinding anything.
 */
class SliderVectorPolicy(var vector: SliderVector) : FactionPolicy {
    override fun weight(value: QValue): Double = vector[value]
}
