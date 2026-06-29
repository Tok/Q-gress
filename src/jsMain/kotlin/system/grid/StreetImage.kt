package system.grid

import extension.Canvas
import extension.CanvasFactory
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.ImageData

/**
 * The offscreen passability-readback canvas + the street→[ImageData] conversion, split out of [World]
 * (which is otherwise pure game state — this is the imperative/DOM edge). The canvas is never displayed;
 * its 2D context only allocates [ImageData] buffers for the passability-grid readback. [canvas] is set
 * once during load (see `system.ui.Bootstrap`).
 */
object StreetImage {
    lateinit var canvas: Canvas
    private fun ctx() = CanvasFactory.getContext2D(canvas)

    private fun imageDataIndex(x: Int, y: Int, w: Int) = (x + (y * w)) * 4

    fun create(streetMap: Uint8Array, w: Int, h: Int): ImageData {
        val imageData: ImageData = ctx().createImageData(w.toDouble(), h.toDouble())
        for (x in 0 until w) {
            for (y in 0 until h) {
                val rawNoise = streetMap[imageDataIndex(x, y, imageData.width)]
                val index = imageDataIndex(x, h - 1 - y, imageData.width)
                imageData.data.set(arrayOf(rawNoise, rawNoise, rawNoise, Byte.MAX_VALUE), index)
            }
        }
        return imageData
    }
}
