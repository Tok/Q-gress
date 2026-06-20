package extension

import org.w3c.dom.CanvasRenderingContext2D
import util.data.Line

typealias Ctx = CanvasRenderingContext2D

fun Ctx.clear(can: Canvas) = this.clearRect(0.0, 0.0, can.w(), can.h())

fun Ctx.clearRect(rect: Line) =
    this.clearRect(rect.fromX, rect.fromY, rect.w, rect.h)

fun Ctx.drawImage(image: Canvas, x: Int, y: Int) =
    this.drawImage(image, x.toDouble(), y.toDouble())

fun Ctx.drawImage(image: Canvas, rect: Line) =
    this.drawImage(image, rect.fromX, rect.fromY, rect.w, rect.h)

fun Ctx.drawImage(image: Canvas, x: Int, y: Int, w: Int, h: Int) =
    this.drawImage(image, x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())

fun Ctx.getImageData(x: Int, y: Int, w: Int, h: Int) =
    this.getImageData(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
