package util.ui

import World
import config.Config
import items.deployable.DeployableItem
import items.level.LevelColor
import items.types.ShieldType
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * Top-agents leaderboard as a DOM table (UI Stage 3, last canvas HUD widget → DOM), replacing the
 * canvas `TopAgentsDisplay`. Columns: #, XM, AP, Agent (faction colour), XMPs/Resos/Cubes/Shields
 * (count + a per-level bar strip in the same rarity/level colours the old HUD used), Keys, Action,
 * Portal. Fixed column widths (table-layout: fixed). The body is rebuilt each frame from the
 * AP-sorted top agents.
 */
object TopAgentsPanel {
    private const val ROWS = Config.topAgentsMessageLimit
    private val HEADERS = listOf("#", "XM", "AP", "Agent", "XMPs", "Resos", "Cubes", "Shields", "Keys", "Action", "Portal")
    private const val MAX_DEPLOY_LEVEL = 8
    private const val MAX_SHIELD_LEVEL = 4
    private const val BAR_MAX_PX = 11

    private var tbody: HTMLElement? = null

    /** Rebuild the table body from the current top agents. */
    fun update() {
        ensure()
        val body = tbody ?: return
        body.textContent = ""
        World.allAgents.toList().sortedBy { -it.ap }.take(ROWS).forEachIndexed { i, agent ->
            val row = el("tr", "taRow")
            row.appendChild(cell((i + 1).toString(), "taNum"))
            row.appendChild(cell(agent.xm.toString(), "taNum"))
            row.appendChild(cell(agent.ap.toString(), "taNum"))
            row.appendChild(cell(agent.name, "taName").also { it.style.color = agent.faction.color })
            row.appendChild(invCell(agent.inventory.findXmps(), MAX_DEPLOY_LEVEL, ::deployColor))
            row.appendChild(invCell(agent.inventory.findResonators(), MAX_DEPLOY_LEVEL, ::deployColor))
            row.appendChild(invCell(agent.inventory.findPowerCubes(), MAX_DEPLOY_LEVEL, ::deployColor))
            row.appendChild(invCell(agent.inventory.findShields(), MAX_SHIELD_LEVEL, ShieldType::getColorForLevel))
            row.appendChild(cell(agent.inventory.keyCount().toString(), "taNum"))
            row.appendChild(cell(agent.action.item.text, "taCell"))
            row.appendChild(cell(agent.actionPortal.name, "taCell"))
            body.appendChild(row)
        }
    }

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
        HEADERS.forEach { h ->
            val th = el("th", "taHeadCell")
            th.textContent = h
            head.appendChild(th)
        }
        val thead = el("thead", "")
        thead.appendChild(head)
        table.appendChild(thead)
        val newBody = el("tbody", "")
        table.appendChild(newBody)
        container.appendChild(table)
        Dock.now().appendChild(container) // NOW tab (between the scoreboard and the LOG)
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
