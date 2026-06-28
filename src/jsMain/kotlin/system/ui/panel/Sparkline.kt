package system.ui.panel

import agent.Faction
import external.UPlot
import org.w3c.dom.HTMLElement
import util.ColorUtil

/**
 * Shared uPlot sparkline config for the two checkpoint-history panels ([HistoryPanel] and
 * [SliderHistoryPanel]). Both draw the same faction-tinted style: an ENL line, a RES line, and a neutral
 * OVERLAP line on top where the two coincide. Only their scales + chart size differ, so those stay per-panel
 * (passed in); everything else — the series styling, the translucent fills, the blended overlap colour, the
 * 4-series layout — lives here once.
 */
internal object Sparkline {
    private const val FILL_ALPHA = "0.16)" // appended to a faction's "rgba(r, g, b, " prefix
    private const val STROKE_W = 1.5

    // The exact midpoint between the two faction colours, used for the OVERLAP line (drawn on top where the
    // two series coincide) so the chart is faction-agnostic — without it, whichever faction is drawn last
    // (RES) always wins the colour where the lines sit on top of each other, even though the values are equal.
    private val OVERLAP_COLOR = ColorUtil.blendHex(Faction.ENL.color, Faction.RES.color)

    /**
     * A uPlot instance with the shared 4-series layout (x, ENL, RES, overlap) and chrome-less look; the caller
     * supplies the panel-specific [width]/[height] and uPlot [scales] (e.g. a fixed `y: [0, 1]` range).
     */
    fun plot(target: HTMLElement, width: Int, height: Int, scales: dynamic): UPlot {
        val opts: dynamic = js("({})")
        opts.width = width
        opts.height = height
        opts.cursor = js("({ show: false })")
        opts.legend = js("({ show: false })")
        opts.scales = scales
        opts.axes = arrayOf(js("({ show: false })"), js("({ show: false })"))
        opts.series = arrayOf(js("({})"), factionSeries(Faction.ENL), factionSeries(Faction.RES), overlapSeries())
        val empty: dynamic = arrayOf(arrayOf<Double>(), arrayOf<Double>(), arrayOf<Double>(), arrayOf<Double>())
        return UPlot(opts, empty, target)
    }

    /** The overlap series for a checkpoint window: each faction's value only where the two are EQUAL, else a
     *  gap (null) — so coincident lines render in the blended [OVERLAP_COLOR] instead of the last-drawn one. */
    fun overlapOf(enl: Array<Double>, res: Array<Double>): Array<Double?> = Array(enl.size) { if (enl[it] == res[it]) enl[it] else null }

    private fun factionSeries(faction: Faction): dynamic {
        val s: dynamic = js("({})")
        s.stroke = faction.color
        s.width = STROKE_W
        s.fill = faction.fieldStyle + FILL_ALPHA // faction-tinted translucent area (maximalist look)
        s.points = js("({ show: false })")
        return s
    }

    // Drawn last, on top of both faction lines: a neutral blended stroke only where ENL == RES (gaps
    // elsewhere). No fill — the two faction area fills already stack to a blend where they overlap.
    private fun overlapSeries(): dynamic {
        val s: dynamic = js("({})")
        s.stroke = OVERLAP_COLOR
        s.width = STROKE_W
        s.points = js("({ show: false })")
        return s
    }
}
