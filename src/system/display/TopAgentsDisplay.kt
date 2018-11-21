package system.display

import Ctx
import World
import config.Colors
import config.Config
import config.Dim
import items.deployable.DeployableItem
import items.level.LevelColor
import items.types.ShieldType
import org.w3c.dom.RIGHT
import org.w3c.dom.START
import util.DrawUtil
import util.data.Coords

object TopAgentsDisplay : TableDisplay() {
    fun draw() {
        val fontSize = Dim.topAgentsInventoryFontSize
        val lineWidth = 2.0
        fun drawBars(ctx: Ctx, barWidth: Int, level: Int, color: String, pos: Coords, count: Int, maxCount: Int) {
            val xOffset = (barWidth * level) - barWidth
            val statPos = Coords(pos.x + xOffset, pos.y + (fontSize / 2))
            val h = fontSize.toDouble() * count / maxCount
            DrawUtil.drawRect(ctx, statPos, h, barWidth.toDouble(), color, Colors.black, lineWidth)
        }

        fun drawCounts(ctx: Ctx, items: List<DeployableItem>?, col: Coords, offset: Int, isShields: Boolean = false) {
            val pos = Coords(col.x + offset, col.y)
            val barWidth = 6
            val totalWidth = 48
            val statPos = Coords(pos.x, pos.y + (fontSize / 2))
            DrawUtil.drawRect(ctx, statPos, 0.0, totalWidth.toDouble(), Colors.black, Colors.black, lineWidth)
            if (items == null || items.isEmpty()) {
                DrawUtil.strokeText(ctx, pos, "0", Colors.white, fontSize, DrawUtil.CODA, lineWidth, Colors.black, org.w3c.dom.CanvasTextAlign.RIGHT)
            } else {
                DrawUtil.strokeText(ctx, pos, items.size.toString(), Colors.white, fontSize, DrawUtil.CODA, lineWidth, Colors.black, org.w3c.dom.CanvasTextAlign.RIGHT)
                val itemsByLevel: Map<Int, List<DeployableItem>> = items.groupBy { it.getLevel() }
                val countsByLevel: Map<Int, Int> = itemsByLevel.mapValues { it.value.count() }
                val maxCount = countsByLevel.map { it.value }.max() ?: 0
                if (isShields) {
                    (1..4).forEach { level ->
                        val count = countsByLevel[level] ?: 0
                        if (count > 0) {
                            val color = ShieldType.getColorForLevel(level)
                            drawBars(ctx, barWidth * 2, level, color, pos, count, maxCount)
                        }
                    }
                } else {
                    (1..8).forEach { level ->
                        val count = countsByLevel[level] ?: 0
                        if (count > 0) {
                            val color = LevelColor.map[level] ?: "#FFFFFF"
                            drawBars(ctx, barWidth, level, color, pos, count, maxCount)
                        }
                    }
                }
            }
        }

        val ctx = World.uiCtx()
        ctx.globalAlpha = 1.0
        val xPos = Dim.topAgentsLeftOffset
        val yOffset = Dim.topAgentsFontSize * 3 / 2
        val yFixOffset = Dim.height - Dim.topAgentsBottomOffset - (Config.topAgentsMessageLimit * yOffset)
        val headerPos = Coords(xPos, yFixOffset - yOffset)
        val top = World.allAgents.toList().sortedBy { -it.ap }.take(Config.topAgentsMessageLimit)
        top.forEachIndexed { index, agent ->
            val rank = (index + 1).toString()
            val name = agent.toString()
            val pos = Coords(xPos, yFixOffset + (yOffset * index))
            var offset = 0

            strokeTableText(pos, offset, rank, org.w3c.dom.CanvasTextAlign.RIGHT)
            offset += 10

            strokeTableHeaderText(headerPos, offset, "XM")
            strokeTableText(pos, offset + 28, agent.xm.toString(), org.w3c.dom.CanvasTextAlign.RIGHT)
            offset += 34

            strokeTableHeaderText(headerPos, offset, "AP")
            strokeTableText(pos, offset + 44, agent.ap.toString(), org.w3c.dom.CanvasTextAlign.RIGHT)
            offset += 50

            strokeTableHeaderText(headerPos, offset, "Agent")
            strokeTableText(pos, offset, name, org.w3c.dom.CanvasTextAlign.START, agent.faction.color)
            offset += 100

            strokeTableHeaderText(headerPos, offset, "XMPs")
            drawCounts(ctx, agent.inventory.findXmps(), pos, offset)
            offset += 70

            strokeTableHeaderText(headerPos, offset, "Resos")
            drawCounts(ctx, agent.inventory.findResonators(), pos, offset)
            offset += 70

            strokeTableHeaderText(headerPos, offset, "Cubes")
            drawCounts(ctx, agent.inventory.findPowerCubes(), pos, offset)
            offset += 70

            strokeTableHeaderText(headerPos, offset, "Shields")
            drawCounts(ctx, agent.inventory.findShields(), pos, offset, true)
            offset += 60

            val keyCount = agent.inventory.keyCount()
            strokeTableHeaderText(headerPos, offset, "Keys")
            strokeTableText(pos, offset + 24, keyCount.toString(), org.w3c.dom.CanvasTextAlign.RIGHT)
            offset += 30

            strokeTableHeaderText(headerPos, offset, "Action")
            val iconRadius = Dim.agentRadius.toInt()
            val actionIconPos = Coords(pos.x + offset - iconRadius, pos.y - iconRadius)
            addIcon(actionIconPos, agent.action.item)
            strokeTableText(pos, offset + (iconRadius * 2) + 5, agent.action.toString())
            offset += 64

            strokeTableHeaderText(headerPos, offset, "Portal")
            strokeTableText(pos, offset, agent.actionPortal.name)
        }
    }
}
