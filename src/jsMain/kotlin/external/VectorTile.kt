// External bindings for the `pbf` (Protocol-Buffers reader) + `@mapbox/vector-tile` (MVT decoder)
// npm modules, bundled by webpack — the same libraries MapLibre uses internally. We fetch the
// OpenFreeMap `.pbf` building tiles ourselves and decode them here, because MapLibre's public query
// APIs only ever expose a fraction of the footprints it actually paints. The decoded
// VectorTileFeature exposes `toGeoJSON(x, y, z)` (reached via dynamic at the call site), which returns
// a lng/lat GeoJSON Feature with the openmaptiles `render_height`/`render_min_height` properties
// intact — exactly the shape `OwnBuildings`/`Scene3D.buildBuildingColliders` already consume.
@file:Suppress("UnusedParameter", "UnusedPrivateProperty")

package external

/** `new Pbf(Uint8Array)` — the low-level protobuf reader the MVT decoder reads through. */
@JsModule("pbf")
@JsNonModule
external class Pbf(buf: dynamic)

/** The `@mapbox/vector-tile` module: `new VectorTile(pbf).layers[name]` → layer (`.length`,
 *  `.feature(i)`); each feature has `.toGeoJSON(x, y, z)` + `.properties` (reached via dynamic). */
@JsModule("@mapbox/vector-tile")
@JsNonModule
external object VectorTileModule {
    class VectorTile(pbf: Pbf)
}
