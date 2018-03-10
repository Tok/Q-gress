package agent.action

import Canvas
import Ctx
import agent.Faction
import config.Colors
import config.Dimensions
import util.DrawUtil
import util.HtmlUtil
import util.data.Circle
import util.data.Coords
import util.data.Line


data class ActionItem(val text: String, val durationSeconds: Int, val qName: String) {
    companion object {
        val MOVE = ActionItem("moving", 1, "Move")
        val WAIT = ActionItem("waiting", 1, "Wait")
        val RECHARGE = ActionItem("recharge", 1, "Recharge")
        val RECYCLE = ActionItem("recycle", 1, "Recycle")
        val HACK = ActionItem("hacking", 5, "Hack")
        val GLYPH = ActionItem("glyphing", 40, "Glyph")
        val ATTACK = ActionItem("attacking", 5, "Attack")
        val DEPLOY = ActionItem("deploying", 10, "Deploy")
        val LINK = ActionItem("linking", 10, "Link")
        fun values() = listOf(MOVE, WAIT, RECHARGE, RECYCLE, HACK, GLYPH, ATTACK, DEPLOY, LINK)

        private val enlImages = ActionItem.values().map { it to drawTemplate(it, Faction.ENL) }.toMap()
        private val resImages = ActionItem.values().map { it to drawTemplate(it, Faction.RES) }.toMap()
        private val nonImages = ActionItem.values().map { it to drawTemplate(it, Faction.NONE) }.toMap()
        fun getIcon(item: ActionItem, faction: Faction = Faction.NONE): Canvas {
            when (faction) {
                Faction.ENL -> return enlImages.get(item) ?: enlImages.get(WAIT)!!
                Faction.RES -> return resImages.get(item) ?: resImages.get(WAIT)!!
                else -> return nonImages.get(item) ?: nonImages.get(WAIT)!!
            }
        }

        private fun drawTemplate(actionItem: ActionItem, faction: Faction): Canvas {
            val strokeStyle = Colors.black
            val lw = Dimensions.agentLineWidth
            val r = Dimensions.agentRadius.toInt()
            val w = (r * 2) + (2 * lw)
            val h = w
            fun drawAgentLine(ctx: Ctx, line: Line) = DrawUtil.drawLine(ctx, line, strokeStyle, 0.7)
            fun drawAgentCircle(ctx: Ctx, circle: Circle) = DrawUtil.drawCircle(ctx, circle, strokeStyle, 1.0)
            return HtmlUtil.prerender(w, h, fun(ctx: Ctx) {
                val pos = Coords(r + lw, r + lw)
                val circle = Circle(pos, r.toDouble())
                DrawUtil.drawCircle(ctx, circle, strokeStyle, lw * 2.0, faction.color)
                when (actionItem) {
                    HACK, RECYCLE, RECHARGE -> drawAgentCircle(ctx, Circle(pos, (r - 2).toDouble()))
                    DEPLOY, LINK -> {
                        drawAgentLine(ctx, Line(Coords(r + 1, 0), Coords(r + 1, h)))
                        drawAgentLine(ctx, Line(Coords(0, r + 1), Coords(w, r + 1)))
                    }
                    GLYPH -> drawAgentLine(ctx, Line(Coords(0, r), Coords(w, r)))
                    ATTACK -> drawAgentLine(ctx, Line(Coords(r + 1, 0), Coords(r + 1, h)))
                    MOVE -> drawAgentCircle(ctx, Circle(pos, (r - 1).toDouble()))
                    WAIT -> Unit
                }
            })
        }
    }
}
