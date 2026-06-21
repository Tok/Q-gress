package util.ui

import World
import agent.Faction
import config.Time
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import system.Com

/**
 * DOM HUD for the always-on stats: the **MU bars** ("covered area", bottom-left), the **time/tick**
 * row, and the **action log** (bottom-right). Per-faction entity counts moved to [HistoryPanel]
 * (live values + history); the MU time-series likewise lives there now.
 *
 * [update] is called once per frame from [util.DrawUtil.redrawUserInterface]; it builds the DOM
 * lazily on first call and then just writes text/width, so per-frame cost is a handful of property
 * sets. The Com list is only rebuilt when its contents actually change.
 */
object StatsPanel {
    private const val MU_BAR_MAX_PX = 150.0 // full-faction MU bar width

    private var built = false
    private val muBars = arrayOfNulls<HTMLElement>(2)
    private val muLabels = arrayOfNulls<HTMLElement>(2)
    private var timeEl: HTMLElement? = null
    private var tickEl: HTMLElement? = null
    private var comPanel: HTMLElement? = null
    private var lastComKey = ""

    /** Refresh the panels from current world state (faction MU passed in, like the old draw). */
    fun update(firstMu: Int, secondMu: Int, factions: Pair<Faction, Faction>) {
        build()
        val (f1, f2) = factions
        updateMindUnits(firstMu, secondMu, f1, f2)
        timeEl?.textContent = Time.ticksToTimestamp(World.tick)
        tickEl?.textContent = "Tick: ${World.tick}"
        // Per-faction entity counts moved to the history dashboard (HistoryPanel — live values + trend).
        updateCom()
    }

    private fun updateMindUnits(firstMu: Int, secondMu: Int, f1: Faction, f2: Faction) {
        val total = (firstMu + secondMu).coerceAtLeast(1)
        applyMuRow(0, f1, firstMu, firstMu.toDouble() / total)
        applyMuRow(1, f2, secondMu, secondMu.toDouble() / total)
    }

    private fun applyMuRow(row: Int, faction: Faction, mu: Int, fraction: Double) {
        muBars[row]?.let {
            it.style.width = "${(fraction * MU_BAR_MAX_PX)}px"
            it.style.background = faction.color
        }
        muLabels[row]?.let {
            it.textContent = "${faction.abbr} ${mu}M"
            it.style.color = faction.color
        }
    }

    private fun updateCom() {
        val panel = comPanel ?: return
        val msgs = Com.currentMessages()
        val key = "${msgs.size}:${msgs.lastOrNull() ?: ""}"
        if (key == lastComKey) return // unchanged — skip the rebuild
        lastComKey = key
        panel.textContent = "" // clear, then re-add oldest→newest (newest sits at the bottom)
        msgs.forEach { m ->
            val line = el("statsComLine")
            line.textContent = m
            panel.appendChild(line)
        }
    }

    private fun build() {
        if (built) return
        val body = document.body ?: return
        built = true

        val muPanel = el("statsMuPanel")
        repeat(2) { row ->
            val r = el("statsMuRow")
            val track = el("statsMuTrack")
            val bar = el("statsMuBar")
            track.appendChild(bar)
            muBars[row] = bar
            val label = el("statsMuLabel")
            muLabels[row] = label
            r.appendChild(track)
            r.appendChild(label)
            muPanel.appendChild(r)
        }
        val tickRow = el("statsTickRow")
        timeEl = el("statsTime").also { tickRow.appendChild(it) }
        tickEl = el("statsTick").also { tickRow.appendChild(it) }
        muPanel.appendChild(tickRow)
        body.appendChild(muPanel)

        // Action log (bottom-right): a titled log section.
        val comWrap = el("statsComPanel")
        val comTitle = el("statsComTitle")
        comTitle.textContent = "LOG"
        comWrap.appendChild(comTitle)
        comPanel = el("statsComLines").also { comWrap.appendChild(it) }
        body.appendChild(comWrap)
    }

    private fun el(cls: String): HTMLElement {
        val e = document.createElement("div") as HTMLElement
        e.className = cls
        return e
    }
}
