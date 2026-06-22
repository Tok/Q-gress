package util.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * Full-width bottom footer: a header with tab buttons (EVENT LOG / AGENTS) + a collapse chevron, and
 * a body that shows the active tab. Collapsing hides the body, leaving just the header bar. Panels
 * append into [tab] ("log" / "agents"); tab visibility + collapse are class/style driven so it
 * survives the panels building in any order.
 */
object Footer {
    private val tabs = listOf("AGENTS" to AGENTS_ID, "EVENT LOG" to LOG_ID)
    private var built = false
    private var active = AGENTS_ID
    private var collapsed = false
    private val tabButtons = mutableMapOf<String, HTMLElement>()
    private var body: HTMLElement? = null
    private var chevron: HTMLElement? = null

    /** The content element for a footer tab ("log" or "agents"); builds the footer on first call. */
    fun tab(id: String): HTMLElement {
        build()
        return document.getElementById(if (id == "agents") AGENTS_ID else LOG_ID) as HTMLElement
    }

    private fun build() {
        if (built) return
        if (document.body == null) return
        built = true
        val footer = el("div", "footer")
        footer.id = "footer"
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
        val chev = el("button", "footerChevron")
        chev.onclick = {
            collapsed = !collapsed
            applyCollapsed()
            null
        }
        chevron = chev
        header.appendChild(tabBar)
        header.appendChild(chev)
        footer.appendChild(header)
        val b = el("div", "footerBody")
        body = b
        tabs.forEach { (_, bodyId) ->
            val pane = el("div", "footerPane")
            pane.id = bodyId
            b.appendChild(pane)
        }
        footer.appendChild(b)
        document.body?.appendChild(footer)
        activate(active)
        applyCollapsed()
    }

    /** Cycle to the next footer tab (Tab key); expands the footer first if it was collapsed. */
    fun cycleTab() {
        build()
        if (collapsed) {
            collapsed = false
            applyCollapsed()
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
    }

    private fun applyCollapsed() {
        body?.style?.display = if (collapsed) "none" else "block"
        chevron?.textContent = if (collapsed) "▴" else "▾"
    }

    private fun el(tag: String, cls: String): HTMLElement {
        val e = document.createElement(tag) as HTMLElement
        if (cls.isNotEmpty()) e.className = cls
        return e
    }

    private const val LOG_ID = "footerLog"
    private const val AGENTS_ID = "footerAgents"
}
