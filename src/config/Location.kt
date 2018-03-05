package config

import util.Util
import kotlin.js.Json

enum class Location(val displayName: String, val lng: Double, val lat: Double) {
    RED_SQUARE("Red Square St. Gallen", 9.37327, 47.42214),
    RED_SQUARE_MOSCOW("Red Square Moscow", 37.62050, 55.75400),
    CHLOSER_PLATZ("Chloster Platz St. Gallen", 9.37700, 47.42400),
    GOLLUMS("Gollums", 8.59520, 47.36200),
    BAD_RAGAZ("Bad Ragaz", 9.50032, 47.00247),
    ESCHER_WYSS("Escher Wyss", 8.52206, 47.39080),
    GIZA_PLATEAU("Giza Plateau", 31.13200, 29.97800),
    EIFFEL_TOWER("Eiffel Tower", 2.29486, 48.85824),
    PRIME_TOWER("Prime Tower", 8.51831, 47.38673),
    GROUND_ZERO("Ground Zero", -74.01230, 40.71250),
    PLATZSPITZ("Platzspitz", 8.53900, 47.38210);

    fun toJSONString() = "[$lng,$lat]"
    fun toJSON(): Json = JSON.parse(toJSONString())
    companion object {
        val DEFAULT = RED_SQUARE
        fun random(): Location = Util.shuffle(values().asList())[0]
    }
}
