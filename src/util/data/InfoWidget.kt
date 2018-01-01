package util.data

import org.w3c.dom.HTMLDivElement
import kotlin.browser.document
import kotlin.dom.addClass

class InfoWidget private constructor(val div: HTMLDivElement, val callback: (String) -> Unit) {
    companion object {
        val COL = "p-2"
        val FLEX = "d-flex"
        val ROW = "flex-row"
        fun create(labelText: String): InfoWidget {
            val label = document.createElement("div") as HTMLDivElement
            label.addClass(COL)
            label.innerText = labelText
            val content = document.createElement("div") as HTMLDivElement
            content.addClass(COL)

            val timeRow = document.createElement("div") as HTMLDivElement
            timeRow.addClass(FLEX, ROW)
            timeRow.append(label)
            timeRow.append(content)

            val callback = fun(text: String) {
                content.innerText = text
            }
            return InfoWidget(timeRow, callback)
        }
    }
}
