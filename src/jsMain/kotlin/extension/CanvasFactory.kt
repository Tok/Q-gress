package extension

import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D

/**
 * Offscreen-[Canvas] creation + 2D-context helpers — the canvas factory split out of [system.ui.Bootstrap]
 * (the SoC / god-object split, PLAN phase B). Generic DOM-canvas plumbing used across modules (HUD icon
 * pre-renders, the world background buffer, the MapLibre shadow/cost readbacks), with no app-bootstrap or
 * `World` coupling.
 */
object CanvasFactory {
    /** A detached offscreen canvas of [w]×[h] (not attached to the document). */
    fun createOffscreenCanvas(w: Int, h: Int): Canvas {
        val canvas = document.createElement("canvas") as Canvas
        canvas.width = w
        canvas.height = h
        return canvas
    }

    /** Render [drawFun] onto a fresh offscreen [w]×[h] canvas and return it (e.g. a pre-baked HUD icon). */
    fun preRender(w: Int, h: Int, drawFun: (CanvasRenderingContext2D) -> Unit): Canvas {
        val offscreen = createOffscreenCanvas(w, h)
        val offscreenCtx = getContext2D(offscreen)
        drawFun(offscreenCtx)
        return offscreen
    }

    fun getContext2D(canvas: Canvas): Ctx = canvas.getContext("2d") as Ctx

    // A 2D context flagged for frequent readback (getImageData) — tells the browser to keep the buffer
    // CPU-side, avoiding the GPU→CPU round-trip and the "willReadFrequently" performance warning.
    fun readbackCtx(canvas: Canvas): Ctx {
        val opts: dynamic = js("({ willReadFrequently: true })")
        return canvas.asDynamic().getContext("2d", opts) as Ctx
    }
}
