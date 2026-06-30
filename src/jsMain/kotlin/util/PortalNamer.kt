package util

import util.data.Pos

/**
 * The imperative-shell boundary for **map-derived portal naming**: turning a portal's location into a real
 * nearby place/street name pulled from the live MapLibre vector tiles (via [PortalNames], which needs
 * `Scene3D`/`window`/`dynamic`). Logic calls `Names.sink.nameFor(location)` and falls back to the pure
 * [PortalNameGen] when it returns null (headless, or no named feature nearby). Mirrors the
 * [system.effect.Effects] / [system.audio.Audio] / [system.grid.FieldFlow] seams; the PLAN Phase-B
 * prerequisite for lifting `Portal` into `commonMain`.
 */
interface PortalNamer {
    /** A real map-derived name near [location], or null to fall back to the generator. */
    fun nameFor(location: Pos): String?
}
