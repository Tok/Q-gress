package util.ui

import agent.Faction
import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import system.display.Scene3D
import util.SoundUtil
import util.data.Pos

/**
 * Demo harness (hash-routed, separate from the game): a menu of demo scenes for triggering and
 * tuning effects. `#demo` shows the menu; `#demo/<scene>` runs a scene (the 3D bootstrap is in
 * HtmlUtil.loadDemoScene). Add a scene by listing it in [SCENES] + handling it in [showControls].
 */
object Demo {
    const val MENU = "menu"
    private const val NEUTRAL = "#bbbbbb"
    private const val PANEL_ID = "demoPanel"

    // route → display title. Add future scenes here (building styles, field shaders, NPC tuning…).
    private val SCENES = listOf(
        "portal-shatter" to "Portal Shatter",
        "xmp" to "XMP Effects",
    )

    private var shardColor = NEUTRAL

    /** The demo route for a location hash, or null for the normal game. */
    fun route(hash: String): String? = when (hash.removePrefix("#").removeSuffix("/")) {
        "demo" -> MENU
        "demo/portal-shatter" -> "portal-shatter"
        "demo/xmp" -> "xmp"
        else -> null
    }

    fun showMenu() {
        val menu = document.createElement("div") as HTMLDivElement
        menu.addClass("demoMenu", "coda")
        menu.append(titleEl("Demo Scenes"))
        SCENES.forEach { (route, title) -> menu.append(link("#demo/$route", title, "demoLink amarillo")) }
        menu.append(link("#", "← Back to game", "demoBack"))
        document.body?.append(menu)
    }

    fun showControls(scene: String, center: Pos) {
        val panel = document.createElement("div") as HTMLDivElement
        panel.id = PANEL_ID
        panel.addClass("demoPanel", "coda")
        when (scene) {
            "portal-shatter" -> buildShatterControls(panel, center)
            "xmp" -> buildXmpControls(panel, center)
        }
        panel.append(link("#demo", "≡ Menu", "demoBack"))
        document.body?.append(panel)
    }

    private fun buildShatterControls(panel: HTMLDivElement, center: Pos) {
        panel.append(titleEl("Portal Shatter"))
        panel.append(
            button("Shatter", "demoButton") {
                SoundUtil.enableAudio()
                Scene3D.shatterPortal(center, shardColor)
                SoundUtil.playGlassShatterSound(center, 0.4, 0.9)
            },
        )
        panel.append(button("ENL", "demoButton enl") { shardColor = Faction.ENL.color })
        panel.append(button("RES", "demoButton res") { shardColor = Faction.RES.color })
        panel.append(button("Neutral", "demoButton") { shardColor = NEUTRAL })
    }

    private fun buildXmpControls(panel: HTMLDivElement, center: Pos) {
        panel.append(titleEl("XMP Effects"))
        for (level in 1..8) {
            panel.append(
                button("L$level", "demoButton") {
                    SoundUtil.enableAudio()
                    Scene3D.playXmpBurst(center, level)
                },
            )
        }
    }

    private fun titleEl(text: String): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
        div.addClass("demoPanelTitle", "amarillo")
        div.innerHTML = text
        return div
    }

    private fun button(label: String, cssClass: String, onClick: () -> Unit): HTMLButtonElement {
        val element = document.createElement("button") as HTMLButtonElement
        element.className = cssClass
        element.innerHTML = label
        element.onclick = {
            onClick()
            null
        }
        return element
    }

    private fun link(href: String, label: String, cssClass: String): HTMLAnchorElement {
        val element = document.createElement("a") as HTMLAnchorElement
        element.href = href
        element.className = cssClass
        element.innerHTML = label
        return element
    }
}
