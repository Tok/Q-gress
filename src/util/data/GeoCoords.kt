package util.data

import kotlin.js.Json
import kotlin.math.abs
import kotlin.math.sqrt

data class GeoCoords(val lng: Double, val lat: Double) {
    fun lngDiff(other: GeoCoords) = lng - other.lng //Y
    fun latDiff(other: GeoCoords) = lat - other.lat //X
    fun distanceTo(other: GeoCoords): Double {
        val lngPow = lngDiff(other) * lngDiff(other)
        val latPow = latDiff(other) * latDiff(other)
        return abs(sqrt(lngPow+ latPow))
    }
    fun toJson(): Json = JSON.parse("""[$lng,$lat]""")
    override fun toString() = "Geo-$lng:$lat"
}
