package util.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * The unified HUD: one right-docked panel with three tabs instead of panes scattered to the corners.
 * - **NOW** — the scoreboard (MU bars + time/tick/stray XM), the leaderboard, and the action LOG,
 * - **HISTORY** — the per-metric uPlot sparklines,
 * - **TUNE** — the behaviour sliders.
 *
 * Panels append themselves into [now] / [history] / [tune] (lazily). Only the active tab's body is
 * shown; ordering inside a tab is set in CSS (`order`) so it doesn't depend on which panel builds
 * first. The dock is the only pointer-interactive part of the HUD (the rest stays click-through so
 * the map keeps its drag/zoom gestures).
 */
object Dock {
    private const val NOW = "dockNow"
    private const val HISTORY = "dockHistory"
    private const val TUNE = "dockTune"
    private val tabs = listOf("NOW" to NOW, "HISTORY" to HISTORY, "TUNE" to TUNE)

    private var built = false
    private var active = NOW
    private val tabButtons = mutableMapOf<String, HTMLElement>()

    fun now(): HTMLElement = body(NOW)
    fun history(): HTMLElement = body(HISTORY)
    fun tune(): HTMLElement = body(TUNE)

    private fun body(id: String): HTMLElement {
        build()
        return document.getElementById(id) as HTMLElement
    }

    private fun build() {
        if (built) return
        if (document.body == null) return
        built = true
        val dock = el("div", "dock")
        dock.id = "dock"
        val header = el("div", "dockHeader")
        val title = el("div", "dockTitle")
        title.textContent = "Q-GRESS"
        val tabBar = el("div", "dockTabs")
        header.appendChild(title)
        header.appendChild(tabBar)
        dock.appendChild(header)
        tabs.forEach { (label, bodyId) ->
            val tab = el("button", "dockTab")
            tab.textContent = label
            tab.onclick = {
                activate(bodyId)
                null
            }
            tabBar.appendChild(tab)
            tabButtons[bodyId] = tab
            val pane = el("div", "dockBody")
            pane.id = bodyId
            dock.appendChild(pane)
        }
        document.body?.appendChild(dock)
        activate(active)
    }

    private fun activate(bodyId: String) {
        active = bodyId
        tabs.forEach { (_, id) ->
            (document.getElementById(id) as? HTMLElement)?.style?.display = if (id == bodyId) "flex" else "none"
            tabButtons[id]?.let { if (id == bodyId) it.classList.add("active") else it.classList.remove("active") }
        }
    }

    private fun el(tag: String, cls: String): HTMLElement {
        val e = document.createElement(tag) as HTMLElement
        if (cls.isNotEmpty()) e.className = cls
        return e
    }
}
