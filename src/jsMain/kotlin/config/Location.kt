package config

import util.Rng
import kotlin.js.Json

/**
 * One real-world place the sim can run at. The catalogue is **externalized** to
 * `resources/locations.json` (edit it freely — no Kotlin build needed) and loaded at startup via
 * [Locations.load]; this keeps location data out of code and is the seam for future scenario sharing.
 * `title = true` marks a place eligible as a title-screen fallback (when the player's home isn't known).
 */
data class Location(val name: String, val displayName: String, val lng: Double, val lat: Double, val title: Boolean) {
    fun toJSONString() = "[$lng,$lat]"
    fun toJSON(): Json = JSON.parse(toJSONString())
}

/** The runtime location catalogue, loaded from `locations.json` (falls back to [DEFAULT] only). */
object Locations {
    // Hardcoded default: needed synchronously (before the JSON loads) and as the fetch-failure fallback.
    val DEFAULT = Location("RED_SQUARE", "Red Square, St. Gallen, Switzerland", 9.37327, 47.42214, true)

    private var all: List<Location> = listOf(DEFAULT)

    fun all(): List<Location> = all
    fun random(): Location = Rng.shuffle(all)[0]
    fun byName(name: String): Location? = all.firstOrNull { it.name == name }

    /** The catalogue location at ([lng], [lat]) within ~50 m, or null (a custom/unlisted spot). */
    fun byCoords(lng: Double, lat: Double): Location? {
        val eps = 0.0005 // ≈ 50 m
        return all.firstOrNull { kotlin.math.abs(it.lng - lng) < eps && kotlin.math.abs(it.lat - lat) < eps }
    }

    /** A random title-eligible location (the curated showpiece set); [DEFAULT] if none are flagged. */
    fun randomTitle(): Location {
        val eligible = all.filter { it.title }.ifEmpty { listOf(DEFAULT) }
        return Rng.shuffle(eligible)[0]
    }

    /** Parse a `locations.json` array into [Location]s (pure — unit-tested; skips malformed entries). */
    fun parse(json: String): List<Location> {
        val arr = JSON.parse<Array<dynamic>>(json)
        return arr.mapNotNull { e ->
            val name = e.name as? String ?: return@mapNotNull null
            val displayName = e.displayName as? String ?: return@mapNotNull null
            val lng = (e.lng as? Number)?.toDouble() ?: return@mapNotNull null
            val lat = (e.lat as? Number)?.toDouble() ?: return@mapNotNull null
            Location(name, displayName, lng, lat, (e.title as? Boolean) ?: false)
        }
    }

    /** Replace the catalogue with [list] (used by [load] and tests). Keeps [DEFAULT] if [list] is empty. */
    fun setAll(list: List<Location>) {
        all = list.ifEmpty { listOf(DEFAULT) }
    }
}
