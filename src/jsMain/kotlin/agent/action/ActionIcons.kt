package agent.action

import agent.Faction
import agent.action.ActionItem.Companion.ATTACK
import agent.action.ActionItem.Companion.CAPTURE
import agent.action.ActionItem.Companion.DEPLOY
import agent.action.ActionItem.Companion.EXPLORE
import agent.action.ActionItem.Companion.GLYPH
import agent.action.ActionItem.Companion.HACK
import agent.action.ActionItem.Companion.LINK
import agent.action.ActionItem.Companion.MOVE
import agent.action.ActionItem.Companion.RECHARGE
import agent.action.ActionItem.Companion.RECRUIT
import agent.action.ActionItem.Companion.RECYCLE
import agent.action.ActionItem.Companion.VIRUS
import agent.action.ActionItem.Companion.WAIT
import config.Colors
import config.Dim
import extension.Canvas
import extension.CanvasFactory
import extension.Ctx
import system.ui.Bootstrap
import system.ui.HudRenderer
import util.data.Circle
import util.data.Line
import util.data.Pos

/**
 * The JS-canvas action-icon prerender for [ActionItem] — kept in the JS shell (Canvas/Ctx/HudRenderer) so the
 * pure [ActionItem] data can ride the commonMain lift. Each action's little disc-plus-glyph is vector-redrawn
 * once at construction (base + a hi-res set for the 3D agent billboards); call sites read [getIcon] /
 * [getHiResIcon].
 */
object ActionIcons {
    private val enlImages = if (Bootstrap.isRunningInBrowser()) {
        ActionItem.values().map { it to drawTemplate(it, Faction.ENL) }.toMap()
    } else {
        emptyMap()
    }
    private val resImages = if (Bootstrap.isRunningInBrowser()) {
        ActionItem.values().map { it to drawTemplate(it, Faction.RES) }.toMap()
    } else {
        emptyMap()
    }
    private val nonImages = if (Bootstrap.isRunningInBrowser()) {
        ActionItem.values().map { it to drawTemplate(it) }.toMap()
    } else {
        emptyMap()
    }

    // The base icon is only ~10 px wide (slider-sized). The 3D billboards above agents scale it up
    // a lot, so we keep a separate set re-rendered at ICON_HIRES_SCALE× (vector redraw → crisp).
    private const val ICON_HIRES_SCALE = 20
    private val enlHiRes = hiResSet(Faction.ENL)
    private val resHiRes = hiResSet(Faction.RES)
    private val nonHiRes = hiResSet(null)

    private fun hiResSet(faction: Faction?): Map<ActionItem, Canvas> = if (Bootstrap.isRunningInBrowser()) {
        ActionItem.values().mapNotNull { i ->
            drawTemplate(i, faction, ICON_HIRES_SCALE)?.let {
                i to
                    it
            }
        }.toMap()
    } else {
        emptyMap()
    }

    fun getIcon(item: ActionItem, faction: Faction? = null): Canvas = when (faction) {
        Faction.ENL -> enlImages[item] ?: requireNotNull(enlImages[WAIT]) { "missing ENL WAIT icon" }
        Faction.RES -> resImages[item] ?: requireNotNull(resImages[WAIT]) { "missing RES WAIT icon" }
        else -> nonImages[item] ?: requireNotNull(nonImages[WAIT]) { "missing non-faction WAIT icon" }
    }

    /** A high-resolution copy of [getIcon] for the 3D agent billboards (avoids upscale blur). */
    fun getHiResIcon(item: ActionItem, faction: Faction? = null): Canvas = when (faction) {
        Faction.ENL -> enlHiRes[item] ?: requireNotNull(enlHiRes[WAIT]) { "missing ENL WAIT hi-res icon" }
        Faction.RES -> resHiRes[item] ?: requireNotNull(resHiRes[WAIT]) { "missing RES WAIT hi-res icon" }
        else -> nonHiRes[item] ?: requireNotNull(nonHiRes[WAIT]) { "missing non-faction WAIT hi-res icon" }
    }

