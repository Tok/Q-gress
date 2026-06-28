package system.ui.panel

import World
import config.Config
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import portal.Portal
import system.ui.Footer
import system.ui.el

/**
 * The **PORTALS** footer tab: every portal (both factions + neutral) as a DOM table, styled like the AGENTS
 * tab. Columns: Portal (owner-faction colour), Faction, Level, Health, Resos (n/8), Mods (n/4), Links, Fields
 * and Owner. **Sortable** — click any column header to sort by it (toggles asc/desc). Collapsed it shows the
 * top [ROWS]; when the footer is **expanded** ([Footer.isExpanded]) it lists **every** portal (the body
 * scrolls). Rebuilt each frame from the sorted list.
 */
object PortalsPanel {
    private const val ROWS = Config.topAgentsMessageLimit
    private const val MAX_RESOS = 8
    private const val MAX_MODS = 4
    private const val NEUTRAL_COLOR = "#9a9a9a"

    // A column: a header, an optional numeric/string sort key (null → not sortable), and how to render its cell.
    private class Col(
        val header: String,
        val num: ((Portal) -> Double)? = null,
        val str: ((Portal) -> String)? = null,
        val render: (Portal) -> HTMLElement,
    ) {
        val sortable get() = num != null || str != null
    }

    private val COLS = listOf(
        Col("Portal", str = { it.name }, render = { p -> nameCell(p) }),
        Col("Faction", str = { factionAbbr(it) }, render = { p -> factionCell(p) }),
        Col("Lvl", num = { it.getLevel().toInt().toDouble() }, render = { p -> cell("L${p.getLevel().toInt()}", "taNum") }),
        Col("Health", num = { it.calcHealth().toDouble() }, render = { p -> cell("${p.calcHealth()}%", "taNum") }),
        Col("Resos", num = { it.numberOfResosLeft().toDouble() }, render = { p -> cell("${p.numberOfResosLeft()}/$MAX_RESOS", "taNum") }),
        Col("Mods", num = { it.modCount().toDouble() }, render = { p -> cell("${p.modCount()}/$MAX_MODS", "taNum") }),
        Col("Links", num = { it.links.count().toDouble() }, render = { p -> cell(p.links.count().toString(), "taNum") }),
        Col("Fields", num = { it.fields.count().toDouble() }, render = { p -> cell(p.fields.count().toString(), "taNum") }),
        Col("Owner", str = { it.owner?.name ?: "—" }, render = { p -> ownerCell(p) }),
    )

    private val DEFAULT_SORT = COLS.indexOfFirst { it.header == "Lvl" }.coerceAtLeast(0) // sort by level by default
    private var sortIndex = DEFAULT_SORT
    private var sortAsc = false // level descending by default

    private var tbody: HTMLElement? = null
    private val headerCells = mutableListOf<HTMLElement>()

    /** Rebuild the table body from the current portals (sorted by the chosen column). */
    fun update() {
        ensure()
        val body = tbody ?: return
        refreshHeaders()
        body.textContent = ""
        val ordered = sortedPortals()
        val limit = if (Footer.isExpanded()) ordered.size else ROWS // expanded → every portal (the body scrolls)
        ordered.take(limit).forEach { portal ->
            val row = el("tr", "taRow")
            COLS.forEach { row.appendChild(it.render(portal)) }
            body.appendChild(row)
        }
    }

    private fun sortedPortals(): List<Portal> {
        val portals = World.allPortals.toList()
        val col = COLS[sortIndex]
        // Tie-break on the (unique) portal name so equal-key rows keep a fixed order instead of jittering
        // with allPortals' iteration order frame to frame.
        val byCol = when {
            col.num != null -> compareBy<Portal> { col.num.invoke(it) }
            col.str != null -> compareBy<Portal> { col.str.invoke(it) }
            else -> compareBy<Portal> { it.getLevel().toInt().toDouble() }
        }
        val sorted = portals.sortedWith(byCol.thenBy { it.name })
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

    private fun factionAbbr(p: Portal): String = p.owner?.faction?.abbr ?: "—"
    private fun factionColor(p: Portal): String = p.owner?.faction?.color ?: NEUTRAL_COLOR

    private fun nameCell(portal: Portal): HTMLElement = cell(portal.name, "taName").also { it.style.color = factionColor(portal) }

    private fun factionCell(portal: Portal): HTMLElement =
        cell(factionAbbr(portal), "taCell").also { it.style.color = factionColor(portal) }

    private fun ownerCell(portal: Portal): HTMLElement = cell(portal.owner?.name ?: "—", "taCell")

    private fun ensure() {
        if (tbody != null) return
        if (document.body == null) return
        val container = el("div", "topAgents")
        val table = el("table", "taTable")
        val head = el("tr", "taHead")
        COLS.forEachIndexed { idx, c ->
            val th = el("th", "taHeadCell")
            th.textContent = c.header
            if (c.str != null) th.classList.add("taHeadLeft") // string columns left-justify (header + cells)
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
        Footer.tab("portals").appendChild(container) // PORTALS footer tab (full width)
        tbody = newBody
    }

    private fun cell(text: String, cls: String): HTMLElement {
        val td = el("td", cls)
        td.textContent = text
        return td
    }
}
