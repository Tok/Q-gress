package system

import World
import agent.NonFaction
import config.Config
import kotlinx.browser.document
import system.display.Scene3D
import system.ui.Bootstrap
import system.ui.LoadingOverlay

/**
 * Paced serial creation of the NPC (non-faction) crowd, split out of [World] (the imperative/DOM edge).
 * Uses `setTimeout` so each NPC renders as it lands — a serial drop-in — while reading the [World] grid
 * and appending to its roster.
 */
object NonFactionSpawner {
    fun spawn(callback: () -> Unit, count: Int) {
        document.defaultView?.setTimeout(fun() {
            if (count > 0) {
                val total = Config.maxFor()
                LoadingOverlay.building(LoadingOverlay.PCT_PEOPLE, 100, total - count + 1, total, "Creating people")
                World.allNonFaction.add(NonFaction.create(World.grid))
                if (Bootstrap.isRunningInBrowser()) {
                    Scene3D.sync() // render each NPC as created → serial drop-in
                    // (Flow fields flash once per portal when each portal's field is ready — not per NPC.)
                }
                spawn(callback, count - 1)
            } else {
                callback()
            }
        }, 0)
    }
}
