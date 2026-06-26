package util.ui

import World
import agent.Agent
import config.Config
import items.deployable.DeployableItem
import items.level.LevelColor
import items.types.ShieldType
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * The **AGENTS** footer tab: a leaderboard of agents (both factions mixed) as a DOM table. Columns: #, XM, AP,
 * Agent (faction colour), XMPs/Resos/Cubes/Shields (count + a per-level bar strip in rarity/level colours),
 * Keys, Action, Portal. **Sortable** — click any column header to sort by it (toggles asc/desc). Collapsed it
 * shows the top [ROWS]; when the footer is **expanded** ([Footer.isExpanded]) it lists **every** agent (the
 * body scrolls). Rebuilt each frame from the sorted roster.
 */
object TopAgentsPanel {
    private const val ROWS = Config.topAgentsMessageLimit
    private const val MAX_DEPLOY_LEVEL = 8
    private const val MAX_SHIELD_LEVEL = 4
    private const val BAR_MAX_PX = 11

    // A column: a header, an optional numeric/string sort key (null → not sortable), and how to render its cell.
    private class Col(
        val header: String,
        val num: ((Agent) -> Double)? = null,
        val str: ((Agent) -> String)? = null,
        val render: (Agent) -> HTMLElement,
    ) {
        val sortable get() = num != null || str != null
    }

    private val COLS = listOf(
        Col("XM", num = { it.xm.toDouble() }, render = { a -> cell(a.xm.toString(), "taNum") }),
        Col("AP", num = { it.ap.toDouble() }, render = { a -> cell(a.ap.toString(), "taNum") }),
        Col("Agent", str = { it.faction.abbr + it.name }, render = { a -> nameCell(a) }),
        Col("XMPs", num = {
            it.inventory.findXmps().size.toDouble()
        }, render = { a -> invCell(a.inventory.findXmps(), MAX_DEPLOY_LEVEL, ::deployColor) }),
        Col("Resos", num = {
            it.inventory.findResonators().size.toDouble()
        }, render = { a -> invCell(a.inventory.findResonators(), MAX_DEPLOY_LEVEL, ::deployColor) }),
        Col("Cubes", num = {
            it.inventory.findPowerCubes().size.toDouble()
        }, render = { a -> invCell(a.inventory.findPowerCubes(), MAX_DEPLOY_LEVEL, ::deployColor) }),
        Col("Shields", num = {
            it.inventory.findShields().size.toDouble()
        }, render = { a -> invCell(a.inventory.findShields(), MAX_SHIELD_LEVEL, ShieldType::getColorForLevel) }),
        Col("Keys", num = { it.inventory.keyCount().toDouble() }, render = { a -> cell(a.inventory.keyCount().toString(), "taNum") }),
        Col("Action", str = { it.action.item.text }, render = { a -> cell(a.action.item.text, "taCell") }),
        Col("Portal", str = { it.actionPortal.name }, render = { a -> cell(a.actionPortal.name, "taCell") }),
    )

    private val DEFAULT_SORT = COLS.indexOfFirst { it.header == "AP" }.coerceAtLeast(0) // sort by AP by default
    private var sortIndex = DEFAULT_SORT
    private var sortAsc = false // AP descending by default

    private var tbody: HTMLElement? = null
    private val headerCells = mutableListOf<HTMLElement>()

    /** Rebuild the table body from the current roster (sorted by the chosen column). */
    fun update() {
        ensure()
        val body = tbody ?: return
        refreshHeaders()
        body.textContent = ""
        val ordered = sortedAgents()
        val limit = if (Footer.isExpanded()) ordered.size else ROWS // expanded → every agent (the body scrolls)
        ordered.take(limit).forEach { agent ->
            val row = el("tr", "taRow")
            COLS.forEach { row.appendChild(it.render(agent)) }
            body.appendChild(row)
        }
    }

    private fun sortedAgents(): List<Agent> {
        val agents = World.allAgents.toList()
        val col = COLS[sortIndex]
        val sorted = when {
            col.num != null -> agents.sortedBy { col.num.invoke(it) }
            col.str != null -> agents.sortedBy { col.str.invoke(it) }
            else -> agents.sortedBy { it.ap.toDouble() }
        }
        return if (sortAsc) sorted else sorted.asReversed()
    }

    // Header click: re-sort by [idx] — toggle direction if it's already the sort column, else select it
    // (numbers default to descending, strings to ascending).
    private fun sortBy(idx: Int) {
        val col = COLS[idx]
        if (!col.sortable) return
        if (sortIndex == idx) {
            sortAsc = !sortAsc
        } else {
            sortIndex = idx
            sortAsc = col.str != null
        }
        update() // immediate feedback (even while the sim is paused)
    }

    private fun refreshHeaders() {
        COLS.forEachIndexed { idx, c ->
            val arrow = if (idx == sortIndex) (if (sortAsc) " ▲" else " ▼") else ""
            headerCells.getOrNull(idx)?.textContent = c.header + arrow
        }
    }

    private fun nameCell(agent: Agent): HTMLElement = cell(agent.name, "taName").also { it.style.color = agent.faction.color }

    private fun deployColor(level: Int): String = LevelColor.map[level] ?: "#ffffff"

    /** A count + a per-level bar strip (height = count, colour = level via [colorFor]). */
    private fun invCell(items: List<DeployableItem>, maxLevel: Int, colorFor: (Int) -> String): HTMLElement {
        val td = el("td", "taInv")
        val count = el("span", "taInvCount")
        count.textContent = items.size.toString()
        td.appendChild(count)
        if (items.isNotEmpty()) {
            val byLevel = items.groupBy { it.getLevel() }.mapValues { it.value.size }
            val maxCount = byLevel.values.maxOrNull() ?: 1
            val bars = el("span", "taInvBars")
            for (lvl in 1..maxLevel) {
                val c = byLevel[lvl] ?: 0
                val bar = el("span", "taInvBar")
                bar.style.height = "${(c.toDouble() / maxCount * BAR_MAX_PX).toInt()}px"
                if (c > 0) bar.style.background = colorFor(lvl)
                bars.appendChild(bar)
            }
            td.appendChild(bars)
        }
        return td
    }

    private fun ensure() {
        if (tbody != null) return
        if (document.body == null) return
        val container = el("div", "topAgents")
        val table = el("table", "taTable")
        val head = el("tr", "taHead")
        COLS.forEachIndexed { idx, c ->
            val th = el("th", "taHeadCell")
            th.textContent = c.header
            if (c.sortable) {
                th.classList.add("taSortable")
                th.onclick = {
                    sortBy(idx)
                    null
                }
            }
            head.appendChild(th)
            headerCells.add(th)
        }
        val thead = el("thead", "")
        thead.appendChild(head)
        table.appendChild(thead)
        val newBody = el("tbody", "")
        table.appendChild(newBody)
        container.appendChild(table)
        Footer.tab("agents").appendChild(container) // AGENTS footer tab (full width)
        tbody = newBody
    }

    private fun cell(text: String, cls: String): HTMLElement {
        val td = el("td", cls)
        td.textContent = text
        return td
    }

    private fun el(tag: String, cls: String): HTMLElement {
        val e = document.createElement(tag) as HTMLElement
        if (cls.isNotEmpty()) e.className = cls
        return e
    }
}
