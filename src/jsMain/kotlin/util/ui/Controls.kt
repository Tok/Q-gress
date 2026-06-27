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
            "<div class=\"noticeTitle displayFont\">Q-Gress</div>" +
            "Desktop only — please open this on a desktop browser with a mouse and WebGL." +
            "</div>"
        document.body?.append(div)
    }

    private const val LEGEND_ID = "controlsLegend"

    // The single controls+shortcuts reference (mouse interactions + keyboard shortcuts). Shown bottom-left via
    // the "?" button AND from Menu → Shortcuts — one source of truth (the old separate menu popup is retired).
    private val MOUSE = listOf(
        "Left-drag" to "Pan",
        "Right-drag" to "Rotate + tilt",
        "Wheel" to "Zoom",
        "Click portal" to "Select",
        "Click ground" to "Build",
    )
    private val KEYS = listOf(
        "Space" to "Pause / resume",
        "Home" to "Recenter on the play area",
        "PageUp / PageDn" to "Zoom in / out",
        "Arrows / WASD" to "Pan",
        "Q-E / R-F" to "Rotate / pitch",
        "− / +" to "Slower / faster",
        ", / ." to "Building transparency",
        "Tab" to "Switch footer tab",
        "M" to "Mute / unmute",
        "C" to "Auto-camera",
        "Esc" to "Close panels",
    )

    /** A small "?" button toggles a controls+shortcuts legend (hidden by default so it can't cover the HUD). */
    fun addLegend() {
        val legend = document.createElement("div") as HTMLDivElement
        legend.id = LEGEND_ID
        legend.addClass("controlsLegend", "coda", "invisible")
        legend.innerHTML = legendHtml()
        val info = document.createElement("div") as HTMLDivElement
        info.addClass("controlsInfo", "coda")
        info.innerHTML = "?"
        info.onclick = { toggleLegend() }
        document.body?.append(legend)
        document.body?.append(info)
    }

    /** Reveal / hide / toggle the bottom-left legend (Menu → Shortcuts reveals it; Esc hides it; "?" toggles). */
    fun showLegend() = document.getElementById(LEGEND_ID)?.classList?.remove("invisible")
    fun hideLegend() = document.getElementById(LEGEND_ID)?.classList?.add("invisible")
    fun toggleLegend() = document.getElementById(LEGEND_ID)?.classList?.toggle("invisible")

    private fun legendHtml(): String {
        val sb = StringBuilder("<div class=\"shortcutsTitle\">Controls</div>")
        MOUSE.forEach { (key, desc) -> sb.append(row(key, desc)) }
        sb.append("<div class=\"shortcutsTitle\">Keyboard</div>")
        KEYS.forEach { (key, desc) -> sb.append(row(key, desc)) }
        return sb.toString()
    }

    private fun row(key: String, desc: String) =
        "<div class=\"shortcutsRow\"><span class=\"shortcutsKey\">$key</span><span>$desc</span></div>"
}
