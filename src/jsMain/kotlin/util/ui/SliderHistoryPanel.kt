package util.ui

import agent.Faction
import agent.qvalue.QActions
import agent.qvalue.QValue
import ai.FactionPolicies
import ai.SliderVector
import external.UPlot
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import system.Cycle

/**
 * The slider-history section of the **AI** footer tab (merged with [AiPanel]'s observation readout): one
 * sparkline per behaviour-slider slot, tracking how each faction's weighting has moved over the checkpoint
 * window. It's the visible record of an AI driver re-tuning the sliders (lines that drift as the match swings)
 * versus Manual control (flat lines). Each slot's snapshot is the faction's installed [ai.FactionPolicy]
 * weighting, so it reads uniformly for manual sliders and AI policies alike. Mirrors [HistoryPanel]'s uPlot
 * row style; rebuilt lazily, fed only when a checkpoint changes.
 */
object SliderHistoryPanel {
    private const val WINDOW = 35 // mirror the checkpoint rolling window (Cycle.numberOfCheckpoints)
    private val ACTION_COUNT = QActions.values().size // first N slots are actions, the rest are targets
    private const val COLS = 4 // lay the slots out in 4 columns (was 2) to use the footer width
    private const val CHART_W = 240 // initial chart width; resizeCharts() then stretches each to its cell
    private const val CHART_H = 40
    private const val FILL_ALPHA = "0.16)" // appended to a faction's "rgba(r, g, b, " prefix

    private val OVERLAP_COLOR = blendHex(Faction.ENL.color, Faction.RES.color)
    private val SLOTS: List<QValue> = SliderVector.ORDER

    private var built = false
    private val plots = arrayOfNulls<UPlot>(SLOTS.size)
    private val chartDivs = arrayOfNulls<HTMLElement>(SLOTS.size)
    private var lastChartW = 0
    private val enlVals = arrayOfNulls<HTMLElement>(SLOTS.size)
    private val resVals = arrayOfNulls<HTMLElement>(SLOTS.size)

    // Rolling per-checkpoint snapshots: each entry holds one faction's weighting for every slot (in SLOTS order).
    private val enlHist = mutableListOf<DoubleArray>()
    private val resHist = mutableListOf<DoubleArray>()
    private var lastKey = ""

    fun update() {
        if (!ensure()) return
        resizeCharts() // stretch each sparkline to its (4-col) cell width, tracking footer maximize / resize
        SLOTS.forEachIndexed { i, q ->
            enlVals[i]?.textContent = pct(FactionPolicies.of(Faction.ENL).weight(q))
            resVals[i]?.textContent = pct(FactionPolicies.of(Faction.RES).weight(q))
        }
        val cps = Cycle.INSTANCE.checkpoints.toList().sortedBy { it.first }
        val key = "${cps.size}:${cps.lastOrNull()?.first ?: 0}"
        if (key == lastKey) return // history unchanged — skip the snapshot + sparkline re-feed
        lastKey = key
        snapshot()
        feed()
    }

    private fun snapshot() {
        enlHist.add(DoubleArray(SLOTS.size) { FactionPolicies.of(Faction.ENL).weight(SLOTS[it]) })
        resHist.add(DoubleArray(SLOTS.size) { FactionPolicies.of(Faction.RES).weight(SLOTS[it]) })
        while (enlHist.size > WINDOW) enlHist.removeAt(0)
        while (resHist.size > WINDOW) resHist.removeAt(0)
    }

