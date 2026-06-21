package config

import util.Util
import kotlin.js.Json

enum class Location(val displayName: String, val lng: Double, val lat: Double) {
    // Kept favourites.
    RED_SQUARE("Red Square St. Gallen", 9.37327, 47.42214),
    GOLLUMS("Gollums", 8.59520, 47.36200),
    EIFFEL_TOWER("Eiffel Tower", 2.29486, 48.85824),
    PRIME_TOWER("Prime Tower", 8.51831, 47.38673),
    GIZA_PLATEAU("Giza Plateau", 31.13200, 29.97800),
    GROUND_ZERO("Ground Zero", -74.01230, 40.71250),
    RED_SQUARE_MOSCOW("Red Square Moscow", 37.62050, 55.75400),

    // Iconic city centres / squares.
    TIMES_SQUARE("Times Square, New York", -73.98550, 40.75800),
    SHIBUYA("Shibuya Crossing, Tokyo", 139.70054, 35.65950),
    PICCADILLY("Piccadilly Circus, London", -0.13420, 51.50986),
    BRANDENBURG_GATE("Brandenburg Gate, Berlin", 13.37770, 52.51630),
    COLOSSEUM("Colosseum, Rome", 12.49220, 41.89020),
    DAM_SQUARE("Dam Square, Amsterdam", 4.89320, 52.37310),
    PLAZA_MAYOR("Plaza Mayor, Madrid", -3.70740, 40.41550),
    GRAND_BAZAAR("Grand Bazaar, Istanbul", 28.96800, 41.01060),

    // Bridges / waterways — interesting routing (chokepoints, islands, canals).
    TOWER_BRIDGE("Tower Bridge, London", -0.07540, 51.50550),
    BROOKLYN_BRIDGE("Brooklyn Bridge, New York", -73.99690, 40.70610),
    GOLDEN_GATE("Golden Gate Bridge, San Francisco", -122.47830, 37.81990),
    RIALTO("Rialto Bridge, Venice", 12.33580, 45.43800),
    CHARLES_BRIDGE("Charles Bridge, Prague", 14.41130, 50.08650),
    PONTE_VECCHIO("Ponte Vecchio, Florence", 11.25310, 43.76800),
    SYDNEY_HARBOUR("Sydney Harbour Bridge", 151.21080, -33.85230),
    MARINA_BAY("Marina Bay, Singapore", 103.86070, 1.28340),
    ;

    fun toJSONString() = "[$lng,$lat]"
    fun toJSON(): Json = JSON.parse(toJSONString())
    companion object {
        val DEFAULT = RED_SQUARE
        fun random(): Location = Util.shuffle(values().asList())[0]
    }
}
