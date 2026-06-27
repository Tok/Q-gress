package ai

import agent.Faction
import agent.qvalue.QValue
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement
import system.ui.Bootstrap

/**
 * A per-faction source of behaviour-slider weightings — the **action substrate** (PLAN Phase 6.0). For
 * each [QValue] a policy returns its raw 0..1 weighting; [agent.action.ActionSelector] multiplies that by
 * the QValue's own [QValue.weight]. The slider vector stays the action substrate: a policy only re-tunes
 * the weightings, it does **not** replace per-agent action selection.
 *
 * Today the only live policy is [DomSliderPolicy] (reads the tuning sliders — the pre-6.0 behaviour); a
 * future net/LLM driver installs a [SliderVectorPolicy] via [FactionPolicies.set] and rewrites it at
 * checkpoint cadence, with no change to how agents pick actions. (The pure [SliderVector] model lives in
 * commonMain; the policy plumbing here — DOM slider reads, overrides, the registry — is the jsMain shell.)
 */
interface FactionPolicy {
    /** The raw slider weighting (0..1) for [value], before [QValue.weight] is applied. */
    fun weight(value: QValue): Double

    /**
     * The full slider vector this policy is currently driving, or `null` when the policy IS the live UI
     * sliders ([DomSliderPolicy]) — i.e. **non-null means "an AI is in control"**, so the tuning UI should
     * stop being interactive and auto-move to mirror what the AI chose. Re-read at display cadence.
     */
    fun currentVector(): SliderVector? = null
}

/**
 * The default policy: read the live tuning slider for [faction] (id `"<qvalue>Slider<nickName>"`), or
 * [SliderVector.DEFAULT_WEIGHT] when there's no tuning UI (the title sim / headless matches). Byte-for-byte
 * the pre-6.0 `ActionSelector.q` read, so installing it changes nothing.
 */
class DomSliderPolicy(private val faction: Faction) : FactionPolicy {
    override fun weight(value: QValue): Double {
        // Headless (Node tests / future SimRunner) there's no `window` at all — skip the DOM read entirely.
        if (Bootstrap.isNotRunningInBrowser()) return SliderVector.DEFAULT_WEIGHT
        val id = value.id + "Slider" + faction.nickName
        val slider = window.document.getElementById(id) as? HTMLInputElement
        return slider?.valueAsNumber ?: SliderVector.DEFAULT_WEIGHT
    }
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
 * The live per-faction [FactionPolicy] registry. Each faction defaults to its own [DomSliderPolicy] (zero
 * gameplay change vs pre-6.0); an AI driver calls [set] to install a [SliderVectorPolicy] / net / LLM, and
 * [reset] restores the defaults (e.g. between headless matches). [lock]/[unlock] let the player override
 * individual sliders of an AI-driven faction (the AI keeps driving the rest).
 */
object FactionPolicies {
    private val policies = mutableMapOf<Faction, FactionPolicy>()

    fun of(faction: Faction): FactionPolicy = policies.getOrPut(faction) { DomSliderPolicy(faction) }

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
