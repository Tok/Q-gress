package ai

import agent.Faction
import agent.qvalue.QValue
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement
import system.ui.Bootstrap

/**
 * The live UI [FactionPolicy]: read the tuning slider for [faction] (id `"<qvalue>Slider<nickName>"`), or
 * [SliderVector.DEFAULT_WEIGHT] when there's no tuning UI (the title sim / headless matches). Byte-for-byte
 * the pre-6.0 `ActionSelector.q` read. The jsMain shell installs this as [FactionPolicies.defaultPolicy] at
 * boot (see `Bootstrap.load`), so it's the browser default while headless stays on [DefaultPolicy].
 */
class DomSliderPolicy(private val faction: Faction) : FactionPolicy {
    override fun weight(value: QValue): Double {
        // Headless (Node tests / the SimRunner) there's no `window` at all — skip the DOM read entirely.
        if (Bootstrap.isNotRunningInBrowser()) return SliderVector.DEFAULT_WEIGHT
        val id = value.id + "Slider" + faction.nickName
        val slider = window.document.getElementById(id) as? HTMLInputElement
        return slider?.valueAsNumber ?: SliderVector.DEFAULT_WEIGHT
    }
}
