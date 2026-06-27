package util.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * Full-width bottom footer: a header with tab buttons + controls (a **maximize** toggle and a **collapse**
 * chevron), and a body that shows the active tab. The body has three heights: **collapsed** (hidden, just the
 * header bar), **normal** (a short docked strip), and **expanded** (near-full-screen, for space-hungry tabs
 * like NET — the sim keeps running behind it). Panels append into [tab]; tab/collapse/expand are class/style
 * driven so it survives the panels building in any order.
 */
object Footer {
    private val tabs = listOf(
        "AGENTS" to AGENTS_ID,
        "PORTALS" to PORTALS_ID,
        "BRAINS" to BRAINS_ID,
        "STATS" to AI_ID,
        "TRAIN" to TRAIN_ID,
        "AUDIO" to AUDIO_ID,
        "EVENT LOG" to LOG_ID,
    )
    private var built = false
    private var active = AGENTS_ID
    private var collapsed = false
    private var expanded = false
    private var autoExpanded = false // expansion driven by entering a space-hungry tab (NET), not a manual click
    private val tabButtons = mutableMapOf<String, HTMLElement>()
    private var rootEl: HTMLElement? = null
    private var body: HTMLElement? = null
    private var chevron: HTMLElement? = null
    private var maximize: HTMLElement? = null

    /** Whether the footer is currently maximized + visible — panels can render fuller detail when it is. */
    fun isExpanded() = expanded && !collapsed

    /** The content element for a footer tab ("log" / "agents" / "ai"); builds the footer on first call. */
    fun tab(id: String): HTMLElement {
        build()
        val bodyId = when (id) {
            "agents" -> AGENTS_ID
            "portals" -> PORTALS_ID
            "brains" -> BRAINS_ID
            "ai" -> AI_ID
            "train" -> TRAIN_ID
            "audio" -> AUDIO_ID
            else -> LOG_ID
        }
        return document.getElementById(bodyId) as HTMLElement
    }

    private fun build() {
        if (built) return
        if (document.body == null) return
        built = true
        val footerEl = el("div", "footer")
        footerEl.id = "footer"
        rootEl = footerEl
        val header = el("div", "footerHeader")
        val tabBar = el("div", "footerTabs")
        tabs.forEach { (label, bodyId) ->
            val t = el("button", "footerTab")
            t.textContent = label
            t.onclick = {
                activate(bodyId)
                null
            }
            tabBar.appendChild(t)
            tabButtons[bodyId] = t
        }
        header.appendChild(tabBar)
        header.appendChild(controls())
        footerEl.appendChild(header)
        val b = el("div", "footerBody")
        body = b
        tabs.forEach { (_, bodyId) ->
            val pane = el("div", "footerPane")
            pane.id = bodyId
            b.appendChild(pane)
        }
        footerEl.appendChild(b)
        document.body?.appendChild(footerEl)
        activate(active)
        applyState()
    }

    // The size controls: maximize (normal ↔ near-full-screen) then collapse (hide ↔ show).
    private fun controls(): HTMLElement {
        val box = el("div", "footerControls")
        val max = el("button", "footerMaximize")
        max.title = "Expand / restore the panel"
        max.onclick = {
            expanded = !expanded
            autoExpanded = false // the user took manual control of the size
            if (expanded) collapsed = false // expanding always reveals the body
            applyState()
            null
        }
        maximize = max
        val chev = el("button", "footerChevron")
        chev.title = "Collapse / show the panel"
        chev.onclick = {
            collapsed = !collapsed
            applyState()
            null
        }
        chevron = chev
        box.appendChild(max)
        box.appendChild(chev)
        return box
    }

    /** Cycle to the next footer tab (Tab key); reveals the footer first if it was collapsed. */
    fun cycleTab() {
        build()
        if (collapsed) {
            collapsed = false
            applyState()
        }
        val ids = tabs.map { it.second }
        activate(ids[(ids.indexOf(active) + 1) % ids.size])
    }

    private fun activate(bodyId: String) {
        active = bodyId
        tabs.forEach { (_, id) ->
            (document.getElementById(id) as? HTMLElement)?.style?.display = if (id == bodyId) "block" else "none"
            tabButtons[id]?.let { if (id == bodyId) it.classList.add("active") else it.classList.remove("active") }
        }
        // The NET viz + TRAIN tab want the whole screen — auto-expand on entry, auto-restore on leave (unless
        // the player has since taken manual control of the size, in which case we leave their choice alone).
        if (bodyId == TRAIN_ID || bodyId == BRAINS_ID) {
            expanded = true
            collapsed = false
            autoExpanded = true
        } else if (autoExpanded) {
            expanded = false
            autoExpanded = false
        }
        applyState()
    }

    private fun applyState() {
        body?.style?.display = if (collapsed) "none" else "block"
        chevron?.textContent = if (collapsed) "▴" else "▾"
        // .expanded grows the body to near-full-screen (CSS); only meaningful while the body is shown.
        rootEl?.let { if (expanded && !collapsed) it.classList.add("expanded") else it.classList.remove("expanded") }
        maximize?.innerHTML = if (expanded && !collapsed) ICON_RESTORE else ICON_MAXIMIZE
    }

    // Simple monochrome maximize/restore icons (feather-style, currentColor) — not OS emoji.
    private fun svgIcon(inner: String): String = "<svg viewBox='0 0 24 24' width='13' height='13' fill='none' stroke='currentColor' " +
        "stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>$inner</svg>"
    private val ICON_MAXIMIZE = svgIcon(
        "<polyline points='15 3 21 3 21 9'/><polyline points='9 21 3 21 3 15'/>" +
            "<line x1='21' y1='3' x2='14' y2='10'/><line x1='3' y1='21' x2='10' y2='14'/>",
    )
    private val ICON_RESTORE = svgIcon(
        "<polyline points='4 14 10 14 10 20'/><polyline points='20 10 14 10 14 4'/>" +
            "<line x1='14' y1='10' x2='21' y2='3'/><line x1='3' y1='21' x2='10' y2='14'/>",
    )

    private const val LOG_ID = "footerLog"
    private const val AGENTS_ID = "footerAgents"
    private const val PORTALS_ID = "footerPortals"
    private const val BRAINS_ID = "footerBrains"
    private const val AI_ID = "footerAi"
    private const val TRAIN_ID = "footerTrain"
    private const val AUDIO_ID = "footerAudio"
}
