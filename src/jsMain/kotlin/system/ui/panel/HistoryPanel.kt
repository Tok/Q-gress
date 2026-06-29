package system.ui.panel

import World
import agent.Faction
import external.UPlot
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import system.Checkpoint
import system.Cycle
import system.ui.Hud
import system.ui.el

/**
 * Right-side **history dashboard** (DOM + uPlot): one row per metric — Covered area (MU), Portals,
 * Links, Fields, Agents — each showing the live ENL/RES values plus a faction-tinted sparkline of
 * the rolling checkpoint window. Unifies the old single MU chart ([CycleChart], retired) and the
 * separate counts table into one panel, and tracks every metric over time (not just MU).
 *
 * [update] runs once per frame from [util.HudRenderer.redrawUserInterface]: the header numbers refresh
 * every frame (cheap text sets), but the sparklines only re-feed uPlot when a checkpoint changed.
 */
object HistoryPanel {
    private const val CONTAINER_ID = "historyPanel"
    private const val CHART_W = 212
    private const val CHART_H = 42

    /** A dashboard row: a title, the live per-faction values, and the per-checkpoint history value. The
     *  white header total is [totalNow] = ENL+RES by default; [totalOverride] supplies the true board count
     *  where that isn't the whole story (Portals — which also has NEUTRAL ones not in either colour). */
    private class Metric(
        val title: String,
        val enlNow: () -> Int,
        val resNow: () -> Int,
        val enlAt: (Checkpoint) -> Int,
        val resAt: (Checkpoint) -> Int,
        val totalOverride: (() -> Int)? = null,
    ) {
        fun totalNow(): Int = totalOverride?.invoke() ?: (enlNow() + resNow())
    }

    private val metrics = listOf(
        Metric("Mind Units", { World.calcTotalMu(Faction.ENL) }, { World.calcTotalMu(Faction.RES) }, { it.enlMu }, { it.resMu }),
        // Fields sits right under MU (MU = field area, so it's the headline's direct driver).
        Metric("Fields", { World.countFields(Faction.ENL) }, { World.countFields(Faction.RES) }, { it.enlFields }, { it.resFields }),
        Metric("Links", { World.countLinks(Faction.ENL) }, { World.countLinks(Faction.RES) }, { it.enlLinks }, { it.resLinks }),
        Metric(
            "Portals",
            { World.countPortals(Faction.ENL) },
            { World.countPortals(Faction.RES) },
            { it.enlPortals },
            { it.resPortals },
            totalOverride = { World.countPortals() }, // includes neutral portals (not in either colour)
        ),
        Metric("Agents", { World.countAgents(Faction.ENL) }, { World.countAgents(Faction.RES) }, { it.enlAgents }, { it.resAgents }),
    )

    private const val TIE_COLOR = "#888888" // neutral grey winner-dot for a tie (e.g. 0–0 before any fields)

    private var built = false
    private val plots = arrayOfNulls<UPlot>(metrics.size)
    private val enlVals = arrayOfNulls<HTMLElement>(metrics.size)
    private val resVals = arrayOfNulls<HTMLElement>(metrics.size)
    private val totalVals = arrayOfNulls<HTMLElement>(metrics.size)
    private var muDots: HTMLElement? = null // the per-checkpoint winner strip under the MU graph
    private var lastKey = ""

    /** Per-checkpoint winner: the faction colour of whoever held more MU at that checkpoint (margin
     *  doesn't matter — only who led); neutral grey when tied. A row of these IS the "winning team" readout. */
    private fun winnerColor(cp: Checkpoint): String = when {
        cp.enlMu > cp.resMu -> Faction.ENL.color
        cp.resMu > cp.enlMu -> Faction.RES.color
        else -> TIE_COLOR
    }

    fun update() {
        if (!ensure()) return
        metrics.forEachIndexed { i, m ->
            enlVals[i]?.textContent = m.enlNow().toString()
            resVals[i]?.textContent = m.resNow().toString()
            totalVals[i]?.textContent = m.totalNow().toString()
        }
        val cps = Cycle.INSTANCE.checkpoints.toList().sortedBy { it.first }
        val key = "${cps.size}:${cps.lastOrNull()?.first ?: 0}"
        if (key == lastKey) return // history unchanged — skip the sparkline re-feed
        lastKey = key
        val xs = cps.indices.map { it.toDouble() }.toTypedArray()
        metrics.forEachIndexed { i, m ->
            val enl = cps.map { m.enlAt(it.second).toDouble() }.toTypedArray()
            val res = cps.map { m.resAt(it.second).toDouble() }.toTypedArray()
            plots[i]?.setData(arrayOf(xs, enl, res, Sparkline.overlapOf(enl, res)))
        }
        rebuildWinnerDots(cps.map { it.second })
    }

    // Repaint the MU winner strip: one dot per checkpoint, coloured by who led. Cheap (≤35 spans), and only
    // runs when the checkpoint window actually changed (same guard as the sparkline re-feed above).
    private fun rebuildWinnerDots(cps: List<Checkpoint>) {
        val strip = muDots ?: return
        strip.textContent = ""
        cps.forEach { cp ->
            val dot = el("historyDot")
            dot.style.background = winnerColor(cp)
            strip.appendChild(dot)
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
            val total = el("historyVal")
            total.classList.add("historyTotal") // white combined total, at-a-glance count
            head.appendChild(total)
            totalVals[i] = total
            row.appendChild(head)
            val chart = el("historyChart")
            row.appendChild(chart)
            // Under the MU graph only: the per-checkpoint winner-dot strip (the "winning team" readout).
            if (i == 0) {
                val dots = el("historyDots")
                row.appendChild(dots)
                muDots = dots
            }
            container.appendChild(row)
            plots[i] = makePlot(chart)
        }
        Hud.right().appendChild(container) // intel column (right)
        built = true
        return true
    }

    private fun makePlot(target: HTMLElement): UPlot = Sparkline.plot(target, CHART_W, CHART_H, js("({ x: { time: false } })"))
}
