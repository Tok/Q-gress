package system.display

import World
import agent.Faction
import config.Colors
import config.Dim
import org.w3c.dom.END
import util.DrawUtil
import util.data.Coords

object StatsDisplay {
    val fontSize = Dim.statsFontSize
    val lineWidth = 3.0
    fun draw() {
        fun drawCell(pos: Coords, text: String, color: String) {
            DrawUtil.strokeText(World.uiCtx(), pos, text, color, fontSize, DrawUtil.CODA, lineWidth, Colors.black, org.w3c.dom.CanvasTextAlign.END)
        }

        val yOff = Dim.statsTopOffset
        val yStep = fontSize * 3 / 2
        val xStep = 55
        fun drawRow(pos: Int, header: String, enl: Int, res: Int, total: Int) {
            drawCell(Coords(pos, yOff), header, Colors.white)
            drawCell(Coords(pos, yOff + yStep), enl.toString(), Faction.ENL.color)
            drawCell(Coords(pos, yOff + yStep * 2), res.toString(), Faction.RES.color)
            drawCell(Coords(pos, yOff + yStep * 3), total.toString(), Colors.white)
        }

        val xPos = Dim.width - Dim.statsRightOffset
        return with(World) {
            (1..4).forEach { step ->
                when (step) {
                    1 -> drawRow(xPos, "Agents", countAgents(Faction.ENL), countAgents(Faction.RES), countAgents())
                    2 -> drawRow(xPos + xStep, "Portals", countPortals(Faction.ENL), countPortals(Faction.RES), countPortals())
                    3 -> drawRow(xPos + (xStep * 2), "Links", countLinks(Faction.ENL), countLinks(Faction.RES), countLinks())
                    4 -> drawRow(xPos + (xStep * 3), "Fields", countFields(Faction.ENL), countFields(Faction.RES), countFields())
                }
            }
        }
    }
}
