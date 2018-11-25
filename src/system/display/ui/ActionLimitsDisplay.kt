package system.display.ui

import World
import config.Dim
import system.display.Display
import util.DrawUtil
import util.HtmlUtil
import util.data.Coords
import util.data.Line

object ActionLimitsDisplay : Display {
    private fun topArea() = Line.create(0, 0, Dim.width, HtmlUtil.topActionOffset())
    private fun bottomArea() = Line.create(0, Dim.height - Dim.botActionOffset.toInt(), Dim.width, Dim.height)
    private fun leftSliderMouseArea() = Line.create(0, HtmlUtil.topActionOffset(), HtmlUtil.leftSliderWidth(), HtmlUtil.topActionOffset() + HtmlUtil.leftSliderHeight())
    private fun rightSliderMouseArea() = Line.create(Dim.width - HtmlUtil.rightSliderWidth(), HtmlUtil.topActionOffset(), Dim.width, HtmlUtil.topActionOffset() + HtmlUtil.rightSliderHeight())
    private fun leftSliderArea() = Line.create(0, HtmlUtil.topActionOffset(), HtmlUtil.leftSliderWidth(), HtmlUtil.leftSliderHeight())
    private fun rightSliderArea() = Line.create(Dim.width - HtmlUtil.rightSliderWidth(), HtmlUtil.topActionOffset(), Dim.width, HtmlUtil.rightSliderHeight())
    private fun blockedAreas() = listOf(topArea(), bottomArea(), leftSliderMouseArea(), rightSliderMouseArea())

    fun isBlocked(pos: Coords) = blockedAreas().any { it.isPointInArea(pos) }
    fun isNotBlocked(pos: Coords) = blockedAreas().none { it.isPointInArea(pos) }

    override fun draw() = draw(true)

    fun draw(isHighlightBottom: Boolean) {
        val top = topArea()
        val bot = bottomArea()
        val left = leftSliderArea()
        val right = rightSliderArea()
        fun fillArea(line: Line) {
            if (line.isValidArea()) {
                with(line) {
                    World.ctx().fillRect(fromX, fromY, toX, toY)
                }
            }
        }
        with(World.ctx()) {
            beginPath()
            fillStyle = "#00000077"
            fillArea(top)
            if (isHighlightBottom) {
                fillArea(bot)
            }
            fillArea(left)
            fillArea(right)
            closePath()
        }
    }
}
