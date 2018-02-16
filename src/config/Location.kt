package config

enum class Location(val displayName: String, val lng: Double, val lat: Double) {
    RED_SQUARE("Red Square", 9.373274, 47.422139),
    CHLOSER_PLATZ("Chloster Platz", 9.3770000, 47.4240000),
    GOLLUMS("Gollums", 8.5952000, 47.3620000),
    BAD_RAGAZ("Bad Ragaz", 9.500324, 47.0024734),
    ESCHER_WYSS("Escher Wyss", 8.5220562, 47.3907937),
    GIZA_PLATEAU("Giza Plateau", 31.1320000, 29.9780000),
    EIFFEL_TOWER("Eiffel Tower", 2.2948595, 48.858243),
    PRIME_TOWER("Prime Tower", 8.5183064, 47.3867261),
    GROUND_ZERO("Ground Zero", -74.0123000, 40.7125000);

    fun toJSONString() = "[$lng,$lat]"
    fun toJSON(): JSON = JSON.parse(toJSONString())
    companion object {
        val DEFAULT = RED_SQUARE
    }
}
