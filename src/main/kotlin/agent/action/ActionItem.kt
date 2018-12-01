package agent.action

import Canvas
import Ctx
import agent.Faction
import config.Colors
import config.Dim
import util.DrawUtil
import util.HtmlUtil
import util.data.Circle
import util.data.Coords
import util.data.Line


data class ActionItem(val text: String, val durationSeconds: Int, val qName: String) {
    companion object {
        val MOVE = ActionItem("moving", 60, "Move")
        val WAIT = ActionItem("waiting", 10, "Wait")
        val RECHARGE = ActionItem("recharging", 30, "Recharge")
        val RECRUIT = ActionItem("recruiting", 120, "Recruit")
        val EXPLORE = ActionItem("exploring", 300, "Explore")
        val RECYCLE = ActionItem("recycling", 30, "Recycle")
        val HACK = ActionItem("hacking", 10, "Hack")
        val GLYPH = ActionItem("glyphing", 60, "Glyph")
        val ATTACK = ActionItem("attacking", 15, "Attack")
        val DEPLOY = ActionItem("deploying", 15, "Deploy")
        val CAPTURE = ActionItem("capturing", 15, "Capture")
        val LINK = ActionItem("linking", 30, "Link")
        fun values() = listOf(MOVE, WAIT, RECHARGE, RECRUIT, EXPLORE, RECYCLE, HACK, GLYPH, ATTACK, DEPLOY, CAPTURE, LINK)

        private val enlImages = values().map { it to drawTemplate(it, Faction.ENL) }.toMap()
        private val resImages = values().map { it to drawTemplate(it, Faction.RES) }.toMap()
        private val nonImages = values().map { it to drawTemplate(it, Faction.NONE) }.toMap()
        fun getIcon(item: ActionItem, faction: Faction = Faction.NONE): Canvas {
            return when (faction) {
                Faction.ENL -> enlImages[item] ?: enlImages[WAIT]!!
                Faction.RES -> resImages[item] ?: resImages[WAIT]!!
                else -> nonImages[item] ?: nonImages[WAIT]!!
            }
        }

        private fun drawTemplate(actionItem: ActionItem, faction: Faction): Canvas {
            val strokeStyle = Colors.black
            val lw = Dim.agentLineWidth
            val r = Dim.agentRadius
            val rr = r + lw
            val w = rr * 2
            val h = w
            fun drawAgentLine(ctx: Ctx, line: Line) = DrawUtil.drawLine(ctx, line, strokeStyle, 0.7)
            fun drawAgentCircle(ctx: Ctx, circle: Circle) = DrawUtil.drawCircle(ctx, circle, strokeStyle, 1.0)
            return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
                val pos = Coords(rr, rr)
                val circle = Circle(pos, r.toDouble() + 1)
                DrawUtil.drawCircle(ctx, circle, strokeStyle, lw.toDouble(), faction.color)
                when (actionItem) {
                    MOVE -> drawAgentCircle(ctx, Circle(pos, rr - 2.0))
                    EXPLORE -> {
                        val off = 2
                        drawAgentLine(ctx, Line(Coords(off, off), Coords(w - off, h - off)))
                        drawAgentLine(ctx, Line(Coords(off, h - off), Coords(w - off, off)))
                        drawAgentLine(ctx, Line(Coords(rr, 0), Coords(rr, h)))
                        drawAgentLine(ctx, Line(Coords(0, rr), Coords(w, rr)))
                        drawAgentCircle(ctx, Circle(pos, rr - 2.0))
                    }
                    RECRUIT -> {
                        drawAgentLine(ctx, Line(Coords(rr, 0), Coords(rr, h)))
                        drawAgentLine(ctx, Line(Coords(0, rr), Coords(w, rr)))
                    }
                    ATTACK -> drawAgentLine(ctx, Line(Coords(rr, 0), Coords(rr, h)))
                    LINK -> drawAgentLine(ctx, Line(Coords(0, rr), Coords(w, rr)))
                    DEPLOY -> {
                        drawAgentLine(ctx, Line(Coords(0, rr - 1), Coords(w, rr - 1)))
                        drawAgentLine(ctx, Line(Coords(0, rr + 1), Coords(w, rr + 1)))
                    }
                    CAPTURE -> {
                        drawAgentLine(ctx, Line(Coords(rr, 0), Coords(rr, h)))
                        drawAgentLine(ctx, Line(Coords(0, rr - 1), Coords(w, rr - 1)))
                        drawAgentLine(ctx, Line(Coords(0, rr + 1), Coords(w, rr + 1)))
                    }
                    HACK -> drawAgentCircle(ctx, Circle(pos, rr - 4.0))
                    GLYPH -> drawAgentCircle(ctx, Circle(pos, rr - 3.0))
                    RECHARGE -> {
                        val off = 2
                        drawAgentLine(ctx, Line(Coords(off, h - off), Coords(w - off, off)))
                        drawAgentCircle(ctx, Circle(pos, rr - 2.0))
                    }
                    RECYCLE -> {
                        val off = 2
                        drawAgentLine(ctx, Line(Coords(off, off), Coords(w - off, h - off)))
                        drawAgentCircle(ctx, Circle(pos, rr - 2.0))
                    }
                    WAIT -> Unit
                }
            })
        }
    }
}
