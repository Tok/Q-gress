package util.data.html

import agent.Agent
import org.w3c.dom.HTMLDivElement
import kotlin.browser.document
import kotlin.dom.addClass


class AgentsTableWidget private constructor(val div: HTMLDivElement) {
    fun update(agents: Set<Agent>) {
        agents.forEach {
            val row = document.createElement("div") as HTMLDivElement
            row.addClass("row")

            val nameCol = createColumn(it.name)
            row.append(nameCol)

            val actionCol = createColumn(it.action.toString())
            row.append(actionCol)

            val inventoryCol = createColumn(it.inventory.toString())
            row.append(inventoryCol)

            div.appendChild(row)
        }
    }

    companion object {
        fun create(): AgentsTableWidget {
            val table = document.createElement("div") as HTMLDivElement
            val header = document.createElement("div") as HTMLDivElement
            header.addClass("row")
            header.append(createColumn("Name"))
            header.append(createColumn("Status"))
            header.append(createColumn("Inventory"))
            table.appendChild(header)
            return AgentsTableWidget(table)
        }
        private fun createColumn(text: String): HTMLDivElement {
            val column = document.createElement("div") as HTMLDivElement
            column.addClass("col-md-3", "col-xs-3")
            column.innerText = text
            return column
        }
    }
}
