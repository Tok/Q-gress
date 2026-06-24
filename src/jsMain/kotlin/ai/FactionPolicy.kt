package ai

import agent.Faction
import agent.qvalue.QValue
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement
import util.HtmlUtil

/**
 * A per-faction source of behaviour-slider weightings — the **action substrate** (PLAN Phase 6.0). For
 * each [QValue] a policy returns its raw 0..1 weighting; [agent.action.ActionSelector] multiplies that by
 * the QValue's own [QValue.weight]. The slider vector stays the action substrate: a policy only re-tunes
 * the weightings, it does **not** replace per-agent action selection.
 *
 * Today the only live policy is [DomSliderPolicy] (reads the tuning sliders — the pre-6.0 behaviour); a
 * future net/LLM driver installs a [SliderVectorPolicy] via [FactionPolicies.set] and rewrites it at
 * checkpoint cadence, with no change to how agents pick actions.
 */
interface FactionPolicy {
    /** The raw slider weighting (0..1) for [value], before [QValue.weight] is applied. */
    fun weight(value: QValue): Double
}

/**
 * The default policy: read the live tuning slider for [faction] (id `"<qvalue>Slider<nickName>"`), or
 * [DEFAULT_WEIGHT] when there's no tuning UI (the title sim / headless matches). Byte-for-byte the pre-6.0
 * `ActionSelector.q` read, so installing it changes nothing.
 */
class DomSliderPolicy(private val faction: Faction) : FactionPolicy {
    override fun weight(value: QValue): Double {
        // Headless (Node tests / future SimRunner) there's no `window` at all — skip the DOM read entirely.
        if (HtmlUtil.isNotRunningInBrowser()) return DEFAULT_WEIGHT
        val id = value.id + "Slider" + faction.nickName
        val slider = window.document.getElementById(id) as? HTMLInputElement
        return slider?.valueAsNumber ?: DEFAULT_WEIGHT
    }

    companion object {
        const val DEFAULT_WEIGHT = 0.1 // the tuning sliders' default value
    }
}

/**
 * The live per-faction [FactionPolicy] registry. Each faction defaults to its own [DomSliderPolicy] (zero
 * gameplay change vs pre-6.0); an AI driver calls [set] to install a [SliderVectorPolicy] / net / LLM, and
 * [reset] restores the defaults (e.g. between headless matches).
 */
object FactionPolicies {
    private val policies = mutableMapOf<Faction, FactionPolicy>()

    fun of(faction: Faction): FactionPolicy = policies.getOrPut(faction) { DomSliderPolicy(faction) }

    fun set(faction: Faction, policy: FactionPolicy) {
        policies[faction] = policy
    }

    fun reset() = policies.clear()
}
