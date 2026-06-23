package util.ui

import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSpanElement

/** Builders for the game-menu dropdown controls — split out of HtmlUtil (size). */
object MenuControls {

    /** A labelled slider row (e.g. building opacity / combat dynamism / building shake). */
    fun slider(
        labelText: String,
        initial: Double,
        min: Double = 0.0,
        max: Double = 1.0,
        step: Double = 0.05,
        onInput: (Double) -> Unit,
    ): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        span.addClass("menuCheck", "menuSliderRow")
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label")
        label.innerHTML = labelText
        val slider = document.createElement("input") as HTMLInputElement
        slider.type = "range"
        slider.min = min.toString()
        slider.max = max.toString()
        slider.step = step.toString()
        slider.value = initial.toString()
        slider.addClass("slider", "menuSlider")
        slider.oninput = {
            onInput(slider.valueAsNumber)
            null
        }
        span.append(label, slider)
        return span
    }
}