    private fun drawTemplate(item: ActionItem, faction: Faction? = null, scale: Int = 1): Canvas? {
        if (Bootstrap.isNotRunningInBrowser()) return null
        val rr = Dim.agentRadius + Dim.agentLineWidth
        val w = rr * 2
        return CanvasFactory.preRender(w * scale, w * scale, fun(ctx: Ctx) {
            ctx.scale(scale.toDouble(), scale.toDouble()) // vector redraw at higher res (1 = no-op)
            val circle = Circle(Pos(rr, rr), Dim.agentRadius.toDouble() + 1)
            HudRenderer.drawCircle(ctx, circle, Colors.black, Dim.agentLineWidth.toDouble(), faction?.color ?: Colors.white)
            drawGlyph(ctx, item, w, rr)
        })
    }

    private fun line(ctx: Ctx, a: Line) = HudRenderer.drawLine(ctx, a, Colors.black, 0.7)
    private fun circ(ctx: Ctx, c: Circle) = HudRenderer.drawCircle(ctx, c, Colors.black, 1.0)
    private fun dot(ctx: Ctx, c: Pos, r: Double) = HudRenderer.drawCircle(ctx, Circle(c, r), Colors.black, 1.0, Colors.black)

    // The per-action glyph drawn over the agent disc (split out of drawTemplate for complexity).
    private fun drawGlyph(ctx: Ctx, item: ActionItem, w: Int, rr: Int) {
        val h = w
        val pos = Pos(rr, rr)
        val off = 2
        when (item) {
            MOVE -> circ(ctx, Circle(pos, rr - 2.0))
            // Wander (the no-idle roam, reusing the retired EXPLORE action): a single light dot — minimal,
            // clearly "just ambling", not a heavy action glyph.
            EXPLORE -> dot(ctx, pos, 1.8)
            RECRUIT -> {
                line(ctx, Line(Pos(rr, 0), Pos(rr, h)))
                line(ctx, Line(Pos(0, rr), Pos(w, rr)))
            }
            // ATTACK is an "X" and LINK a single line: in 2D they were the same line rotated 90°, but on the
            // 3D pill (free billboard orientation) you can't tell horizontal from vertical — so make them
            // shape-distinct. The X reuses the diagonals freed from the old EXPLORE glyph.
            ATTACK -> {
                line(ctx, Line(Pos(off, off), Pos(w - off, h - off)))
                line(ctx, Line(Pos(off, h - off), Pos(w - off, off)))
            }
            LINK -> line(ctx, Line(Pos(0, rr), Pos(w, rr)))
            DEPLOY -> {
                line(ctx, Line(Pos(0, rr - 1), Pos(w, rr - 1)))
                line(ctx, Line(Pos(0, rr + 1), Pos(w, rr + 1)))
            }
            CAPTURE -> {
                line(ctx, Line(Pos(rr, 0), Pos(rr, h)))
                line(ctx, Line(Pos(0, rr - 1), Pos(w, rr - 1)))
                line(ctx, Line(Pos(0, rr + 1), Pos(w, rr + 1)))
            }
            HACK -> circ(ctx, Circle(pos, rr - 4.0))
            GLYPH -> circ(ctx, Circle(pos, rr - 3.0))
            RECHARGE -> {
                line(ctx, Line(Pos(off, h - off), Pos(w - off, off)))
                circ(ctx, Circle(pos, rr - 2.0))
            }
            RECYCLE -> {
                line(ctx, Line(Pos(off, off), Pos(w - off, h - off)))
                circ(ctx, Circle(pos, rr - 2.0))
            }
            VIRUS -> {
                circ(ctx, Circle(pos, rr - 4.0))
                line(ctx, Line(Pos(off, off), Pos(w - off, h - off)))
            }
            WAIT -> Unit
        }
    }
}
