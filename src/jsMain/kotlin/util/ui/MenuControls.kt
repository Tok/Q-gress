package util.ui

import config.Config
import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.events.Event
import system.building.BuildingTransparency
import system.display.PassabilityOverlay
import system.display.PortalNameTicker
import system.display.fx.DamageNumberFx
import util.GameplayPrefs

/** Builders for the game-menu dropdown controls — split out of HtmlUtil (size). */
object MenuControls {

    private const val COMBAT_TIP =
        "How combat plays: 0 = tanky, slow-flipping board; 1 = portals flip easily. Drives shield mitigation, " +
            "weapon drops, attack eagerness, and the underdog comeback bonus."
    private const val PROGRESS_TIP =
        "How fast the game ramps early→endgame. Scales the recruiting rate (rosters grow faster) AND AP gain " +
            "(agents level up faster). 1 = baseline."

    /**
     * Append the menu's settings controls, split into a **Gameplay** group (affects the run; persisted to
     * [GameplayPrefs] + shown in the TUNING LAB) and a **Visual** group (no gameplay effect).
     */
    fun settings(menu: HTMLElement) {
        menu.append(sectionHead("Gameplay"))
        menu.append(dropRatesButton())
        menu.append(
            slider("Combat dynamics", Config.combatDynamism, Spec(tooltip = COMBAT_TIP, id = "combatDynSlider")) {
                Config.combatDynamism = it
                GameplayPrefs.save()
            },
        )
        menu.append(
            slider("Progress speed", Config.progressSpeed, Spec(0.25..4.0, tooltip = PROGRESS_TIP, id = "progressSlider")) {
                Config.progressSpeed = it
                GameplayPrefs.save()
            },
        )
        menu.append(gameplayResetButton())
        menu.append(sectionHead("Visual"))
        menu.append(checkbox("passabilityToggle", "Passability", false) { PassabilityOverlay.setVisible(it) })
        menu.append(checkbox("damageNumbersToggle", "Damage numbers", DamageNumberFx.enabled) { DamageNumberFx.enabled = it })
        menu.append(checkbox("portalNamesToggle", "Portal names", PortalNameTicker.enabled) { PortalNameTicker.setEnabled(it) })
        menu.append(slider("Buildings transparency", BuildingTransparency.default()) { BuildingTransparency.set(it) })
        menu.append(slider("Building shake", Config.buildingShakeMultiplier, Spec(0.0..2.0, 0.1)) { Config.buildingShakeMultiplier = it })
    }

    /** Optional slider settings — range / step / hover [tooltip] / input [id] — bundled to keep [slider] lean. */
    data class Spec(
        val range: ClosedFloatingPointRange<Double> = 0.0..1.0,
        val step: Double = 0.05,
        val tooltip: String = "",
        val id: String = "",
    )

    /** A labelled slider row over [spec]'s range, seeded at [initial]; [onInput] fires on every move. */
    fun slider(labelText: String, initial: Double, spec: Spec = Spec(), onInput: (Double) -> Unit): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        span.addClass("menuCheck", "menuSliderRow")
        if (spec.tooltip.isNotEmpty()) span.title = spec.tooltip
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label")
        label.innerHTML = labelText
        val slider = document.createElement("input") as HTMLInputElement
        if (spec.id.isNotEmpty()) slider.id = spec.id
        slider.type = "range"
        slider.min = spec.range.start.toString()
        slider.max = spec.range.endInclusive.toString()
        slider.step = spec.step.toString()
        slider.value = initial.toString()
        slider.addClass("slider", "menuSlider")
        val valueEl = document.createElement("span") as HTMLSpanElement
        valueEl.addClass("menuSliderVal")
        valueEl.textContent = fmt(initial)
        slider.oninput = {
            onInput(slider.valueAsNumber)
            valueEl.textContent = fmt(slider.valueAsNumber) // live numeric readout beside the slider
            null
        }
        span.append(label, slider, valueEl)
        return span
    }

    /** Two-decimal readout for a slider value (e.g. 0.60 / 1.00) shown beside the menu sliders. */
    private fun fmt(v: Double): String = v.asDynamic().toFixed(2) as String

    private fun checkbox(id: String, labelText: String, checked: Boolean, onChange: (Boolean) -> Unit): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        span.addClass("menuCheck")
        val check = document.createElement("input") as HTMLInputElement
        check.id = id
        check.type = "checkbox"
        check.checked = checked
        check.addClass("checkbox")
        check.onchange = {
            onChange(check.checked)
            null
        }
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label", "topLabel")
        label.innerHTML = labelText
        label.onclick = { check.click() }
        span.append(check, label)
        return span
    }

    // Reset the gameplay knobs (combat dynamics / progress speed / portal churn) to their shipped defaults and
    // reflect the new values back onto the menu's sliders so the UI doesn't read stale.
    private fun gameplayResetButton(): HTMLButtonElement {
        val b = document.createElement("button") as HTMLButtonElement
        b.id = "menuGameplayReset"
        b.className = "menuItem displayFont"
        b.textContent = "Reset gameplay"
        b.onclick = {
            GameplayPrefs.resetToDefaults()
            syncSlider("combatDynSlider", GameplayPrefs.DEFAULT_COMBAT)
            syncSlider("progressSlider", GameplayPrefs.DEFAULT_PROGRESS)
            null
        }
        return b
    }

    // Push a value onto a built menu slider and fire its input handler so the numeric readout + apply re-run.
    private fun syncSlider(id: String, value: Double) {
        val el = document.getElementById(id) as? HTMLInputElement ?: return
        el.value = value.toString()
        el.dispatchEvent(Event("input"))
    }

    private fun dropRatesButton(): HTMLButtonElement {
        val b = document.createElement("button") as HTMLButtonElement
        b.id = "menuDropRates"
        b.className = "menuItem displayFont"
        b.textContent = "Drop rates"
        b.onclick = {
            DropRatesPanel.toggle()
            null
        }
        return b
    }

    // A small section divider in the game menu (groups gameplay vs visual settings).
    private fun sectionHead(text: String): HTMLDivElement {
        val head = document.createElement("div") as HTMLDivElement
        head.addClass("menuSectionHead")
        head.textContent = text
        return head
    }
}
