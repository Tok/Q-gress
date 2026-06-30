package util

import util.data.Pos

/**
 * The live, in-browser [PortalNamer] sink: forwards to [PortalNames.nameFor], which reads the nearest named
 * POI/street out of the MapLibre vector tiles (projected via `Scene3D`). Mirrors [system.effect.BrowserEffects]
 * / [system.audio.BrowserAudio].
 */
object MapPortalNamer : PortalNamer {
    override fun nameFor(location: Pos): String? = PortalNames.nameFor(location)
}
