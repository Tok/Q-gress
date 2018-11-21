package system.display.ui.table

import World
import agent.action.ActionItem
import config.Colors
import config.Dim
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.START
import util.DrawUtil
import util.data.Coords

abstract class UiTable {
    fun strokeTableHeaderText(headerPos: Coords, offset: Int, text: String) {
        val pos = Coords(headerPos.x + offset, headerPos.y)
        DrawUtil.strokeText(World.uiCtx(), pos, text, Colors.white, Dim.topAgentsFontSize, DrawUtil.CODA, 3.0)
    }

    fun strokeTableText(headerPos: Coords, offset: Int, text: String,
                        textAlign: CanvasTextAlign = CanvasTextAlign.START, fillStyle: String = Colors.white) {
        val pos = Coords(headerPos.x + offset, headerPos.y)
        DrawUtil.strokeText(World.uiCtx(), pos, text, fillStyle, Dim.topAgentsFontSize, DrawUtil.CODA, 3.0, Colors.black, textAlign)
    }

    fun addIcon(pos: Coords, item: ActionItem) {
        val image = ActionItem.getIcon(item)
        World.uiCtx().drawImage(image, pos.xx(), pos.yy())
    }
}
