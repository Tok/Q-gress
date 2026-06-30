package util

import system.ui.Bootstrap

/**
 * The live [PortalNamer] sink — the install point for map-derived portal naming. Self-selects
 * [MapPortalNamer] in the browser / [NoOpPortalNamer] headless at first reference. A harness installs a fake
 * via [install]; [reset] restores the default. Mirrors [system.effect.Fx] / [system.audio.Snd] /
 * [system.grid.Nav].
 */
object Names {
    private fun default(): PortalNamer = if (Bootstrap.isNotRunningInBrowser()) NoOpPortalNamer else MapPortalNamer

    var sink: PortalNamer = default()
        private set

    fun install(namer: PortalNamer) {
        sink = namer
    }

    fun reset() {
        sink = default()
    }
}
