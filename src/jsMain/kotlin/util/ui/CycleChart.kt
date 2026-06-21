package util.ui

import agent.Faction
import external.UPlot
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import system.Cycle

/**
 * The MU time-series graph as a DOM **uPlot** chart (top-right), replacing the canvas CycleDisplay.
 * Two faction-coloured lines over the rolling checkpoint window. [update] is cheap — it only feeds
 * uPlot new data when a checkpoint actually changed.
 */
object CycleChart {
    private const val CONTAINER_ID = "cycleChart"
    private const val WIDTH = 300
    private const val HEIGHT = 96

    private var plot: UPlot? = null
    private var lastKey = ""

    /** Refresh from the current checkpoints; builds the chart lazily on first call. */
    fun update() {
        ensure()
        val p = plot ?: return
        val cps = Cycle.INSTANCE.checkpoints.toList().sortedBy { it.first }
        val key = "${cps.size}:${cps.lastOrNull()?.first ?: 0}"
        if (key == lastKey) return // unchanged — skip
        lastKey = key
        val xs = cps.indices.map { it.toDouble() }.toTypedArray()
        val enl = cps.map { it.second.enlMu.toDouble() }.toTypedArray()
        val res = cps.map { it.second.resMu.toDouble() }.toTypedArray()
        p.setData(arrayOf(xs, enl, res))
    }

    private fun ensure() {
        if (plot != null) return
        val body = document.body ?: return
        if (js("typeof uPlot === 'undefined'").unsafeCast<Boolean>()) return // CDN script not ready
        val container = document.createElement("div") as HTMLElement
        container.id = CONTAINER_ID
        container.className = "cycleChart"
        body.appendChild(container)

        val opts: dynamic = js("({})")
        opts.width = WIDTH
        opts.height = HEIGHT
        opts.cursor = js("({ show: false })")
        opts.legend = js("({ show: false })")
        opts.scales = js("({ x: { time: false } })")
        opts.axes = arrayOf(js("({ show: false })"), js("({ show: false })"))
        opts.series = arrayOf(js("({})"), seriesOpts(Faction.ENL.color), seriesOpts(Faction.RES.color))
        val empty: dynamic = arrayOf(arrayOf<Double>(), arrayOf<Double>(), arrayOf<Double>())
        plot = UPlot(opts, empty, container)
    }

    private fun seriesOpts(color: String): dynamic {
        val s: dynamic = js("({})")
        s.stroke = color
        s.width = 1.5
        s.points = js("({ show: false })")
        return s
    }
}
