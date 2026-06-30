package ai

import agent.Faction
import agent.qvalue.QValue

/**
 * A per-faction source of behaviour-slider weightings — the **action substrate** (PLAN Phase 6.0). For
 * each [QValue] a policy returns its raw 0..1 weighting; [agent.action.ActionSelector] multiplies that by
 * the QValue's own [QValue.weight]. The slider vector stays the action substrate: a policy only re-tunes
 * the weightings, it does **not** replace per-agent action selection.
 *
 * The interface + the pure policies ([DefaultPolicy], [SliderVectorPolicy], [OverridePolicy]) + the
 * [FactionPolicies] registry live in commonMain; the live UI policy that reads the tuning sliders
 * (`DomSliderPolicy`) is the jsMain shell and installs itself as [FactionPolicies.defaultPolicy] at boot.
 */
interface FactionPolicy {
    /** The raw slider weighting (0..1) for [value], before [QValue.weight] is applied. */
    fun weight(value: QValue): Double

    /**
     * The full slider vector this policy is currently driving, or `null` when the policy IS the live UI
     * sliders (`DomSliderPolicy`) — i.e. **non-null means "an AI is in control"**, so the tuning UI should
     * stop being interactive and auto-move to mirror what the AI chose. Re-read at display cadence.
     */
    fun currentVector(): SliderVector? = null
}

/**
 * The headless default policy: every weighting is [SliderVector.DEFAULT_WEIGHT] — what a faction reads with
 * no tuning UI (Node tests / headless matches), matching `DomSliderPolicy`'s headless branch. The jsMain
 * shell swaps in `DomSliderPolicy` as the live default at boot (see [FactionPolicies.defaultPolicy]).
 */
object DefaultPolicy : FactionPolicy {
    override fun weight(value: QValue): Double = SliderVector.DEFAULT_WEIGHT
}

/**
 * Wraps an AI [inner] policy with **player overrides** (PLAN Phase 6.4): a locked slider returns the
 * player's value instead of the AI's, for both behaviour ([weight]) and display ([currentVector]). Unlocked
 * slots pass straight through, so the AI still drives everything the player hasn't grabbed. Overrides are
 * keyed by [QValue.id]; an empty override set is a transparent pass-through.
 */
class OverridePolicy(val inner: FactionPolicy) : FactionPolicy {
    private val overrides = mutableMapOf<String, Double>()

    fun lock(value: QValue, weight: Double) {
        overrides[value.id] = weight.coerceIn(0.0, 1.0)
    }

    fun unlock(value: QValue) {
        overrides.remove(value.id)
    }

    fun lockedValue(value: QValue): Double? = overrides[value.id]

    override fun weight(value: QValue): Double = overrides[value.id] ?: inner.weight(value)

    override fun currentVector(): SliderVector? {
        val base = inner.currentVector() ?: return null
        if (overrides.isEmpty()) return base
        var v = base
        SliderVector.ORDER.forEach { q -> overrides[q.id]?.let { v = v.with(q, it) } }
        return v
    }
}

/**
 * The live per-faction [FactionPolicy] registry. Each faction defaults to [defaultPolicy] (the jsMain shell
 * installs `DomSliderPolicy` there at boot → zero gameplay change vs pre-6.0; headless stays [DefaultPolicy]);
 * an AI driver calls [set] to install a [SliderVectorPolicy] / net / LLM, and [reset] restores the defaults
 * (e.g. between headless matches). [lock]/[unlock] let the player override individual sliders of an AI-driven
 * faction (the AI keeps driving the rest).
 */
object FactionPolicies {
    private val policies = mutableMapOf<Faction, FactionPolicy>()

    /** The per-faction default-policy factory. commonMain defaults to [DefaultPolicy] (headless weighting);
     *  the jsMain shell installs `::DomSliderPolicy` at boot so the browser reads the live tuning sliders. */
    var defaultPolicy: (Faction) -> FactionPolicy = { DefaultPolicy }

    fun of(faction: Faction): FactionPolicy = policies.getOrPut(faction) { defaultPolicy(faction) }

    fun set(faction: Faction, policy: FactionPolicy) {
        policies[faction] = policy // installing a fresh driver drops any prior overrides (intentional reset)
    }

    /** Pin [value] to the player's [weight] for [faction] (wrapping its AI policy in an [OverridePolicy]). */
    fun lock(faction: Faction, value: QValue, weight: Double) {
        val current = of(faction)
        val override = current as? OverridePolicy ?: OverridePolicy(current).also { policies[faction] = it }
        override.lock(value, weight)
    }

    /** Hand [value] back to the AI for [faction]. */
    fun unlock(faction: Faction, value: QValue) {
        (policies[faction] as? OverridePolicy)?.unlock(value)
    }

    /** The player-locked value for [faction]'s [value], or null if the AI still drives it. */
    fun lockedValue(faction: Faction, value: QValue): Double? = (policies[faction] as? OverridePolicy)?.lockedValue(value)

    fun reset() = policies.clear()
}

/**
 * A [FactionPolicy] backed by a [SliderVector] — what an AI driver installs via [FactionPolicies.set]. The
 * [vector] is swappable, so the driver re-tunes at checkpoint cadence without rebinding anything.
 */
class SliderVectorPolicy(var vector: SliderVector) : FactionPolicy {
    override fun weight(value: QValue): Double = vector[value]
    override fun currentVector(): SliderVector = vector
}
