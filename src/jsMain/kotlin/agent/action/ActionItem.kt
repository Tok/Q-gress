package agent.action

import agent.Faction
import config.Colors
import config.Dim
import extension.Canvas
import extension.Ctx
import util.DrawUtil
import util.HtmlUtil
import util.data.Circle
import util.data.Line
import util.data.Pos


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
        fun values() =
            listOf(MOVE, WAIT, RECHARGE, RECRUIT, EXPLORE, RECYCLE, HACK, GLYPH, ATTACK, DEPLOY, CAPTURE, LINK)

        private val enlImages = if (HtmlUtil.isRunningInBrowser())
            values().map { it to drawTemplate(it, Faction.ENL) }.toMap() else emptyMap()
        private val resImages = if (HtmlUtil.isRunningInBrowser())
            values().map { it to drawTemplate(it, Faction.RES) }.toMap() else emptyMap()
        private val nonImages = if (HtmlUtil.isRunningInBrowser())
            values().map { it to drawTemplate(it) }.toMap() else emptyMap()

        fun getIcon(item: ActionItem, faction: Faction? = null): Canvas {
            return when (faction) {
                Faction.ENL -> enlImages[item] ?: enlImages[WAIT]!!
                Faction.RES -> resImages[item] ?: resImages[WAIT]!!
                else -> nonImages[item] ?: nonImages[WAIT]!!
            }
        }

        private fun drawTemplate(item: ActionItem, faction: Faction? = null): Canvas? {
            if (HtmlUtil.isNotRunningInBrowser()) return null
            val stroke = Colors.black
            val lw = Dim.agentLineWidth
            val r = Dim.agentRadius
            val rr = r + lw
            val w = rr * 2
            val h = w
            fun drawAgentLine(ctx: Ctx, line: Line) = DrawUtil.drawLine(ctx, line, stroke, 0.7)
            fun drawAgentCircle(ctx: Ctx, circle: Circle) = DrawUtil.drawCircle(ctx, circle, stroke, 1.0)
            return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
                val pos = Pos(rr, rr)
                val circle = Circle(pos, r.toDouble() + 1)
                DrawUtil.drawCircle(ctx, circle, stroke, lw.toDouble(), faction?.color ?: Colors.white)
                when (item) {
                    MOVE -> drawAgentCircle(ctx, Circle(pos, rr - 2.0))
                    EXPLORE -> {
                        val off = 2
                        drawAgentLine(ctx, Line(Pos(off, off), Pos(w - off, h - off)))
                        drawAgentLine(ctx, Line(Pos(off, h - off), Pos(w - off, off)))
                        drawAgentLine(ctx, Line(Pos(rr, 0), Pos(rr, h)))
                        drawAgentLine(ctx, Line(Pos(0, rr), Pos(w, rr)))
                        drawAgentCircle(ctx, Circle(pos, rr - 2.0))
                    }
                    RECRUIT -> {
                        drawAgentLine(ctx, Line(Pos(rr, 0), Pos(rr, h)))
                        drawAgentLine(ctx, Line(Pos(0, rr), Pos(w, rr)))
                    }
                    ATTACK -> drawAgentLine(ctx, Line(Pos(rr, 0), Pos(rr, h)))
                    LINK -> drawAgentLine(ctx, Line(Pos(0, rr), Pos(w, rr)))
                    DEPLOY -> {
                        drawAgentLine(ctx, Line(Pos(0, rr - 1), Pos(w, rr - 1)))
                        drawAgentLine(ctx, Line(Pos(0, rr + 1), Pos(w, rr + 1)))
                    }
                    CAPTURE -> {
                        drawAgentLine(ctx, Line(Pos(rr, 0), Pos(rr, h)))
                        drawAgentLine(ctx, Line(Pos(0, rr - 1), Pos(w, rr - 1)))
                        drawAgentLine(ctx, Line(Pos(0, rr + 1), Pos(w, rr + 1)))
                    }
                    HACK -> drawAgentCircle(ctx, Circle(pos, rr - 4.0))
                    GLYPH -> drawAgentCircle(ctx, Circle(pos, rr - 3.0))
                    RECHARGE -> {
                        val off = 2
                        drawAgentLine(ctx, Line(Pos(off, h - off), Pos(w - off, off)))
                        drawAgentCircle(ctx, Circle(pos, rr - 2.0))
                    }
                    RECYCLE -> {
                        val off = 2
                        drawAgentLine(ctx, Line(Pos(off, off), Pos(w - off, h - off)))
                        drawAgentCircle(ctx, Circle(pos, rr - 2.0))
                    }
                    WAIT -> Unit
                }
            })
        }
    }
}