    private fun feed() {
        val xs = enlHist.indices.map { it.toDouble() }.toTypedArray()
        SLOTS.indices.forEach { slot ->
            val enl = enlHist.map { it[slot] }.toTypedArray()
            val res = resHist.map { it[slot] }.toTypedArray()
            // Overlap series: the value only where both factions coincide (else a null gap), drawn on top in
            // the blended colour so identical weightings read as neutral, not whichever line was drawn last.
            val overlap: Array<Double?> = Array(enl.size) { if (enl[it] == res[it]) enl[it] else null }
            plots[slot]?.setData(arrayOf(xs, enl, res, overlap))
        }
    }

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
        if (js("typeof uPlot === 'undefined'").unsafeCast<Boolean>()) return false // CDN not ready
        val section = el("aiSection")
        section.appendChild(el("aiSectionTitle").also { it.textContent = "Behaviour sliders over time" })
        // Grouped like the tuning sliders: the 10 agent "actions" first, then the 7 "targets" (destinations).
        section.appendChild(buildGroup("Actions", 0 until ACTION_COUNT))
        section.appendChild(buildGroup("Targets", ACTION_COUNT until SLOTS.size))
        Footer.tab("ai").appendChild(section)
        built = true
        return true
    }

    private fun buildGroup(title: String, range: IntRange): HTMLElement {
        val group = el("aiSliderGroup")
        group.appendChild(el("aiSliderGroupTitle").also { it.textContent = title })
        val grid = el("sliderHistoryGrid")
        val st = grid.asDynamic().style
        st.display = "grid"
        st.gridTemplateColumns = "repeat($COLS, minmax(0, 1fr))" // COLS equal columns across the footer width
        st.columnGap = "16px"
        for (i in range) buildSlot(grid, i)
        group.appendChild(grid)
        return group
    }

    private fun buildSlot(grid: HTMLElement, i: Int) {
        val row = el("historyRow")
        val head = el("historyHead")
        head.appendChild(el("historyTitle").also { it.textContent = SLOTS[i].description })
        val enl = el("historyVal").also { it.style.color = Faction.ENL.color }
        head.appendChild(enl)
        enlVals[i] = enl
        val res = el("historyVal").also { it.style.color = Faction.RES.color }
        head.appendChild(res)
        resVals[i] = res
        row.appendChild(head)
        val chart = el("historyChart")
        row.appendChild(chart)
        grid.appendChild(row)
        chartDivs[i] = chart
        plots[i] = makePlot(chart)
    }

    // Stretch every sparkline to its grid cell's current width (so they fill the 4 columns and follow the
    // footer being maximized / the window resizing). uPlot needs an explicit pixel size, so we measure + setSize
    // only when the cell width actually changes.
    private fun resizeCharts() {
        val w = chartDivs[0]?.clientWidth ?: 0
        if (w <= 0 || w == lastChartW) return
        lastChartW = w
        plots.forEach { plot ->
            val size: dynamic = js("({})")
            size.width = w
            size.height = CHART_H
            plot?.setSize(size)
        }
    }

    private fun makePlot(target: HTMLElement): UPlot {
        val opts: dynamic = js("({})")
        opts.width = CHART_W
        opts.height = CHART_H
        opts.cursor = js("({ show: false })")
        opts.legend = js("({ show: false })")
        // Fix the y-axis to the slider range so slots are visually comparable (no per-chart auto-zoom).
        opts.scales = js("({ x: { time: false }, y: { range: [0, 1] } })")
        opts.axes = arrayOf(js("({ show: false })"), js("({ show: false })"))
        opts.series = arrayOf(js("({})"), seriesOpts(Faction.ENL), seriesOpts(Faction.RES), overlapSeriesOpts())
        val empty: dynamic = arrayOf(arrayOf<Double>(), arrayOf<Double>(), arrayOf<Double>(), arrayOf<Double>())
        return UPlot(opts, empty, target)
    }

    private fun seriesOpts(faction: Faction): dynamic {
        val s: dynamic = js("({})")
        s.stroke = faction.color
        s.width = 1.5
        s.fill = faction.fieldStyle + FILL_ALPHA
        s.points = js("({ show: false })")
        return s
    }

    private fun overlapSeriesOpts(): dynamic {
        val s: dynamic = js("({})")
        s.stroke = OVERLAP_COLOR
        s.width = 1.5
        s.points = js("({ show: false })")
        return s
    }

    // Midpoint of two "#rrggbb" colours as an "rgb(r, g, b)" string (the faction-agnostic overlap colour).
    private fun blendHex(a: String, b: String): String {
        fun channel(hex: String, i: Int) = hex.substring(1 + i * 2, 3 + i * 2).toInt(16)
        val r = (channel(a, 0) + channel(b, 0)) / 2
        val g = (channel(a, 1) + channel(b, 1)) / 2
        val bl = (channel(a, 2) + channel(b, 2)) / 2
        return "rgb($r, $g, $bl)"
    }

    private fun pct(value: Double): String = "${(value * 100.0).toInt()}%"

    private fun el(cls: String): HTMLElement {
        val e = document.createElement("div") as HTMLElement
        e.className = cls
        return e
    }
}
