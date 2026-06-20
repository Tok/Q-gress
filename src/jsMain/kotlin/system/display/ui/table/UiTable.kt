package system.display.ui.table

import World
import agent.action.ActionItem
import config.Colors
import config.Dim
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.START
import util.DrawUtil
import util.data.Pos

abstract class UiTable {
    fun strokeTableHeaderText(headerPos: Pos, offset: Int, text: String) {
        val pos = Pos(headerPos.x + offset, headerPos.y)
        DrawUtil.strokeText(World.uiCtx(), pos, text, Colors.white, Dim.topAgentsFontSize, DrawUtil.CODA, 3.0)
    }

    fun strokeTableText(headerPos: Pos, offset: Int, text: String,
                        textAlign: CanvasTextAlign = CanvasTextAlign.START, fill: String = Colors.white) {
        val pos = Pos(headerPos.x + offset, headerPos.y)
        DrawUtil.strokeText(World.uiCtx(), pos, text, fill, Dim.topAgentsFontSize, DrawUtil.CODA, 3.0, Colors.black, textAlign)
    }

    fun addIcon(pos: Pos, item: ActionItem) {
        val image = ActionItem.getIcon(item)
        World.uiCtx().drawImage(image, pos.x, pos.y)
    }
}
