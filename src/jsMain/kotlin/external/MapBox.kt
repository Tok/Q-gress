package external

import kotlin.js.Json

external class MapBox {
    fun setMinZoom(zoom: Int)
    fun setMaxZoom(zoom: Int)
    fun setZoom(zoom: Int)
    fun setCenter(latLng: Json)
    fun isMoving(): Boolean
    fun flyTo(options: Json)
    fun jumpTo(options: Json)
    fun getCenter(): Json
    fun loaded(): Boolean
    fun addLayer(config: Json)
    fun addControl(control: String)
    fun on(actionName: String, callback: Function<Unit>)
    fun getContext(type: String)
    fun setPaintProperty(layer: String, type: String, color: String)
}
