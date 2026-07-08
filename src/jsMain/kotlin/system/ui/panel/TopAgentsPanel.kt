package system.ui.panel

import World
import agent.Agent
import agent.action.ActionIcons
import agent.action.ActionItem
import config.Config
import config.Sim
import items.deployable.DeployableItem
import items.level.LevelColor
import items.types.HeatSinkType
import items.types.MultihackType
import items.types.ShieldType
import items.types.VirusType
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import system.map.MapCamera
import system.ui.Footer
import system.ui.Inspector
import system.ui.el
import util.Profiler

/**
 * The **AGENTS** footer tab: a leaderboard of agents (both factions mixed) as a DOM table. Columns: #, XM, AP,
 * Agent (faction colour), then each carried item as a count + per-level bar strip in rarity/level colours
 * (XMPs, US, Resos, Cubes, Shields, Heat sinks, Multi-hacks), Keys + Unique keys, Action, Portal. **Sortable**
 * — click any column header to sort by it (toggles asc/desc). Collapsed it
 * shows the top [ROWS]; when the footer is **expanded** ([Footer.isExpanded]) it lists **every** agent (the
 * body scrolls). Rebuilt each frame from the sorted roster.
 */
object TopAgentsPanel {
    private const val ROWS = Config.topAgentsMessageLimit
    private const val MAX_DEPLOY_LEVEL = 8
    private const val MAX_SHIELD_LEVEL = 4
    private const val MAX_MOD_LEVEL = 3 // heat sinks / multi-hacks: Common / Rare / Very Rare
    private const val BAR_MAX_PX = 11
    private const val MPH_PER_MS = 2.2369363 // m/s → miles per hour (imperial hover)

    // A column: a header, an optional numeric/string sort key (null → not sortable), and how to render its cell.
    // [headerColor]/[headerTitle] let a header be a compact coloured glyph (e.g. the JARVIS/ADA flip dots).
    private class Col(
        val header: String,
        val num: ((Agent) -> Double)? = null,
        val str: ((Agent) -> String)? = null,
        val headerColor: String? = null,
        val headerTitle: String? = null,
        val render: (Agent) -> HTMLElement,
    ) {
        val sortable get() = num != null || str != null
    }

