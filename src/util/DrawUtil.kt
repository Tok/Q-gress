package util

import Canvas
import Ctx
import World
import agent.Faction
import agent.NonFaction
import config.*
import items.XmpBurster
import items.deployable.Resonator
import items.level.PortalLevel
import items.level.ResonatorLevel
import items.level.XmpLevel
import org.w3c.dom.*
import portal.Portal
import system.Com
import system.Queues
import util.data.*
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object DrawUtil {
    val CODA = "Coda"
    val AMARILLO = "AmarilloUSAF"

    fun redraw() {
        with(World) {
            redraw(can, ctx())
            allAgents.forEach { it.drawRadius(ctx()) }
            allPortals.forEach { it.drawResonators(ctx()) }
            allNonFaction.forEach { it.draw(ctx()) }
            allFields().forEach { it.draw(ctx()) }
            allLinks().forEach { it.draw(ctx()) }
            allPortals.forEach { it.drawCenter(ctx()) }
            allAgents.forEach { it.draw(ctx()) }
            DrawUtil.drawAttacks()
            if (Styles.isDrawPortalNames) {
                allPortals.forEach { it.drawName(ctx()) }
            }
        }
    }

    fun clearBackground() = redraw(World.bgCan, World.bgCtx(), if (Styles.isDrawNoiseMap) World.noiseImage else null)
    fun clearUserInterface() = redraw(World.uiCan, World.uiCtx())
    fun clear() = redraw(World.can, World.ctx())

    private fun redraw(canvas: Canvas, ctx: Ctx, image: ImageData? = null) {
        canvas.width = window.innerWidth
        canvas.height = window.innerHeight
        if (image != null) {
            ctx.putImageData(image, 0.0, 0.0)
        } else {
            ctx.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
        }
    }

    fun drawLoadingText(text: String) {
        clearUserInterface()
        val fontSize = 21
        val y = World.uiCan.height / 2
        val x = (World.uiCan.width - (text.length * fontSize / 2)) / 2
        val lineWidth = 3.0
        val strokeStyle: String = Colors.black
        strokeText(World.uiCtx(), Coords(x, y), text, Colors.white, fontSize, AMARILLO, lineWidth, strokeStyle)
    }

    fun drawNonFaction(nonFaction: NonFaction) = nonFaction.draw(World.ctx())
    fun drawAllNonFaction(ctx: Ctx) = World.allNonFaction.forEach { it.draw(ctx) }
    fun drawAllPortals(ctx: Ctx) = World.allPortals.forEach { it.drawCenter(ctx) }

    fun redrawUserInterface() {
        clearUserInterface()
        drawTick()
        drawMindUnits()
        drawStats()
        if (Styles.isDrawCom) {
            Com.draw(World.uiCtx())
        }
        if (Styles.isDrawTopAgents) {
            drawTopAgents()
        }
        if (World.mousePos != null) {
            highlightMouse(World.mousePos!!)
        }
    }

    private fun highlightMouse(pos: Coords) {
        val ctx = World.uiCtx()
        val topOffset = Dimensions.topActionOffset
        val botOffset = window.innerHeight - Dimensions.botActionOffset

        val w = World.can.width
        val h = World.can.height
        World.ctx().beginPath()
        World.ctx().fillStyle = "#00000077"
        World.ctx().fillRect(0.0, 0.0, w.toDouble(), topOffset)
        World.ctx().fillRect(0.0, botOffset, w.toDouble(), h.toDouble())
        World.ctx().closePath()

        val r = Dimensions.maxDeploymentRange * Constants.phi
        val circle = Circle(pos, r)

        val tempCan = document.createElement("canvas") as Canvas
        val tempCtx = tempCan.getContext("2d") as Ctx
        tempCan.width = 2 * circle.radius.toInt()
        tempCan.height = 2 * circle.radius.toInt()
        val xOffset = -(circle.center.xx() - r)
        val yOffset = -(circle.center.yy() - r)
        tempCtx.putImageData(World.shadowStreetMap, xOffset, yOffset)

        ctx.beginPath()
        ctx.arc(circle.center.xx(), circle.center.yy(), circle.radius, 0.0, 2.0 * PI)
        ctx.clip()

        ctx.beginPath()
        ctx.drawImage(tempCan, pos.xx() - r, pos.yy() - r, 2 * r, 2 * r)

        ctx.globalAlpha = 0.5

        val color = if (pos.hasClosePortalForClick()) {
            Colors.orange
        } else if (pos.isBuildable()) {
            Colors.white
        } else {
            Colors.red
        }
        val image = Portal.renderPortalCenter(color, PortalLevel.ZERO)
        ctx.drawImage(image, pos.xx() - (image.width / 2), pos.yy() - (image.height / 2))
    }

    fun drawAttacks() {
        val attackQueue: MutableMap<Int, MutableMap<Coords, List<XmpBurster>>> = Queues.attackQueue
        attackQueue.forEach { tickEntry: Map.Entry<Int, MutableMap<Coords, List<XmpBurster>>> ->
            val futureTick = tickEntry.key
            val ticksInFuture = futureTick - World.tick
            val attackMap: MutableMap<Coords, List<XmpBurster>> = tickEntry.value
            attackMap.forEach { attackEntry: Map.Entry<Coords, List<XmpBurster>> ->
                val pos = attackEntry.key
                val bursters = attackEntry.value
                bursters.forEach { xmp ->
                    val image = damageCircleImages.get(xmp.level to ticksInFuture)
                    if (image != null) { //FIXME
                        World.ctx().drawImage(image, pos.xx() - (image.width / 2), pos.yy() - (image.height / 2))
                    }
                }
            }
        }

        val r = Dimensions.maxDeploymentRange.toInt()
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
                World.ctx().drawImage(image, newPos.xx(), newPos.yy())
            }
        }
        Queues.endTick(World.tick)
    }

    private fun getImage(damage: Damage): Canvas {
        val damagePercent = kotlin.math.min(damage.value, 100)
        return if (damage.isCritical) {
            critDamageImages.getValue(damagePercent)
        } else {
            damageImages.getValue(damagePercent)
        }
    }

    private val damageImages: Map<Int, Canvas> = (0..100).map { it to createDamageImage(it, false) }.toMap()
    private val critDamageImages: Map<Int, Canvas> = (0..100).map { it to createDamageImage(it, true) }.toMap()

    private fun createDamageImage(damageValue: Int, isCritical: Boolean): Canvas {
        val fontSize = 11
        val lineWidth = 3.0
        val w = (fontSize * 5) + (2 * lineWidth)
        val h = fontSize + (2 * lineWidth)
        return HtmlUtil.prerender(w.toInt(), h.toInt(), fun(ctx: Ctx) {
            val coords = Coords(lineWidth.toInt() + (fontSize * 3 / 2), lineWidth.toInt() + (fontSize / 2))
            val clipped = max(damageValue, 1).toString()
            val color = if (isCritical) Colors.critDamage else Colors.damage
            val text = "-" + clipped + "%"
            strokeText(ctx, coords, text, Colors.white, fontSize, CODA, lineWidth, color)
        })
    }

    private val damageCircleImages: Map<Pair<XmpLevel, Int>, Canvas> = XmpLevel.values().flatMap { xmpLevel ->
        (0..Queues.attackDelayTicks).map { ticksInFuture -> (xmpLevel to ticksInFuture) to createDamageCircleImage(xmpLevel, ticksInFuture) }
    }.toMap()

    private fun createDamageCircleImage(xmpLevel: XmpLevel, ticksInFuture: Int): Canvas {
        val strokeStyle = "#ff731533"
        val fillStyle = "#fece5a11"
        val lineWidth = 8
        val ratio = (Queues.damageDelayTicks - ticksInFuture) / Queues.damageDelayTicks
        val r = xmpLevel.rangeM * ratio / 2
        val w = r * 2 + (2 * lineWidth)
        val h = w
        return HtmlUtil.prerender(w, h, fun(ctx: Ctx) {
            val attackCircle = Circle(Coords(r + lineWidth, r + lineWidth), r.toDouble())
            drawCircle(ctx, attackCircle, strokeStyle, lineWidth.toDouble(), fillStyle)
        })
    }

    fun drawStats() {
        val fontSize = Dimensions.statsFontSize
        val lineWidth = 3.0
        fun drawCell(pos: Coords, text: String, color: String) {
            strokeText(World.uiCtx(), pos, text, color, fontSize, CODA, lineWidth, Colors.black, CanvasTextAlign.END)
        }

        val rightXOffset = 300
        val yStep = fontSize * 3 / 2

        val xStep = 55
        fun drawRow(pos: Int, header: String, enl: Int, res: Int, total: Int) {
            drawCell(Coords(pos, yStep), header, Colors.white)
            drawCell(Coords(pos, yStep * 2), enl.toString(), Faction.ENL.color)
            drawCell(Coords(pos, yStep * 3), res.toString(), Faction.RES.color)
            drawCell(Coords(pos, yStep * 4), total.toString(), Colors.white)
        }

        val xPos = World.can.width - rightXOffset
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

    fun drawTick() {
        val pos = Coords(13, World.uiCan.height - Dimensions.tickBottomOffset)
        val half = Dimensions.tickFontSize / 2
        World.uiCtx().fillStyle = "#00000077"
        World.uiCtx().fillRect(pos.xx() - 8, pos.yy() - half - 1, 164.0, Dimensions.tickFontSize + 2.0)
        World.uiCtx().fill()
        World.uiCtx().globalAlpha = 3.0
        val stamp = Util.ticksToTimestamp(World.tick)
        drawText(World.uiCtx(), pos, stamp, Colors.white, Dimensions.tickFontSize, CODA)
        val tick = " Tick: " + World.tick
        drawText(World.uiCtx(), pos.copy(x = pos.x + 55), tick, Colors.white, Dimensions.tickFontSize, CODA)
    }

    fun drawMindUnits() {
        //TODO only redraw if updated.
        fun fillMuRect(from: Coords, width: Double, height: Double,
                       fillStyle: String, strokeStyle: String, lineWidth: Double) {
            with(World) {
                if (Styles.isFillMuDisplay) {
                    uiCtx().globalAlpha = 0.3
                    uiCtx().fillStyle = fillStyle
                    uiCtx().fillRect(from.x.toDouble(), from.y.toDouble(), width, height)
                }
                uiCtx().strokeStyle = strokeStyle
                uiCtx().globalAlpha = 1.0
                uiCtx().lineWidth = lineWidth
                uiCtx().beginPath()
                uiCtx().strokeRect(from.x.toDouble(), from.y.toDouble(), width, height)
                uiCtx().closePath()
                uiCtx().stroke()
            }
        }

        fun drawMuRect(pos: Coords, part: Int, faction: Faction, mu: Int) {
            val fromRect = Coords(pos.x, pos.y - Dimensions.muFontSize)
            val width = 1.5 * part
            val height = Dimensions.muFontSize.toDouble() * Constants.phi
            fillMuRect(fromRect, width, height, faction.color, faction.color, 3.0)
            val text = faction.abbr + " " + mu + "M"
            val textPos = Coords(pos.x + 21, pos.y - 3)
            strokeText(World.uiCtx(), textPos, text, faction.color, Dimensions.muFontSize, AMARILLO)
        }

        val enlMu = World.calcTotalMu(Faction.ENL)
        val resMu = World.calcTotalMu(Faction.RES)
        val totalMu = enlMu + resMu
        val enlPart: Int = round((100.0 * enlMu) / totalMu).toInt()
        val resPart: Int = round((100.0 * resMu) / totalMu).toInt()
        val xPos = Dimensions.muLeftOffset
        val yPos = World.can.height - Dimensions.muBottomOffset
        val enlPos = Coords(xPos, yPos - Dimensions.muFontSize * 2)
        val resPos = Coords(xPos, yPos)
        drawMuRect(enlPos, enlPart, Faction.ENL, enlMu)
        drawMuRect(resPos, resPart, Faction.RES, resMu)
    }

    fun drawTopAgents() {
        val ctx = World.uiCtx()
        val xPos = Dimensions.topAgentsLeftOffset
        val yOffset = Dimensions.topAgentsFontSize * 3 / 2
        val xOffset = (Dimensions.topAgentsInventoryFontSize * Constants.phi).toInt()
        val yFixOffset = World.can.height - Dimensions.topAgentsBottomOffset - (Config.topAgentsMessageLimit * yOffset)
        val headerPos = Coords(xPos, yFixOffset - yOffset)
        val top = World.allAgents.toList().sortedBy { -it.ap }.take(Config.topAgentsMessageLimit)
        val headerText =
                "Agent AP                                               " +
                        "XMPs                                                             " +
                        "Resos                                                            " +
                        "Keys"
        strokeText(ctx, headerPos, headerText, Colors.white, Dimensions.topAgentsInventoryFontSize, CODA, 2.0, Colors.black)

        val invFontSize = Dimensions.topAgentsInventoryFontSize
        top.forEachIndexed { index, agent ->
            val rank = (index + 1).toString()
            val name = agent.toString()
            val ap = agent.ap
            val text = "$rank: $ap $name"
            val pos = Coords(xPos, yFixOffset + (yOffset * index))
            strokeText(ctx, pos, text, agent.faction.color, Dimensions.topAgentsFontSize, CODA, 3.0, Colors.black)
            val columnOff = 160

            //XMP counts
            val xmpGroups: Map<XmpLevel, List<XmpBurster>>? = agent.inventory.findXmps()?.groupBy { it.level }
            val xmpLevels: Map<XmpLevel, Int>? = xmpGroups?.mapValues { it.value.count() }
            XmpLevel.values().forEachIndexed { i, level ->
                val xmpPos = Coords(xPos + columnOff + (xOffset * i), yFixOffset + (yOffset * index))
                val clipped = min(xmpLevels?.get(level) ?: 0, 100)
                val xmpText = if (clipped <= 99) {
                    clipped.toString()
                } else "##"
                strokeText(ctx, xmpPos, xmpText, level.color, invFontSize, CODA, 2.0, Colors.black, CanvasTextAlign.END)
            }

            //Reso counts
            val resoGroups: Map<ResonatorLevel, List<Resonator>>? = agent.inventory.findResonators()?.groupBy { it.level }
            val resoLevels: Map<ResonatorLevel, Int>? = resoGroups?.mapValues { it.value.count() }
            ResonatorLevel.values().forEachIndexed { i, level ->
                val resoPos = Coords(xPos + (2 * columnOff) + (xOffset * i), yFixOffset + (yOffset * index))
                val clipped = min(resoLevels?.get(level) ?: 0, 100)
                val resoText = if (clipped <= 99) {
                    clipped.toString()
                } else "##"
                strokeText(ctx, resoPos, resoText, level.color, invFontSize, CODA, 2.0, Colors.black, CanvasTextAlign.END)
            }

            //Key counts
            val clipped = min(agent.inventory.keyCount(), 100)
            val keyText = if (clipped <= 99) {
                clipped.toString()
            } else "##"
            val keyPos = Coords(xPos + (3 * columnOff), yFixOffset + (yOffset * index))
            strokeText(ctx, keyPos, keyText, Colors.white, invFontSize, CODA, 2.0, Colors.black, CanvasTextAlign.END)
        }
    }

    fun drawGrid() {
        clearBackground()
        with(World) {
            if (isReady) {
                grid.forEach {
                    val pos = PathUtil.shadowPosToPos(it.key)
                    val cell = it.value
                    bgCtx().fillStyle = cell.getColor()
                    bgCtx().fillRect(pos.xx() + 1, pos.yy() + 1, PathUtil.RESOLUTION.toDouble() - 1, PathUtil.RESOLUTION.toDouble() - 1)
                    bgCtx().fill()
                }
            }
        }
    }

    fun drawVectorField(portal: Portal) {
        drawVectorField(portal.vectorField)
        portal.drawCenter(World.bgCtx())
    }

    fun drawVectorField(vectorField: Map<Coords, Complex>) {
        World.bgCtx().clearRect(0.0, 0.0, World.can.width.toDouble(), World.can.height.toDouble())
        val w = PathUtil.RESOLUTION - 1
        val h = PathUtil.RESOLUTION - 1
        with(World) {
            vectorField.forEach {
                fun isWalkable() = World.grid.get(it.key)?.isPassable ?: false
                if (Styles.isDrawObstructedVectors || isWalkable()) {
                    val vectorImageData = getOrCreateVectorImageData(w, h, it.value)
                    val pos = PathUtil.shadowPosToPos(it.key)
                    World.bgCtx().putImageData(vectorImageData, pos.xx(), pos.yy())
                }
            }
        }
    }

    private val VECTORS = mutableMapOf<Line, ImageData>()
    private fun getOrCreateVectorImageData(w: Int, h: Int, complex: Complex): ImageData {
        val center = PathUtil.RESOLUTION / 2
        val scale = center
        val scaled = Complex.fromMagnitudeAndPhase(complex.magnitude * scale, complex.phase)
        val line = Line(Coords(center, center), Coords(center + scaled.re.toInt(), center + scaled.im.toInt()))
        val maybeImage = VECTORS.get(line)
        if (maybeImage != null) {
            return maybeImage
        } else {
            val newImageCan = createVectorImage(w, h, complex, line)
            val newImageCtx = newImageCan.getContext("2d") as Ctx
            val imageData = newImageCtx.getImageData(0.toDouble(), 0.toDouble(), w.toDouble(), h.toDouble())
            VECTORS.put(line, imageData)
            return imageData
        }
    }

    private fun createVectorImage(w: Int, h: Int, complex: Complex, line: Line): Canvas {
        return HtmlUtil.prerender(w, h, fun(ctx: Ctx) {
            ctx.fillStyle = "#ffffff22"
            when (Styles.vectorStyle) {
                Styles.VectorStyle.CIRCLE -> {
                    val r = w / 2.0
                    val path = Path2D()
                    path.moveTo(r, r)
                    path.arc(r, r, r, 0.0, 2.0 * kotlin.math.PI)
                    ctx.fill(path)
                }
                Styles.VectorStyle.SQUARE -> {
                    ctx.fillRect(1.0, 1.0, w.toDouble(), h.toDouble())
                    ctx.fill()
                }
            }
            val lineWidth = 2.0
            val strokeStyle = ColorUtil.getColor(complex) + "AA"
            drawLine(ctx, line, strokeStyle, lineWidth)
        })
    }

    fun drawText(ctx: Ctx, coords: Coords, text: String, fillStyle: String, fontSize: Int, fontName: String) {
        with(World) {
            ctx.textAlign = CanvasTextAlign.START
            ctx.font = fontSize.toString() + "px '$fontName'"
            ctx.fillStyle = fillStyle
            val xOff = (fontSize / 2) - 2
            val yOff = fontSize / 3
            ctx.fillText(text, coords.x.toDouble() - xOff, coords.y.toDouble() + yOff)
        }
    }

    fun strokeText(ctx: Ctx, coords: Coords, text: String, fillStyle: String, fontSize: Int, fontName: String = CODA,
                   lineWidth: Double = 0.0, strokeStyle: String = Colors.black,
                   textAlign: CanvasTextAlign = CanvasTextAlign.START) {
        val xOff: Double = (fontSize / 2.0) - 2
        val yOff: Double = fontSize / 3.0
        with(World) {
            ctx.beginPath()
            ctx.font = fontSize.toString() + "px '$fontName'"
            ctx.fillStyle = fillStyle
            ctx.lineCap = CanvasLineCap.ROUND
            ctx.lineJoin = CanvasLineJoin.ROUND
            ctx.textAlign = textAlign
            if (lineWidth > 0.0) {
                ctx.lineWidth = lineWidth
                ctx.strokeStyle = strokeStyle
                ctx.strokeText(text, coords.x - xOff, coords.y + yOff)
            }
            ctx.fillText(text, coords.x - xOff, coords.y + yOff)
            ctx.closePath()
            if (lineWidth > 0.0) {
                ctx.stroke()
            }
        }
    }

    fun drawCircle(ctx: Ctx, circle: Circle, strokeStyle: String, lineWidth: Double, fillStyle: String? = null, alpha: Double = 1.0) {
        with(World) {
            ctx.globalAlpha = alpha
            ctx.strokeStyle = strokeStyle
            ctx.lineWidth = lineWidth
            ctx.beginPath()
            ctx.arc(circle.center.xx(), circle.center.yy(), circle.radius, 0.0, 2.0 * PI)
            ctx.closePath()
            ctx.stroke()
            if (fillStyle != null) {
                ctx.fillStyle = fillStyle
                ctx.fill()
            }
            ctx.globalAlpha = 1.0
        }
    }

    fun drawLine(ctx: Ctx, line: Line, strokeStyle: String, lineWidth: Double) {
        ctx.strokeStyle = strokeStyle
        ctx.lineWidth = lineWidth
        ctx.beginPath()
        ctx.moveTo(line.from.xx(), line.from.yy())
        ctx.lineTo(line.to.xx(), line.to.yy())
        ctx.closePath()
        ctx.stroke()
    }
}
