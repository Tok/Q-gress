package util.ui

import World
import agent.Faction
import config.Config
import config.Time
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import portal.XmMap
import system.Com

/**
 * DOM HUD for the always-on stats. The **scoreboard** (bottom-left) is a fixed-width grid: per faction
 * a tag + a proportional MU bar + the value, so the two faction rows line up in columns and never
 * reflow; a footer row shows time / tick / stray-XM. The **action log** is a collapsible section in
 * the right "intel" column. Per-faction entity counts + the MU time-series live in [HistoryPanel].
 *
 * [update] is called once per frame from [util.DrawUtil.redrawUserInterface]; it builds the DOM lazily
 * on first call and then just writes text/width. The Com list is only rebuilt when its contents change.
 */
object StatsPanel {
    private var built = false
    private val muBars = arrayOfNulls<HTMLElement>(2)
    private val muValues = arrayOfNulls<HTMLElement>(2)
    private val muTags = arrayOfNulls<HTMLElement>(2)
    private var timeEl: HTMLElement? = null
    private var tickEl: HTMLElement? = null
    private var xmEl: HTMLElement? = null
    private var comPanel: HTMLElement? = null
    private var lastComKey = ""
    private var onlyKey = false // EVENT LOG filter: when on, show only MAJOR ("key") events

    /** Refresh the panels from current world state (faction MU passed in, like the old draw). */
    fun update(firstMu: Int, secondMu: Int, factions: Pair<Faction, Faction>) {
        build()
        val (f1, f2) = factions
        val total = (firstMu + secondMu).coerceAtLeast(1)
        applyMuRow(0, f1, firstMu, firstMu.toDouble() / total)
        applyMuRow(1, f2, secondMu, secondMu.toDouble() / total)
        timeEl?.textContent = Time.ticksToTimestamp(World.tick)
        tickEl?.textContent = "Tick ${World.tick}"
        xmEl?.textContent = "XM ${compact(XmMap.all().values.sumOf { it.xm })}"
        updateCom()
    }

    private fun applyMuRow(row: Int, faction: Faction, mu: Int, fraction: Double) {
        muTags[row]?.let {
            it.textContent = faction.abbr
            it.style.color = faction.color
        }
        muBars[row]?.let {
            it.style.width = "${(fraction * 100.0)}%"
            it.style.background = faction.color
        }
        muValues[row]?.let {
            it.textContent = "${compact(mu)} MU"
            it.style.color = faction.color
        }
    }

    /** Compact large counts so the fixed-width value column never reflows: 1234 → "1.2k". */
    private fun compact(n: Int): String = if (n >= 1000) "${(n / 100) / 10.0}k" else n.toString()

    private fun updateCom() {
        val panel = comPanel ?: return
        val all = Com.entries()
        val filtered = if (onlyKey) all.filter { it.importance == Com.Importance.MAJOR } else all
        val expanded = Footer.isExpanded()
        // Collapsed → just the last few lines; expanded → the whole (scrolling) backlog.
        val shown = if (expanded) filtered else filtered.takeLast(Config.comMessageLimit)
        val key = "$onlyKey:$expanded:${all.size}:${all.lastOrNull()?.segments?.joinToString { it.text } ?: ""}"
        if (key == lastComKey) return // unchanged — skip the rebuild
        lastComKey = key
        panel.textContent = "" // clear, then re-add oldest→newest (newest sits at the bottom)
        shown.forEach { entry ->
            val line = el("statsComLine")
            if (entry.importance == Com.Importance.MAJOR) line.classList.add("statsComMajor")
            entry.segments.forEach { seg ->
                val span = document.createElement("span") as HTMLElement
                span.textContent = seg.text
                seg.color?.let { span.style.color = it }
                line.appendChild(span)
            }
            panel.appendChild(line)
        }
    }

    private fun build() {
        if (built) return
        if (document.body == null) return
        built = true

        val panel = el("scorePanel") // fixed bottom-left (see CSS)
        repeat(2) { row ->
            val r = el("scoreRow")
            val tag = el("scoreTag").also { muTags[row] = it }
            val track = el("scoreTrack")
            val bar = el("scoreBar").also { muBars[row] = it }
            track.appendChild(bar)
            val value = el("scoreValue").also { muValues[row] = it }
            r.appendChild(tag)
            r.appendChild(track)
            r.appendChild(value)
            panel.appendChild(r)
        }
        val footer = el("scoreFooter")
        timeEl = el("scoreTime").also { footer.appendChild(it) }
        tickEl = el("scoreTick").also { footer.appendChild(it) }
        xmEl = el("scoreXm").also { footer.appendChild(it) }
        panel.appendChild(footer)
        Hud.top().appendChild(panel)

        // Action log — the footer's EVENT LOG tab provides the label + collapse chevron. A filter row lets the
        // player hide the routine (MINOR) events and keep only the key (MAJOR) ones.
        val logTab = Footer.tab("log")
        logTab.appendChild(buildLogFilter())
        comPanel = el("statsComLines").also { logTab.appendChild(it) }
    }

    private fun buildLogFilter(): HTMLElement {
        val row = el("logFilter")
        val box = document.createElement("input") as org.w3c.dom.HTMLInputElement
        box.type = "checkbox"
        box.checked = onlyKey
        box.onchange = {
            onlyKey = box.checked
            lastComKey = "" // force a rebuild on the next frame
            null
        }
        row.appendChild(box)
        row.appendChild(
            el("logFilterLabel").also {
                it.textContent = " Only key events (fields, captures, checkpoints, recruiting, portals)"
            },
        )
        return row
    }

    private fun el(cls: String): HTMLElement {
        val e = document.createElement("div") as HTMLElement
        e.className = cls
        return e
    }
}
