package util.ui

import World
import agent.Agent
import kotlinx.browser.document
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import portal.Portal
import system.display.Scene3D

/**
 * Screen-fixed DOM panel showing the currently selected entity (portal or agent).
 * Selection ids are the ones produced by [Scene3D.pick] ("portal:<id>" / "agent:<name>").
 * [refresh] re-reads live entity state each tick; if the entity is gone, it deselects.
 */
object Inspector {
    private const val PANEL_ID = "inspector"
    private const val CONTENT_ID = "inspectorContent"
    private const val NEUTRAL = "#bbbbbb"

    private var selectedId: String? = null

    fun select(id: String?) {
        selectedId = id
        Scene3D.selected = id
        refresh()
    }

    fun refresh() {
        val id = selectedId
        if (id == null) {
            panel().addClass("invisible")
            return
        }
        val html = when {
            id.startsWith("portal:") -> findPortal(id.removePrefix("portal:"))?.let(::portalHtml)
            id.startsWith("agent:") -> findAgent(id.removePrefix("agent:"))?.let(::agentHtml)
            else -> null
        }
        if (html == null) {
            select(null) // selected entity no longer exists
            return
        }
        content().innerHTML = html
        panel().removeClass("invisible")
    }

    private fun findPortal(id: String): Portal? = World.allPortals.find { it.id == id }
    private fun findAgent(name: String): Agent? = World.allAgents.find { it.name == name }

    private fun row(label: String, value: String) = "<div class=\"inspectorRow\">$label: <b>$value</b></div>"

    private fun portalHtml(portal: Portal): String {
        val faction = portal.owner?.faction
        val color = faction?.color ?: NEUTRAL
        return "<div class=\"inspectorTitle\">Portal</div>" +
            "<div class=\"inspectorName\" style=\"color:$color\">${portal.name}</div>" +
            row("Owner", faction?.abbr ?: "Neutral") +
            row("Level", portal.getLevel().value.toString()) +
            row("Health", "${portal.calcHealth()}%") +
            row("Resonators", "${portal.filledSlots().count()}/8") +
            row("Links", portal.links.count().toString()) +
            row("Fields", portal.fields.count().toString())
    }

    private fun agentHtml(agent: Agent): String = "<div class=\"inspectorTitle\">Agent</div>" +
        "<div class=\"inspectorName\" style=\"color:${agent.faction.color}\">${agent.name}</div>" +
        row("Faction", agent.faction.abbr) +
        row("Level", agent.getLevel().toString()) +
        row("AP", agent.ap.toString()) +
        row("XM", agent.xm.toString()) +
        row("Action", agent.action.item.text) +
        row("Target", agent.actionPortal.name)

    private fun content(): HTMLElement {
        panel()
        return document.getElementById(CONTENT_ID) as HTMLElement
    }

    private fun panel(): HTMLElement {
        (document.getElementById(PANEL_ID) as? HTMLElement)?.let { return it }
        val div = document.createElement("div") as HTMLDivElement
        div.id = PANEL_ID
        div.addClass("inspector", "invisible")
        val close = document.createElement("button") as HTMLButtonElement
        close.addClass("inspectorClose")
        close.innerHTML = "&times;"
        close.onclick = { select(null) }
        val contentDiv = document.createElement("div") as HTMLDivElement
        contentDiv.id = CONTENT_ID
        div.append(close)
        div.append(contentDiv)
        Hud.right().append(div) // intel column, below the history dashboard (contextual)
        return div
    }
}
