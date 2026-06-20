package util.ui

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import org.w3c.dom.HTMLDivElement

/**
 * Onboarding/help UI: an on-screen controls legend and a desktop-only gate
 * (the game needs WebGL + a mouse). Kept out of HtmlUtil to avoid bloating it.
 */
object Controls {
    /** True if the device can't reasonably run the game (no WebGL, or pure-touch / no mouse). */
    fun isUnsupported(): Boolean {
        val canvas = document.createElement("canvas")
        val gl = canvas.asDynamic().getContext("webgl2") ?: canvas.asDynamic().getContext("webgl")
        if (gl == null) return true
        val coarse = window.matchMedia("(pointer: coarse)").matches
        val fine = window.matchMedia("(pointer: fine)").matches
        return coarse && !fine // touch-only, no mouse
    }

    fun showUnsupportedNotice() {
        val div = document.createElement("div") as HTMLDivElement
        div.addClass("notice", "coda")
        div.innerHTML =
            "<div class=\"noticeBox\">" +
            "<div class=\"noticeTitle amarillo\">Q-Gress</div>" +
            "Desktop only — please open this on a desktop browser with a mouse and WebGL." +
            "</div>"
        document.body?.append(div)
    }

    fun addLegend() {
        val div = document.createElement("div") as HTMLDivElement
        div.id = "controlsLegend"
        div.addClass("controlsLegend", "coda")
        div.innerHTML =
            "<b>Controls</b><br>" +
            "Wheel: zoom &middot; Right-drag: pan<br>" +
            "WASD: move &middot; Q/E: rotate &middot; R/F: pitch<br>" +
            "Click portal: select &middot; Click ground: build"
        document.body?.append(div)
    }
}
