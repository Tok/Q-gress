// External JS bindings: parameters/declarations describe the JS contract and
// have no Kotlin body, so detekt's "unused" rules don't apply here.
@file:Suppress("UnusedParameter", "UnusedPrivateProperty")

package external

/**
 * Minimal external bindings for the global `maplibregl` namespace provided by
 * the MapLibre GL JS script loaded in index.html. MapLibre is API-compatible
 * with the (v1-era) Mapbox GL JS this project originally used, so only the
 * handful of members the app actually calls are declared here.
 *
 * Options are passed as `dynamic` JS objects so styles/centers can be built at
 * runtime (the old code used constant-string `js(...)` calls, which could not
 * take a dynamic style).
 */
@JsName("maplibregl")
external object MapLibre {
    class Map(options: dynamic) {
        fun setMinZoom(zoom: Int)
        fun setMaxZoom(zoom: Int)
        fun setZoom(zoom: Int)
        fun setPitch(pitch: Double)
        fun setBearing(bearing: Double)
        fun setCenter(center: dynamic)
        fun jumpTo(options: dynamic)
        fun flyTo(options: dynamic)
        fun panBy(offset: dynamic, options: dynamic = definedExternally)
        fun zoomTo(zoom: Double, options: dynamic = definedExternally)
        fun getZoom(): Double
        fun getBearing(): Double
        fun getPitch(): Double
        fun project(lngLat: dynamic): dynamic
        fun getCenter(): dynamic
        fun isMoving(): Boolean
        fun loaded(): Boolean
        fun addLayer(config: dynamic)
        fun getCanvas(): dynamic
        fun triggerRepaint()
        fun addControl(control: dynamic)
        fun on(type: String, listener: () -> Unit)
        fun once(type: String, listener: () -> Unit)
        fun setPaintProperty(layer: String, name: String, value: dynamic)
        fun remove()
    }

    class GeolocateControl(options: dynamic)
}