    private val COLS = listOf(
        Col("Lvl", num = { it.getLevel().toDouble() }, render = { a -> cell("L${a.getLevel()}", "taNum") }),
        Col("Agent", str = { it.faction.abbr + it.name }, render = { a -> nameCell(a) }),
        Col("AP", num = { it.ap.toDouble() }, render = { a -> cell(a.ap.toString(), "taNum") }),
        Col("XMPs", num = {
            it.inventory.findXmps().size.toDouble()
        }, render = { a -> invCell(a.inventory.findXmps(), MAX_DEPLOY_LEVEL, ::deployColor) }),
        Col("US", num = {
            it.inventory.findUltraStrikes().size.toDouble()
        }, render = { a -> invCell(a.inventory.findUltraStrikes(), MAX_DEPLOY_LEVEL, ::deployColor) }),
        Col("Resos", num = {
            it.inventory.findResonators().size.toDouble()
        }, render = { a -> invCell(a.inventory.findResonators(), MAX_DEPLOY_LEVEL, ::deployColor) }),
        Col("Cubes", num = {
            it.inventory.findPowerCubes().size.toDouble()
        }, render = { a -> invCell(a.inventory.findPowerCubes(), MAX_DEPLOY_LEVEL, ::deployColor) }),
        Col("Shields", num = {
            it.inventory.findShields().size.toDouble()
        }, render = { a -> invCell(a.inventory.findShields(), MAX_SHIELD_LEVEL, ShieldType::getColorForLevel) }),
        Col("Heat", num = {
            it.inventory.findHeatSinks().size.toDouble()
        }, render = { a -> invCell(a.inventory.findHeatSinks(), MAX_MOD_LEVEL, HeatSinkType::getColorForLevel) }),
        Col("MHack", num = {
            it.inventory.findMultihacks().size.toDouble()
        }, render = { a -> invCell(a.inventory.findMultihacks(), MAX_MOD_LEVEL, MultihackType::getColorForLevel) }),
        // Flip items split into their two kinds — a compact coloured dot header each (green JARVIS / blue ADA),
        // then a plain count. (Was one "Flip" column with both dots + a combined total.)
        Col(
            "●",
            headerColor = VirusType.JARVIS_VIRUS.color,
            headerTitle = "JARVIS viruses (flip a portal to ENL)",
            num = { virusCount(it, VirusType.JARVIS_VIRUS).toDouble() },
            render = { a -> cell(virusCount(a, VirusType.JARVIS_VIRUS).toString(), "taNum") },
        ),
        Col(
            "●",
            headerColor = VirusType.ADA_REFACTOR.color,
            headerTitle = "ADA refactors (flip a portal to RES)",
            num = { virusCount(it, VirusType.ADA_REFACTOR).toDouble() },
            render = { a -> cell(virusCount(a, VirusType.ADA_REFACTOR).toString(), "taNum") },
        ),
        Col("Keys", num = { it.inventory.keyCount().toDouble() }, render = { a -> cell(a.inventory.keyCount().toString(), "taNum") }),
        Col("Uniq", num = { uniqueKeys(it).toDouble() }, render = { a -> cell(uniqueKeys(a).toString(), "taNum") }),
        Col("Inv", num = { it.inventory.items.size.toDouble() }, render = { a -> invLimitCell(a) }),
        Col("XM", num = { it.xm.toDouble() }, render = { a -> cell(a.xm.toString(), "taNum") }),
        Col("m/s", num = { speedMs(it) }, render = { a -> speedCell(a) }),
        Col("Action", str = { actionLabel(it.action.item) }, render = { a -> actionCell(a) }),
        Col("Portal", str = { it.actionPortal.name }, render = { a -> cell(clip(a.actionPortal.name, PORTAL_NAME_MAX), "taCell") }),
    )

    private const val PORTAL_NAME_MAX = 20 // cut long portal names so the (fit-content) AGENTS pane can't widen

    /** Truncate [s] to [max] chars with an ellipsis, so a long action-portal name can't stretch the table. */
    private fun clip(s: String, max: Int): String = if (s.length <= max) s else s.take(max - 1) + "…"

    private val DEFAULT_SORT = COLS.indexOfFirst { it.header == "AP" }.coerceAtLeast(0) // sort by AP by default
    private var sortIndex = DEFAULT_SORT
    private var sortAsc = false // AP descending by default

    private var tbody: HTMLElement? = null
    private val headerCells = mutableListOf<HTMLElement>()

    // The table is DOM-heavy (a full teardown + rebuild of every row/cell); cap the periodic refresh to ~5 Hz
    // instead of every frame — the stats it shows don't need per-frame updates. Interactions (sort) call
    // rebuild() directly so they stay instant. [Profiler.nowMs] is the wall clock (0 outside the browser).
    private const val TABLE_REFRESH_MS = 200.0
    private var lastRebuildMs = 0.0

    /** Rebuild the table body from the current roster (sorted by the chosen column). Skips the rebuild when the
     *  AGENTS tab is hidden — the table is DOM-heavy and was rebuilt every frame even off-screen — and throttles
     *  the periodic refresh to [TABLE_REFRESH_MS]. Re-activating the tab rebuilds at once (the clock isn't
     *  advanced while hidden). */
    fun update() {
        ensure()
        if (!Footer.isActive("agents")) return
        val now = Profiler.nowMs()
        if (now - lastRebuildMs < TABLE_REFRESH_MS) return
        lastRebuildMs = now
        rebuild()
    }

    /** Build + populate the table once regardless of tab visibility — [LoadingOverlay] measures the populated
     *  pane to land the AGENTS morph, so it can't wait for the tab to be active. */
    fun forceRebuild() {
        ensure()
        rebuild()
    }

