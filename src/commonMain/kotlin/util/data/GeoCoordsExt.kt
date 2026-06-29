package util.data

import kotlin.js.Json

/** JS-shell serialization of [GeoCoords] to a MapLibre `[lng, lat]` JSON array — kept out of the pure core. */
fun GeoCoords.toJson(): Json = JSON.parse("""[$lng,$lat]""")
