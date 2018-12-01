package system.display.ui

import World
import agent.Faction
import config.Colors
import config.Dim
import org.w3c.dom.END
import system.display.Display
import util.DrawUtil
import util.data.Coords

object StatsDisplay : Display {
    private const val fontSize = 13
    private const val lineWidth = 3.0
    override fun draw() {
        fun drawCell(pos: Coords, text: String, color: String) {
            DrawUtil.strokeText(World.uiCtx(), pos, text, color, fontSize, DrawUtil.CODA, lineWidth, Colors.black, org.w3c.dom.CanvasTextAlign.END)
        }

        val yOff = Dim.statsTopOffset
        val yStep = fontSize * 3 / 2
        val xStep = 55
        fun drawRow(pos: Int, header: String, factions: Pair<Faction, Faction>,
                    first: Int, second: Int, total: Int) {
            drawCell(Coords(pos, yOff), header, Colors.white)
            drawCell(Coords(pos, yOff + yStep), first.toString(), factions.first.color)
            drawCell(Coords(pos, yOff + yStep * 2), second.toString(), factions.second.color)
            drawCell(Coords(pos, yOff + yStep * 3), total.toString(), Colors.white)
        }

        val xPos = Dim.width - Dim.statsRightOffset
        val factions: Pair<Faction, Faction> = World.userFaction!! to World.userFaction!!.enemy()
        return with(World) {
            (1..4).forEach { step ->
                when (step) {
                    1 -> drawRow(xPos, "Agents", factions, countAgents(factions.first), countAgents(factions.second), countAgents())
                    2 -> drawRow(xPos + xStep, "Portals", factions, countPortals(factions.first), countPortals(factions.second), countPortals())
                    3 -> drawRow(xPos + (xStep * 2), "Links", factions, countLinks(factions.first), countLinks(factions.second), countLinks())
                    4 -> drawRow(xPos + (xStep * 3), "Fields", factions, countFields(factions.first), countFields(factions.second), countFields())
                }
            }
        }
    }
}