    private fun rebuild() {
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
        // Tie-break on the unique, stable agent name so equal-key rows (e.g. everyone at 0 AP early on) keep a
        // fixed order instead of jittering with World.allAgents' iteration order frame to frame.
        val byCol = when {
            col.num != null -> compareBy<Agent> { col.num.invoke(it) }
            col.str != null -> compareBy<Agent> { col.str.invoke(it) }
            else -> compareBy<Agent> { it.ap.toDouble() }
        }
        val sorted = agents.sortedWith(byCol.thenBy { it.name })
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
        rebuild() // immediate feedback (even while the sim is paused) — bypasses the update() throttle
    }

    private fun refreshHeaders() {
        COLS.forEachIndexed { idx, c ->
            val arrow = if (idx == sortIndex) (if (sortAsc) " ▲" else " ▼") else ""
            headerCells.getOrNull(idx)?.textContent = c.header + arrow
        }
    }

    // The name cell is clickable (delegated from the persistent tbody — see [ensure]): tag it with the agent name.
    // userSelect=none so a single click registers cleanly instead of starting a text selection on the name.
    private fun nameCell(agent: Agent): HTMLElement = cell(agent.name, "taName").also {
        it.style.color = agent.faction.color
        it.style.cursor = "pointer"
        it.style.asDynamic().userSelect = "none"
        it.title = "Focus the camera on this agent"
        it.setAttribute("data-agent-name", agent.name)
    }

    // Delegated name-click: select the agent (3D highlight + inspector) and make it the camera's centre of
    // attention — the auto-cam orbits it (if on) or the camera tracks it (if off); see MapCamera.focusOn.
    private fun onNameClick(target: Element) {
        val name = target.closest(".taName")?.getAttribute("data-agent-name") ?: return
        Inspector.select("agent:$name")
        MapCamera.focusOn("agent:$name")
    }

    // Agent ground speed in m/s: distance moved last tick (sim px) × metres-per-pixel, and a tick is one sim
    // second ([config.Time.secondsPerTick]), so px/tick already reads as m/s. ~1 m/s is a walking pace; a stuck
    // agent reads ~0. Hover shows the imperial mph.
    private fun speedMs(agent: Agent): Double = agent.stepPx * Sim.MPP_REF

    private fun speedCell(agent: Agent): HTMLElement {
        val ms = speedMs(agent)
        val td = cell(ms.asDynamic().toFixed(1) as String, "taNum")
        td.title = "${(ms * MPH_PER_MS).asDynamic().toFixed(1)} mph"
        return td
    }

    // The Action cell (left-justified string column): the same coin glyph shown beside the tuning sliders,
    // then ~1 char of space, then the action name. Neutral (faction-less) icon to match the slider list.
    // The action icon is static per ActionItem, but toDataURL (canvas → PNG base64) is expensive — and this
    // table rebuilds every frame. Encode each item's icon ONCE and reuse the data URL (was ~80% of runtime CPU).
    private val actionIconUrls = mutableMapOf<ActionItem, String>()

    // Idle FALLBACKS (recruiting / discovery — see ActionItem.isFallback) aren't gameplay actions, so they get the
    // EMPTY coin (the blank WAIT glyph) instead of their own, matching the pill-less head-bob in the 3D scene.
    private fun actionIconUrl(item: ActionItem): String =
        actionIconUrls.getOrPut(item) { ActionIcons.getHiResIcon(if (item.isFallback) ActionItem.WAIT else item).toDataURL() }

    // …and read as "idle/recruiting" / "idle/discovery" so the table makes plain they're filler, not real work.
    private fun actionLabel(item: ActionItem): String = when (item) {
        ActionItem.RECRUIT -> "idle/recruiting"
        ActionItem.EXPLORE -> "idle/discovery"
        else -> item.text
    }

