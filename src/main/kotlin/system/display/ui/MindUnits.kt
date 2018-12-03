package system.display.ui

import World
import agent.Faction
import config.Constants
import config.Dim
import config.Styles
import util.DrawUtil
import util.data.Coords
import kotlin.math.round

object MindUnits {
    fun draw(firstMu: Int, secondMu: Int, factions: Pair<Faction, Faction>) {
        //TODO only redraw if updated.
        fun fillMuRect(from: Coords, width: Double, height: Double,
                       fill: String, stroke: String, line: Double) {
            with(World.uiCtx()) {
                if (Styles.isFillMuDisplay) {
                    globalAlpha = 0.3
                    fillStyle = fill
                    fillRect(from.x, from.y, width, height)
                }
                strokeStyle = stroke
                globalAlpha = 1.0
                lineWidth = line
                beginPath()
                strokeRect(from.x, from.y, width, height)
                closePath()
                stroke()
            }
        }

        fun drawMuRect(pos: Coords, part: Int, faction: Faction, mu: Int) {
            val fromRect = Coords(pos.x, pos.y - Dim.muFontSize)
            val width = 1.5 * part
            val height = Dim.muFontSize.toDouble() * Constants.phi
            fillMuRect(fromRect, width, height, faction.color, faction.color, 3.0)
            val text = faction.abbr + " " + mu + "M"
            val textPos = Coords(pos.x + 21, pos.y - 3)
            DrawUtil.strokeText(World.uiCtx(), textPos, text, faction.color, Dim.muFontSize, DrawUtil.AMARILLO)
        }

        val totalMu = firstMu + secondMu
        val firstPart: Int = round((100.0 * firstMu) / totalMu).toInt()
        val secondPart: Int = round((100.0 * secondMu) / totalMu).toInt()
        val xPos = Dim.muLeftOffset
        val yPos = Dim.height - Dim.muBottomOffset
        val firstPos = Coords(xPos, yPos - Dim.muFontSize * 2)
        val secondPos = Coords(xPos, yPos)
        drawMuRect(firstPos, firstPart, factions.first, firstMu)
        drawMuRect(secondPos, secondPart, factions.second, secondMu)
    }
}
