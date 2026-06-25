package util.ui

import agent.Faction
import ai.Observation
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * The **AI** footer tab (PLAN Phase 6): per faction, a live readout of its **observation** — the normalized
 * world feature vector ([Observation]) a net/LLM receives, as labelled 0–1 bars. The driver picker moved to
 * the footer header ([DriverControls], reachable from any tab); this stays the transparency surface. Rebuilt
 * lazily; the bars refresh each frame from [DrawUtil.redrawUserInterface].
 */
object AiPanel {
    // One label per Observation slot — keep in order/length sync with Observation.observe().
    private val OBS_LABELS = listOf(
        "Cycle", "Mind Units", "Portals", "Portals (foe)", "Neutral",
        "Links", "Fields", "Roster", "Roster (foe)",
        "Avg level", "Avg level (foe)", "Avg XM", "Avg XM (foe)",
    )

    private var built = false
    private val bars = mutableMapOf<Faction, Array<HTMLElement?>>()
    private val values = mutableMapOf<Faction, Array<HTMLElement?>>()

    /** Refresh each faction's observation bars from current world state. */
    fun update() {
        ensure()
        if (!built) return
        Faction.all().forEach { faction ->
            val obs = Observation.observe(faction)
            val factionBars = bars[faction] ?: return@forEach
            val factionVals = values[faction] ?: return@forEach
            obs.forEachIndexed { i, v ->
                factionBars.getOrNull(i)?.style?.width = "${(v * 100.0).toInt()}%"
                factionVals.getOrNull(i)?.textContent = "${(v * 100.0).toInt()}%"
            }
        }
    }

    private fun ensure() {
        if (built) return
        if (document.body == null) return
        built = true
        val panel = el("div", "aiPanel")
        Faction.all().forEach { faction -> panel.appendChild(factionColumn(faction)) }
        Footer.tab("ai").appendChild(panel)
    }

    private fun factionColumn(faction: Faction): HTMLElement {
        val col = el("div", "aiCol")
        val head = el("div", "aiColHead")
        val name = el("span", "aiFactionName").also {
            it.textContent = faction.abbr
            it.style.color = faction.color
        }
        head.appendChild(name)
        col.appendChild(head)

        val barArr = arrayOfNulls<HTMLElement>(Observation.SIZE)
        val valArr = arrayOfNulls<HTMLElement>(Observation.SIZE)
        for (i in 0 until Observation.SIZE) {
            val row = el("div", "aiObsRow")
            row.appendChild(el("span", "aiObsLabel").also { it.textContent = OBS_LABELS.getOrElse(i) { "f$i" } })
            val track = el("span", "aiObsTrack")
            val bar = el("span", "aiObsBar").also { it.style.background = faction.color }
            track.appendChild(bar)
            row.appendChild(track)
            val value = el("span", "aiObsValue")
            row.appendChild(value)
            col.appendChild(row)
            barArr[i] = bar
            valArr[i] = value
        }
        bars[faction] = barArr
        values[faction] = valArr
        return col
    }

    private fun el(tag: String, cls: String): HTMLElement {
        val e = document.createElement(tag) as HTMLElement
        e.className = cls
        return e
    }
}