    private fun actionCell(agent: Agent): HTMLElement {
        val td = el("td", "taCell")
        val img = document.createElement("img") as HTMLImageElement
        img.src = actionIconUrl(agent.action.item)
        img.className = "taActionIcon"
        td.appendChild(img)
        val label = el("span", "taActionLabel")
        label.textContent = actionLabel(agent.action.item)
        td.appendChild(label)
        return td
    }

    private fun deployColor(level: Int): String = LevelColor.map[level] ?: "#ffffff"

    // Distinct portals an agent holds keys to (vs total Keys, which counts duplicates).
    private fun uniqueKeys(agent: Agent): Int = agent.inventory.findUniqueKeys().size

    /** A leading right-justified count + a per-level bar strip (height = count, colour = level via [colorFor]). */
    private fun invCell(items: List<DeployableItem>, maxLevel: Int, colorFor: (Int) -> String): HTMLElement {
        val byLevel = items.groupBy { it.getLevel() }.mapValues { it.value.size }
        val maxCount = byLevel.values.maxOrNull() ?: 1
        val td = el("td", "taInv")
        // Number FIRST (fixed width, right-justified against the bars) so the (right-pinned) bar strip stays put
        // as the count changes digits — the count grows leftward instead of pushing the graph around.
        val count = el("span", "taInvCount")
        count.textContent = items.size.toString()
        td.appendChild(count)
        // Always render the full strip (zero-height bars when empty) so the graph keeps a constant width.
        val bars = el("span", "taInvBars")
        for (lvl in 1..maxLevel) {
            val c = byLevel[lvl] ?: 0
            val bar = el("span", "taInvBar")
            bar.style.height = "${(c.toDouble() / maxCount * BAR_MAX_PX).toInt()}px"
            if (c > 0) bar.style.background = colorFor(lvl)
            bars.appendChild(bar)
        }
        td.appendChild(bars)
        return td
    }

    /** How many flip items of [type] (JARVIS virus / ADA refactor) an agent carries — one per split column. */
    private fun virusCount(agent: Agent, type: VirusType): Int = agent.inventory.findViruses().count { it.type == type }

    private fun ensure() {
        if (tbody != null) return
        if (document.body == null) return
        val container = el("div", "topAgents")
        val table = el("table", "taTable")
        val head = el("tr", "taHead")
        COLS.forEachIndexed { idx, c ->
            val th = el("th", "taHeadCell")
            th.textContent = c.header
            c.headerColor?.let { th.style.color = it } // compact coloured-glyph header (the JARVIS/ADA flip dots)
            c.headerTitle?.let { th.title = it }
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
        // Delegated name-click via MOUSEDOWN, not click: the body is rebuilt every frame while the sim runs, so the
        // pressed cell is removed from the DOM before mouseup — and the browser then fires NO click on the tbody at
        // all (the mousedown target is gone), which is why clicking only worked while paused. mousedown fires on
        // press, before any rebuild, so ev.target is the live cell. (elementFromPoint as a belt-and-suspenders.)
        newBody.onmousedown = { ev ->
            (ev.target as? Element ?: document.elementFromPoint(ev.clientX.toDouble(), ev.clientY.toDouble()))
                ?.let { onNameClick(it) }
            null
        }
        table.appendChild(newBody)
        container.appendChild(table)
        Footer.tab("agents").appendChild(container) // AGENTS footer tab (full width)
        tbody = newBody
    }

    // Inventory fill: carried items / the cap (authentic Ingress = a flat 2000, not level-based). Reddens at the
    // cap — that's when hacking stops (Hacker.isActionPossible) until the agent spends/recycles.
    private fun invLimitCell(agent: Agent): HTMLElement {
        val used = agent.inventory.items.size
        return cell("$used/${Config.maxInventory}", "taNum").also {
            if (used >= Config.maxInventory) it.style.color = "#e2674a" // at cap → can't hack for more
        }
    }

    private fun cell(text: String, cls: String): HTMLElement {
        val td = el("td", cls)
        td.textContent = text
        return td
    }
}
