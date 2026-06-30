package util

/**
 * The pure portal-name generator: letter-frequency "gibberish" + an optional place-type suffix, all driven by
 * [Rng]. Split out of the (JS-bound) `util.PortalNames` map-data lookup so the name fallback is part of the
 * functional core — `Portal.create` uses a real map-derived name when one is available (via the naming seam)
 * and falls back to [generate] otherwise.
 */
object PortalNameGen {
    /** A generated portal name (letter-frequency gibberish + an optional place-type suffix). */
    fun generate(): String {
        val separator = if (Rng.random() < 0.3) "-" else " "
        val name = generateName(3, 5)
        val values = listOf(
            1.00 to "",
            0.15 to separator + "Portal",
            0.05 to separator + "Square",
            0.10 to separator + "Street",
            0.07 to separator + "Fountain",
            0.08 to separator + "Park",
            0.03 to separator + "Station",
            0.02 to separator + "House",
            0.01 to separator + "Memorial",
            0.01 to separator + "Museum",
        )
        return name + Rng.select(values, "")
    }

    private fun generateName(minLength: Int, maxLength: Int): String {
        val length = minLength + Rng.randomInt(maxLength - minLength)
        val firstLetter = Rng.select(generateFirstSelection(), ' ')
        val name = firstLetter + IntRange(1, length).map { Rng.select(generateSelection(), ' ') }.joinToString("")
        val temp = name.substring(0, 1).uppercase() + name.substring(1).lowercase()
        return if (temp.endsWith('-')) temp.dropLast(1) else temp
    }

    /** Relative letter frequency of English words (Wikipedia: Letter frequency). */
    private fun generateSelection(): List<Pair<Double, Char>> = listOf(
        12.702 to 'E', 9.056 to 'T', 8.167 to 'A', 7.507 to 'O', 6.966 to 'I', 6.749 to 'N',
        6.327 to 'S', 6.094 to 'H', 5.987 to 'R', 4.253 to 'D', 4.025 to 'L', 2.782 to 'C',
        2.758 to 'U', 2.406 to 'M', 2.360 to 'W', 2.228 to 'F', 2.015 to 'G', 1.974 to 'Y',
        1.929 to 'P', 1.492 to 'B', 0.978 to 'V', 0.772 to 'K', 0.153 to 'J', 0.150 to 'X',
        0.095 to 'Q', 0.074 to 'Z',
    )

    /** Relative frequency of the FIRST letter of an English word. */
    private fun generateFirstSelection(): List<Pair<Double, Char>> = listOf(
        15.978 to 'T', 11.682 to 'A', 7.631 to 'O', 7.294 to 'I', 6.686 to 'S', 5.497 to 'W',
        5.238 to 'C', 4.434 to 'B', 4.319 to 'P', 4.200 to 'H', 4.027 to 'F', 3.826 to 'M',
        3.174 to 'D', 2.826 to 'R', 2.799 to 'E', 2.415 to 'L', 2.284 to 'N', 1.642 to 'G',
        1.183 to 'U', 0.824 to 'V', 0.763 to 'Y', 0.511 to 'J', 0.456 to 'K', 0.222 to 'Q',
        0.045 to 'X', 0.045 to 'Z',
    )
}
