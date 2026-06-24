package util.ui

import World
import agent.Faction
import external.UPlot
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import system.Checkpoint
import system.Cycle

/**
 * Right-side **history dashboard** (DOM + uPlot): one row per metric — Covered area (MU), Portals,
 * Links, Fields, Agents — each showing the live ENL/RES values plus a faction-tinted sparkline of
 * the rolling checkpoint window. Unifies the old single MU chart ([CycleChart], retired) and the
 * separate counts table into one panel, and tracks every metric over time (not just MU).
 *
 * [update] runs once per frame from [util.DrawUtil.redrawUserInterface]: the header numbers refresh
 * every frame (cheap text sets), but the sparklines only re-feed uPlot when a checkpoint changed.
 */
object HistoryPanel {
    private const val CONTAINER_ID = "historyPanel"
    private const val CHART_W = 212
    private const val CHART_H = 42
    private const val FILL_ALPHA = "0.16)" // appended to a faction's "rgba(r, g, b, " prefix

    // The exact midpoint between the two faction colours, used for the OVERLAP line (drawn on top where the
    // two series coincide) so the chart is faction-agnostic — without it, whichever faction is drawn last
    // (RES) always wins the colour where the lines sit on top of each other, even though the values are equal.
    private val OVERLAP_COLOR = blendHex(Faction.ENL.color, Faction.RES.color)

    /** A dashboard row: a title, the live per-faction value, and the per-checkpoint history value. */
    private class Metric(
        val title: String,
        val enlNow: () -> Int,
        val resNow: () -> Int,
        val enlAt: (Checkpoint) -> Int,
        val resAt: (Checkpoint) -> Int,
    )

    private val metrics = listOf(
        Metric("Mind Units", { World.calcTotalMu(Faction.ENL) }, { World.calcTotalMu(Faction.RES) }, { it.enlMu }, { it.resMu }),
        // Fields sits right under MU (MU = field area, so it's the headline's direct driver).
        Metric("Fields", { World.countFields(Faction.ENL) }, { World.countFields(Faction.RES) }, { it.enlFields }, { it.resFields }),
        Metric("Links", { World.countLinks(Faction.ENL) }, { World.countLinks(Faction.RES) }, { it.enlLinks }, { it.resLinks }),
        Metric("Portals", { World.countPortals(Faction.ENL) }, { World.countPortals(Faction.RES) }, { it.enlPortals }, { it.resPortals }),
        Metric("Agents", { World.countAgents(Faction.ENL) }, { World.countAgents(Faction.RES) }, { it.enlAgents }, { it.resAgents }),
    )

    private var built = false
    private val plots = arrayOfNulls<UPlot>(metrics.size)
    private val enlVals = arrayOfNulls<HTMLElement>(metrics.size)
    private val resVals = arrayOfNulls<HTMLElement>(metrics.size)
    private var lastKey = ""

    fun update() {
        if (!ensure()) return
        metrics.forEachIndexed { i, m ->
            enlVals[i]?.textContent = m.enlNow().toString()
            resVals[i]?.textContent = m.resNow().toString()
        }
        val cps = Cycle.INSTANCE.checkpoints.toList().sortedBy { it.first }
        val key = "${cps.size}:${cps.lastOrNull()?.first ?: 0}"
        if (key == lastKey) return // history unchanged — skip the sparkline re-feed
        lastKey = key
        val xs = cps.indices.map { it.toDouble() }.toTypedArray()
        metrics.forEachIndexed { i, m ->
            val enl = cps.map { m.enlAt(it.second).toDouble() }.toTypedArray()
            val res = cps.map { m.resAt(it.second).toDouble() }.toTypedArray()
            // Overlap series: the value only where the two factions are EQUAL, else a gap (null) — drawn on
            // top in the blended colour so coincident lines read as neutral, not whichever was drawn last.
            val overlap: Array<Double?> = Array(enl.size) { if (enl[it] == res[it]) enl[it] else null }
            plots[i]?.setData(arrayOf(xs, enl, res, overlap))
        }
    }

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
        if (js("typeof uPlot === 'undefined'").unsafeCast<Boolean>()) return false // CDN not ready
        val container = el("historyPanel")
        container.id = CONTAINER_ID
        metrics.forEachIndexed { i, m ->
            val row = el("historyRow")
            val head = el("historyHead")
            val title = el("historyTitle")
            title.textContent = m.title
            head.appendChild(title)
            val enl = el("historyVal")
            enl.style.color = Faction.ENL.color
            head.appendChild(enl)
            enlVals[i] = enl
            val res = el("historyVal")
            res.style.color = Faction.RES.color
            head.appendChild(res)
            resVals[i] = res
            row.appendChild(head)
            val chart = el("historyChart")
            row.appendChild(chart)
            container.appendChild(row)
            plots[i] = makePlot(chart)
        }
        Hud.right().appendChild(container) // intel column (right)
        built = true
        return true
    }

    private fun makePlot(target: HTMLElement): UPlot {
        val opts: dynamic = js("({})")
        opts.width = CHART_W
        opts.height = CHART_H
        opts.cursor = js("({ show: false })")
        opts.legend = js("({ show: false })")
        opts.scales = js("({ x: { time: false } })")
        opts.axes = arrayOf(js("({ show: false })"), js("({ show: false })"))
        opts.series = arrayOf(js("({})"), seriesOpts(Faction.ENL), seriesOpts(Faction.RES), overlapSeriesOpts())
        val empty: dynamic = arrayOf(arrayOf<Double>(), arrayOf<Double>(), arrayOf<Double>(), arrayOf<Double>())
        return UPlot(opts, empty, target)
    }

    private fun seriesOpts(faction: Faction): dynamic {
        val s: dynamic = js("({})")
        s.stroke = faction.color
        s.width = 1.5
        s.fill = faction.fieldStyle + FILL_ALPHA // faction-tinted translucent area (maximalist look)
        s.points = js("({ show: false })")
        return s
    }

    // Drawn last, on top of both faction lines: a neutral blended stroke only where ENL == RES (gaps
    // elsewhere). No fill — the two faction area fills already stack to a blend where they overlap.
    private fun overlapSeriesOpts(): dynamic {
        val s: dynamic = js("({})")
        s.stroke = OVERLAP_COLOR
        s.width = 1.5
        s.points = js("({ show: false })")
        return s
    }

    // Midpoint of two "#rrggbb" colours as an "rgb(r, g, b)" string.
    private fun blendHex(a: String, b: String): String {
        fun channel(hex: String, i: Int) = hex.substring(1 + i * 2, 3 + i * 2).toInt(16)
        val r = (channel(a, 0) + channel(b, 0)) / 2
        val g = (channel(a, 1) + channel(b, 1)) / 2
        val bl = (channel(a, 2) + channel(b, 2)) / 2
        return "rgb($r, $g, $bl)"
    }

    private fun el(cls: String): HTMLElement {
        val e = document.createElement("div") as HTMLElement
        e.className = cls
        return e
    }
}
