package external

external class MapBox {
    fun setMinZoom(zoom: Int)
    fun setMaxZoom(zoom: Int)
    fun setZoom(zoom: Int)
    fun setCenter(latLng: JSON)
    fun addLayer(config: JSON)
    fun addControl(control: String)
    fun on(actionName: String, callback: Function<Unit>)
    fun getContext(type: String)
    fun setPaintProperty(layer: String, type: String, color: String)
}
