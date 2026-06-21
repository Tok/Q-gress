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

    // More city squares / centres.
    TIANANMEN("Tiananmen Square, Beijing", 116.39750, 39.90870),
    CONCORDE("Place de la Concorde, Paris", 2.32160, 48.86550),
    ST_PETERS("St. Peter's Square, Vatican City", 12.45390, 41.90220),
    TRAFALGAR("Trafalgar Square, London", -0.12760, 51.50800),
    SAN_MARCO("Piazza San Marco, Venice", 12.33890, 45.43430),
    GRAND_PLACE("Grand Place, Brussels", 4.35240, 50.84670),
    FEDERATION_SQUARE("Federation Square, Melbourne", 144.96940, -37.81780),
    TAHRIR("Tahrir Square, Cairo", 31.23390, 30.04440),
    ZOCALO("Zócalo, Mexico City", -99.13320, 19.43260),
    CATALUNYA("Plaça de Catalunya, Barcelona", 2.16860, 41.38740),
    DUOMO_FLORENCE("Piazza del Duomo, Florence", 11.25540, 43.77310),
    NAVONA("Piazza Navona, Rome", 12.47310, 41.89920),
    CHAMPS_ELYSEES("Champs-Élysées, Paris", 2.29500, 48.87380),
    LA_RAMBLA("La Rambla, Barcelona", 2.17340, 41.38090),
    STROGET("Strøget, Copenhagen", 12.56830, 55.67610),
    PEOPLES_SQUARE("People's Square, Shanghai", 121.47370, 31.23040),
    PLAZA_DE_MAYO("Plaza de Mayo, Buenos Aires", -58.37310, -34.60370),
    SYNTAGMA("Syntagma Square, Athens", 23.72750, 37.97550),
    NATHAN_PHILLIPS("Nathan Phillips Square, Toronto", -79.38720, 43.65320),
    RYNEK_KRAKOW("Old Town Square, Kraków", 19.93680, 50.06140),
    SENATE_SQUARE("Senate Square, Helsinki", 24.95270, 60.16950),
    JEMAA_EL_FNAA("Jemaa el-Fnaa, Marrakech", -7.98910, 31.62580),
    STORTORGET("Stortorget, Stockholm", 18.07160, 59.32580),
    GENDARMENMARKT("Gendarmenmarkt, Berlin", 13.39360, 52.51360),
    WENCESLAS("Wenceslas Square, Prague", 14.42650, 50.08120),
    DUOMO_MILAN("Duomo Square, Milan", 9.19170, 45.46410),
    PLACE_STANISLAS("Place Stanislas, Nancy", 6.18440, 48.69310),
    CAMPO_SIENA("Piazza del Campo, Siena", 11.33200, 43.31880),
    COMERCIO_LISBON("Praça do Comércio, Lisbon", -9.13660, 38.70690),

    // More bridges / waterways.
    PONT_ALEXANDRE("Pont Alexandre III, Paris", 2.31280, 48.86380),
    PONT_DES_ARTS("Pont des Arts, Paris", 2.33730, 48.85850),
    PONT_NEUF("Pont Neuf, Paris", 2.34060, 48.85810),
    PONT_ALMA("Pont de l'Alma, Paris", 2.30220, 48.86440),
    MILLAU("Millau Viaduct, France", 3.02110, 44.08330),
    LONDON_BRIDGE("London Bridge, London", -0.08660, 51.50780),
    WESTMINSTER_BRIDGE("Westminster Bridge, London", -0.12140, 51.50100),
    MILLENNIUM_BRIDGE("Millennium Bridge, London", -0.09840, 51.50900),
    AKASHI("Akashi Kaikyo Bridge, Kobe", 135.01950, 34.63330),
    ORESUND("Øresund Bridge, Denmark–Sweden", 12.82970, 55.57500),
    ;

    fun toJSONString() = "[$lng,$lat]"
    fun toJSON(): Json = JSON.parse(toJSONString())
    companion object {
        val DEFAULT = RED_SQUARE
        fun random(): Location = Util.shuffle(values().asList())[0]
    }
}
