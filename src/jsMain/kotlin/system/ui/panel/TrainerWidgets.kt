package system.ui.panel

import external.UPlot
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import system.ui.el

/**
 * Stateless DOM / canvas / uPlot builders for the **Train NN** screen ([TrainerPanel]) — split out (the panel
 * was a `LargeClass`) so the panel keeps only the training-run + net-I/O logic. Nothing here holds state.
 */
internal object TrainerWidgets {

    fun numberField(parent: HTMLElement, label: String, value: String, step: String, min: String, max: String): HTMLInputElement {
        val wrap = el("div", "trainField")
        wrap.appendChild(el("span", "trainFieldLabel").also { it.textContent = label })
        val input = document.createElement("input") as HTMLInputElement
        input.type = "number"
        input.value = value
        input.step = step
        input.min = min
        input.max = max
        input.className = "trainInput"
        wrap.appendChild(input)
        parent.appendChild(wrap)
        return input
    }

    fun checkboxField(parent: HTMLElement, label: String, title: String): HTMLInputElement {
        val wrap = el("label", "ladderEntrant").also { it.title = title } // reuse the leaderboard checkbox-row style
        val box = document.createElement("input") as HTMLInputElement
        box.type = "checkbox"
        wrap.appendChild(box)
        wrap.appendChild(el("span", "trainFieldLabel").also { it.textContent = label })
        parent.appendChild(wrap)
        return box
    }

    fun selectField(parent: HTMLElement, label: String, options: List<Pair<String, String>>): HTMLSelectElement {
        val wrap = el("div", "trainField")
        wrap.appendChild(el("span", "trainFieldLabel").also { it.textContent = label })
        val select = document.createElement("select") as HTMLSelectElement
        select.className = "trainSelect"
        options.forEach { (value, text) ->
            val option = document.createElement("option") as HTMLOptionElement
            option.value = value
            option.textContent = text
            select.appendChild(option)
        }
        wrap.appendChild(select)
        parent.appendChild(wrap)
        return select
    }

    fun button(label: String, cls: String, onClick: () -> Unit): HTMLButtonElement {
        val btn = document.createElement("button") as HTMLButtonElement
        btn.className = cls
        btn.textContent = label
        btn.onclick = {
            onClick()
            null
        }
        return btn
    }

    // A device-pixel-resolution canvas → crisp fills on HiDPI (CSS size w×h, backing store ×dpr).
    fun dprCanvas(w: Int, h: Int): HTMLCanvasElement {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        val dpr = window.devicePixelRatio.takeIf { it > 0.0 } ?: 1.0
        canvas.width = (w * dpr).toInt()
        canvas.height = (h * dpr).toInt()
        canvas.style.width = "${w}px"
        canvas.style.height = "${h}px"
        (canvas.getContext("2d") as? CanvasRenderingContext2D)?.scale(dpr, dpr)
        return canvas
    }

    fun makePlot(target: HTMLElement, w: Int, h: Int, color: String): UPlot {
        val opts: dynamic = js("({})")
        opts.width = w
        opts.height = h
        opts.cursor = js("({ show: false })")
        opts.legend = js("({ show: false })")
        opts.scales = js("({ x: { time: false } })")
        opts.axes = arrayOf(whiteAxis(), whiteAxis()) // tick labels white so they read on the dark UI
        val series: dynamic = js("({})")
        series.stroke = color
        series.width = 2
        series.fill = "rgba(120, 170, 255, 0.14)"
        series.points = js("({ show: false })")
        opts.series = arrayOf(js("({})"), series)
        val empty: dynamic = arrayOf(arrayOf<Double>(), arrayOf<Double>())
        return UPlot(opts, empty, target)
    }

    // A uPlot axis with white tick labels + faint grid/ticks — legible on the dark trainer panel.
    private fun whiteAxis(): dynamic {
        val a: dynamic = js("({})")
        a.stroke = "#ffffff" // tick-label + axis colour
        a.grid = js("({ stroke: 'rgba(255,255,255,0.10)', width: 1 })")
        a.ticks = js("({ stroke: 'rgba(255,255,255,0.25)', width: 1 })")
        return a
    }
}
