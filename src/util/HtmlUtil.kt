package util

import Canvas
import Ctx
import config.Time
import org.w3c.dom.*
import org.w3c.dom.events.Event
import util.data.Cell
import util.data.Coords
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.addClass

object HtmlUtil {
    fun createSliderDiv(className: String, value: Int, max: Int,
                        id: String, suffix: String, min: Int = 0): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
        val slider = document.createElement("INPUT") as HTMLInputElement
        slider.id = id
        slider.type = "range"
        slider.min = min.toString()
        slider.max = max.toString()
        slider.value = value.toString()
        slider.addClass("slider", className)
        val sliderValue = document.createElement("span") as HTMLSpanElement
        sliderValue.addClass("sliderLabel")
        slider.oninput = { _ -> sliderValue.innerHTML = slider.value + suffix; null }
        div.appendChild(slider)
        div.appendChild(sliderValue)
        sliderValue.innerHTML = slider.value + suffix
        return div
    }

    fun createButtonDiv(className: String, text: String, callback: ((Event) -> Unit)?): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
        val button = document.createElement("BUTTON") as HTMLButtonElement
        button.addClass(className)
        button.onclick = callback
        button.innerText = text
        div.appendChild(button)
        return div
    }

    fun createCanvas(className: String): Canvas {
        val canvas = document.createElement("canvas") as Canvas
        canvas.addClass("canvas", className)
        canvas.width = window.innerWidth
        canvas.height = window.innerHeight
        return canvas
    }

    fun createOffscreenCanvas(w: Int, h: Int): Canvas {
        val canvas = document.createElement("canvas") as Canvas
        canvas.width = w
        canvas.height = h
        return canvas
    }

    fun prerender(w: Int, h: Int, drawFun: (CanvasRenderingContext2D) -> Unit): Canvas {
        val offscreen = HtmlUtil.createOffscreenCanvas(w, h)
        val offscreenCtx = HtmlUtil.getContext2D(offscreen)
        drawFun(offscreenCtx)
        return offscreen
    }

    fun getContext2D(canvas: Canvas): Ctx = canvas.getContext("2d") as Ctx

    fun pauseHandler(intervalID: Int, tickFunction: () -> Unit): Int {
        if (intervalID != -1) {
            document.defaultView?.clearInterval(intervalID)
            return -1
        } else {
            return document.defaultView?.setInterval({ tickFunction() }, Time.minTickInterval) ?: 0
        }
    }
}
