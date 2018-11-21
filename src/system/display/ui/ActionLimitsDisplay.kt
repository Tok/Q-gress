package system.display.ui

import World
import config.Dim
import system.display.Display
import util.HtmlUtil
import util.data.Coords
import util.data.Line

object ActionLimitsDisplay : Display {
    private val topArea = Line.create(0, 0, Dim.width, HtmlUtil.topActionOffset())
    private val bottomArea = Line.create(0, Dim.height - Dim.botActionOffset.toInt(), Dim.width, Dim.height)
    private val leftSliderArea = Line.create(0, HtmlUtil.topActionOffset(), HtmlUtil.leftSliderWidth(), HtmlUtil.leftSliderHeight())
    private val rightSliderArea = Line.create(Dim.width - HtmlUtil.rightSliderWidth(), HtmlUtil.topActionOffset(), Dim.width, HtmlUtil.rightSliderHeight())

    fun isInLimit(pos: Coords) = topArea.isPointInArea(pos) ||
            leftSliderArea.isPointInArea(pos) ||
            rightSliderArea.isPointInArea(pos)

    override fun draw() {
        return draw(true)
    }

    fun draw(isHighlightBottom: Boolean) {
        with(World.ctx()) {
            beginPath()
            fillStyle = "#00000077"
            fillRect(topArea.fromX, topArea.fromY, topArea.toX, topArea.toY)
            if (isHighlightBottom) {
                fillRect(bottomArea.fromX, bottomArea.fromY, bottomArea.toX, bottomArea.toY)
            }
            fillRect(leftSliderArea.fromX, leftSliderArea.fromY, leftSliderArea.toX, leftSliderArea.toY)
            fillRect(rightSliderArea.fromX, rightSliderArea.fromY, rightSliderArea.toX, rightSliderArea.toY)
            closePath()
        }
    }
}
