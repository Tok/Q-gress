package util

import util.data.Pos

/**
 * The headless [PortalNamer] sink: no map data, so every portal falls back to the [PortalNameGen] generator.
 * The default outside the browser (Node tests / the `SimRunner`). Touches no `Scene3D`/`window`/MapLibre.
 */
object NoOpPortalNamer : PortalNamer {
    override fun nameFor(location: Pos): String? = null
}
