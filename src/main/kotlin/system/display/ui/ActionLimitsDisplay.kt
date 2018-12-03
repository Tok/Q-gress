package system.display.ui

import World
import config.Dim
import system.display.Display
import util.HtmlUtil
import util.data.Pos
import util.data.Line

object ActionLimitsDisplay : Display {
    private fun topArea() = Line.create(0, 0, Dim.width, HtmlUtil.topActionOffset())
    private fun bottomArea() = Line.create(0, Dim.height - Dim.botActionOffset.toInt(), Dim.width, Dim.height)
    private fun leftSliderMouseArea() = Line.create(0, HtmlUtil.topActionOffset(), HtmlUtil.leftSliderWidth(), HtmlUtil.topActionOffset() + HtmlUtil.leftSliderHeight())
    private fun rightSliderMouseArea() = Line.create(Dim.width - HtmlUtil.rightSliderWidth(), HtmlUtil.topActionOffset(), Dim.width, HtmlUtil.topActionOffset() + HtmlUtil.rightSliderHeight())
    private fun leftSliderArea() = Line.create(0, HtmlUtil.topActionOffset(), HtmlUtil.leftSliderWidth(), HtmlUtil.leftSliderHeight())
    private fun rightSliderArea() = Line.create(Dim.width - HtmlUtil.rightSliderWidth(), HtmlUtil.topActionOffset(), Dim.width, HtmlUtil.rightSliderHeight())
    private fun blockedAreas() = listOf(topArea(), bottomArea(), leftSliderMouseArea(), rightSliderMouseArea())

    fun isBlocked(pos: Pos) = blockedAreas().any { it.isPointInArea(pos) }
    fun isNotBlocked(pos: Pos) = blockedAreas().none { it.isPointInArea(pos) }

    override fun draw() {
        drawArea(topArea())
        drawArea(bottomArea())
        drawArea(leftSliderArea())
        drawArea(rightSliderArea())
    }

    fun drawTop() = drawArea(topArea())

    private fun drawArea(area: Line) {
        with(World.ctx()) {
            beginPath()
            fillStyle = "#00000077"
            fillArea(area)
            closePath()
        }
    }

    private fun fillArea(line: Line) {
        if (line.isValidArea()) {
            with(line) {
                World.ctx().fillRect(fromX, fromY, toX, toY)
            }
        }
    }
}
