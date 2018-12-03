package system.display

import World
import config.Colors
import config.Dim
import extension.Canvas
import extension.Ctx
import items.XmpBurster
import items.level.XmpLevel
import system.Queues
import util.DrawUtil
import util.HtmlUtil
import util.data.Circle
import util.data.Pos
import util.data.Damage
import kotlin.math.max

object Attacks : Display {
    override fun draw() {
        val attackQueue: MutableMap<Int, MutableMap<Pos, List<XmpBurster>>> = Queues.attackQueue
        attackQueue.forEach { tickEntry: Map.Entry<Int, MutableMap<Pos, List<XmpBurster>>> ->
            val futureTick = tickEntry.key
            val ticksInFuture = futureTick - World.tick
            val attackMap: MutableMap<Pos, List<XmpBurster>> = tickEntry.value
            attackMap.forEach { attackEntry: Map.Entry<Pos, List<XmpBurster>> ->
                val pos = attackEntry.key
                val bursters = attackEntry.value
                bursters.forEach { xmp ->
                    val image = damageCircleImages[xmp.level to ticksInFuture]
                    if (image != null) { //FIXME
                        World.ctx().drawImage(image, pos.x - (image.width / 2), pos.y - (image.height / 2))
                    }
                }
            }
        }

        val r = Dim.maxDeploymentRange.toInt()
        val damageQueue: MutableMap<Int, List<Damage>> = Queues.damageQueue
        damageQueue.forEach { damageEntry: Map.Entry<Int, List<Damage>> ->
            val futureTick = damageEntry.key
            val ticksInFuture = futureTick - World.tick
            val ratio = (Queues.damageDelayTicks - ticksInFuture) / Queues.damageDelayTicks
            val damageList: List<Damage> = damageEntry.value
            damageList.forEach { damage: Damage ->
                val pos = damage.pos
                val lineWidth = 3
                val newPos = pos.copy(y = pos.y - ratio - lineWidth, x = pos.x - r + lineWidth)
                val image = getImage(damage)
                World.ctx().drawImage(image, newPos.x, newPos.y)
            }
        }
        Queues.endTick(World.tick)
    }

    fun getImage(damage: Damage): Canvas {
        val damagePercent = kotlin.math.min(damage.value, 100)
        return if (damage.isCritical) {
            critDamageImages.getValue(damagePercent)
        } else {
            damageImages.getValue(damagePercent)
        }
    }

    val damageCircleImages: Map<Pair<XmpLevel, Int>, Canvas> = XmpLevel.values().flatMap { xmpLevel ->
        (0..Queues.attackDelayTicks).map { ticksInFuture -> (xmpLevel to ticksInFuture) to createDamageCircleImage(xmpLevel, ticksInFuture) }
    }.toMap()

    private val damageImages: Map<Int, Canvas> = (0..100).map { it to createDamageImage(it, false) }.toMap()
    private val critDamageImages: Map<Int, Canvas> = (0..100).map { it to createDamageImage(it, true) }.toMap()

    private fun createDamageImage(damageValue: Int, isCritical: Boolean): Canvas {
        val fontSize = 11
        val lineWidth = 3.0
        val w = (fontSize * 5) + (2 * lineWidth)
        val h = fontSize + (2 * lineWidth)
        return HtmlUtil.preRender(w.toInt(), h.toInt(), fun(ctx: Ctx) {
            val coords = Pos(lineWidth.toInt() + (fontSize * 3 / 2), lineWidth.toInt() + (fontSize / 2))
            val clipped = max(damageValue, 1).toString()
            val color = if (isCritical) Colors.critDamage else Colors.damage
            val text = "-$clipped%"
            DrawUtil.strokeText(ctx, coords, text, Colors.white, fontSize, DrawUtil.CODA, lineWidth, color)
        })
    }

    private fun createDamageCircleImage(xmpLevel: XmpLevel, ticksInFuture: Int): Canvas {
        val stroke = "#ff731533"
        val fill = "#fece5a11"
        val lw = 8
        val ratio = (Queues.damageDelayTicks - ticksInFuture) / Queues.damageDelayTicks
        val r = (xmpLevel.rangeM * Dim.pixelToMFactor * ratio).toInt()
        val w = (r * 2) + (2 * lw)
        val h = w
        return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
            val attackCircle = Circle(Pos(r + lw, r + lw), r.toDouble())
            DrawUtil.drawCircle(ctx, attackCircle, stroke, lw.toDouble(), fill)
        })
    }
}
