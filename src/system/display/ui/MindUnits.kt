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
    fun draw(enlMu: Int, resMu: Int) {
        //TODO only redraw if updated.
        fun fillMuRect(from: Coords, width: Double, height: Double,
                       fill: String, stroke: String, line: Double) {
            with(World.uiCtx()) {
                if (Styles.isFillMuDisplay) {
                    globalAlpha = 0.3
                    fillStyle = fill
                    fillRect(from.x.toDouble(), from.y.toDouble(), width, height)
                }
                strokeStyle = stroke
                globalAlpha = 1.0
                lineWidth = line
                beginPath()
                strokeRect(from.x.toDouble(), from.y.toDouble(), width, height)
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

        val totalMu = enlMu + resMu
        val enlPart: Int = round((100.0 * enlMu) / totalMu).toInt()
        val resPart: Int = round((100.0 * resMu) / totalMu).toInt()
        val xPos = Dim.muLeftOffset
        val yPos = Dim.height - Dim.muBottomOffset
        val enlPos = Coords(xPos, yPos - Dim.muFontSize * 2)
        val resPos = Coords(xPos, yPos)
        drawMuRect(enlPos, enlPart, Faction.ENL, enlMu)
        drawMuRect(resPos, resPart, Faction.RES, resMu)
    }
}
