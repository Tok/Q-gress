package system.ui.panel

import World
import agent.Faction
import config.Config
import external.UPlot
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import system.Checkpoint
import system.CheckpointStats
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
    private var muTally: HTMLElement? = null // the "who won how many checkpoints/cycles" tally under the dots
    private var lastKey = ""

    // Shared with the MU sparkline's background draw hook: how many checkpoints are on the chart and which
    // indices are cycle ends, so it can paint a faint vertical per checkpoint + a stronger one per cycle.
    private var gridCount = 0
    private var gridCycleEnds: Set<Int> = emptySet()

    private const val CHECKPOINT_LINE_COLOR = "rgba(255, 255, 255, 0.07)" // faint per-checkpoint vertical
    private const val CYCLE_LINE_COLOR = "rgba(255, 255, 255, 0.30)" // stronger per-cycle vertical
    private const val CHECKPOINT_LINE_W = 1.0
    private const val CYCLE_LINE_W = 1.5

    /** Faction colour, or the neutral tie grey when there's no winner. */
    private fun colorOf(faction: Faction?): String = faction?.color ?: TIE_COLOR

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
        val history = cps.map { it.second }
        gridCount = history.size
        gridCycleEnds = history.indices.filter { history[it].isCycleEnd }.toSet()
        val xs = cps.indices.map { it.toDouble() }.toTypedArray()
        metrics.forEachIndexed { i, m ->
            val enl = cps.map { m.enlAt(it.second).toDouble() }.toTypedArray()
            val res = cps.map { m.resAt(it.second).toDouble() }.toTypedArray()
            plots[i]?.setData(arrayOf(xs, enl, res, Sparkline.overlapOf(enl, res)))
        }
        rebuildWinnerDots(history)
        updateTally(history)
    }

    // Repaint the MU winner strip: one dot per checkpoint (coloured by who led it), and at each cycle end a
    // 1.618× dot coloured by the CYCLE winner (whoever led the most checkpoints in it). Cheap (≤35 spans),
    // and only runs when the checkpoint window changed (same guard as the sparkline re-feed above).
    private fun rebuildWinnerDots(cps: List<Checkpoint>) {
        val strip = muDots ?: return
        strip.textContent = ""
        cps.forEachIndexed { i, cp ->
            val dot = el("historyDot")
            if (cp.isCycleEnd) {
                dot.classList.add("historyDotCycle") // 1.618× — a won CYCLE
                dot.style.background = colorOf(CheckpointStats.cycleWinner(cps, i))
            } else {
                dot.style.background = colorOf(CheckpointStats.winner(cp))
            }
            strip.appendChild(dot)
        }
    }

    // The "who's winning" readout under the dots: checkpoints each faction led, plus the current checkpoint's
    // position in the cycle (a white `N/35`) after the faction scores.
    private fun updateTally(cps: List<Checkpoint>) {
        val row = muTally ?: return
        val cp = CheckpointStats.tally(cps)
        row.textContent = ""
        row.appendChild(tallyText("checkpoint/cycle  "))
        row.appendChild(tallyNum(cp.getValue(Faction.ENL).toString(), Faction.ENL.color))
        row.appendChild(tallyText(" – "))
        row.appendChild(tallyNum(cp.getValue(Faction.RES).toString(), Faction.RES.color))
        row.appendChild(tallyText("  "))
        row.appendChild(tallyNum("${currentCyclePosition()}/${Config.checkpointsPerCycle}", "#ffffff")) // white: N of 35
    }

    // Which checkpoint of the current 35-checkpoint scoring cycle we're on (1…35), from the global tick.
    private fun currentCyclePosition(): Int {
        val n = World.tick / Config.ticksPerCheckpoint
        return if (n <= 0) 0 else ((n - 1) % Config.checkpointsPerCycle) + 1
    }

    private fun tallyText(text: String): HTMLElement = el("span", "historyTallyLabel").also { it.textContent = text }
    private fun tallyNum(text: String, color: String): HTMLElement = el("span", "historyTallyNum").also {
        it.textContent = text
        it.style.color = color
    }

    // The MU sparkline's background: a faint vertical per checkpoint + a stronger one at each cycle end, drawn
    // under the data (drawClear hook). Canvas-pixel coords via bbox + valToPos(..., true); line widths scaled
    // by the device pixel ratio so they stay crisp on hi-dpi.
    private fun drawMuGrid(u: dynamic) {
        if (gridCount <= 0) return
        val ctx = u.ctx
        val top = u.bbox.top.unsafeCast<Double>()
        val height = u.bbox.height.unsafeCast<Double>()
        val dpr = (u.pxRatio.unsafeCast<Double?>()) ?: 1.0
        ctx.save()
        for (i in 0 until gridCount) {
            val isCycle = gridCycleEnds.contains(i)
            val x = u.valToPos(i.toDouble(), "x", true).unsafeCast<Double>()
            ctx.beginPath()
            ctx.lineWidth = (if (isCycle) CYCLE_LINE_W else CHECKPOINT_LINE_W) * dpr
            ctx.strokeStyle = if (isCycle) CYCLE_LINE_COLOR else CHECKPOINT_LINE_COLOR
            ctx.moveTo(x, top)
            ctx.lineTo(x, top + height)
            ctx.stroke()
        }
        ctx.restore()
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
            // Under the MU graph only: the per-checkpoint winner-dot strip + the win tally (the "winning team" readout).
            if (i == 0) {
                val dots = el("historyDots")
                row.appendChild(dots)
                muDots = dots
                val tally = el("historyTally")
                row.appendChild(tally)
                muTally = tally
            }
            container.appendChild(row)
            plots[i] = makePlot(chart, i)
        }
        Hud.right().appendChild(container) // intel column (right)
        built = true
        return true
    }

    // MU (row 0) gets the checkpoint/cycle grid verticals beneath its sparkline.
    private fun makePlot(target: HTMLElement, index: Int): UPlot =
        Sparkline.plot(target, CHART_W, CHART_H, js("({ x: { time: false } })"), bgDraw = if (index == 0) ::drawMuGrid else null)
}
