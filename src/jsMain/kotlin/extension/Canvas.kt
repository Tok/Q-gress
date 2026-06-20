package extension

import org.w3c.dom.HTMLCanvasElement

typealias Canvas = HTMLCanvasElement

fun Canvas.w() = this.width.toDouble()
fun Canvas.h() = this.height.toDouble()
