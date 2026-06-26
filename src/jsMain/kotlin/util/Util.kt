package util

import World
import portal.Portal
import util.data.Pos

object Util {
    private fun findNearestPortals(pos: Pos): Set<Pair<Double, Portal>> =
        World.allPortals.map { it.location.distanceTo(pos) to it }.sortedBy { it.first }.toSet()

    fun findNearestPortal(pos: Pos): Portal? {
        val nearest = findNearestPortals(pos)
        return if (nearest.isNotEmpty()) nearest.first().second else null
    }

    // Pure numeric helpers live in the shared core ([MathUtil]); kept here as thin delegates so the many
    // existing Util.clip(...) call sites are unchanged.
    fun clip(value: Int, from: Int, to: Int): Int = MathUtil.clip(value, from, to)
    fun clipDouble(value: Double, from: Double, to: Double): Double = MathUtil.clipDouble(value, from, to)

    // The seedable mulberry32 PRNG + weighted picker now live in the shared core ([Rng]); these are thin
    // delegates so the ~150 Util.random()/seed()/select() call sites are unchanged. random() is the game's
    // ONLY randomness source — seeding it makes a whole world (and AI match) reproducible.
    fun seed(value: Int) = Rng.seed(value)
    fun currentSeed(): Int = Rng.currentSeed()
    fun freshSeed(): Int = util.freshSeed()
    fun random(): Double = Rng.random()
    fun randomBool() = Rng.randomBool()
    fun randomInt(max: Int) = Rng.randomInt(max)
    fun randomInt(min: Int, max: Int): Int = Rng.randomInt(min, max)
    fun <T> shuffle(items: Set<T>): Set<T> = Rng.shuffle(items)
    fun <T> shuffle(items: List<T>): List<T> = Rng.shuffle(items)
    fun <T> select(probabilityList: List<Pair<Double, T>>, default: T): T = Rng.select(probabilityList, default)

    fun generatePortalName(): String {
        val separator = if (random() < 0.3) "-" else " "
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
        return name + Util.select(values, "")
    }

    private fun generateName(minLength: Int, maxLength: Int): String {
        val length = minLength + randomInt(maxLength - minLength)
        val firstLetter = select(generateFirstSelection(), ' ')
        val name = firstLetter + IntRange(1, length).map { select(generateSelection(), ' ') }.joinToString("")
        val temp = name.substring(0, 1).uppercase() + name.substring(1).lowercase()
        return if (temp.endsWith('-')) temp.dropLast(1) else temp
    }

    /**
     * Relative letter frequency of english words
     * https://en.wikipedia.org/wiki/Letter_frequency#Relative_frequencies_of_letters_in_the_English_language
     */
    private fun generateSelection(): List<Pair<Double, Char>> = listOf(
        12.702 to 'E',
        9.056 to 'T',
        8.167 to 'A',
        7.507 to 'O',
        6.966 to 'I',
        6.749 to 'N',
        6.327 to 'S',
        6.094 to 'H',
        5.987 to 'R',
        4.253 to 'D',
        4.025 to 'L',
        2.782 to 'C',
        2.758 to 'U',
        2.406 to 'M',
        2.360 to 'W',
        2.228 to 'F',
        2.015 to 'G',
        1.974 to 'Y',
        1.929 to 'P',
        1.492 to 'B',
        0.978 to 'V',
        0.772 to 'K',
        0.153 to 'J',
        0.150 to 'X',
        0.095 to 'Q',
        0.074 to 'Z',
    )

    /**
     * Relative frequency of the first letter of an English word
     * https://en.wikipedia.org/wiki/Letter_frequency#Relative_frequencies_of_letters_in_the_English_language
     */
    private fun generateFirstSelection(): List<Pair<Double, Char>> = listOf(
        15.978 to 'T',
        11.682 to 'A',
        7.631 to 'O',
        7.294 to 'I',
        6.686 to 'S',
        5.497 to 'W',
        5.238 to 'C',
        4.434 to 'B',
        4.319 to 'P',
        4.200 to 'H',
        4.027 to 'F',
        3.826 to 'M',
        3.174 to 'D',
        2.826 to 'R',
        2.799 to 'E',
        2.415 to 'L',
        2.284 to 'N',
        1.642 to 'G',
        1.183 to 'U',
        0.824 to 'V',
        0.763 to 'Y',
        0.511 to 'J',
        0.456 to 'K',
        0.222 to 'Q',
        0.045 to 'X',
        0.045 to 'Z',
    )

    fun degToRad(degrees: Double): Double = MathUtil.degToRad(degrees)
    fun radToDeg(radians: Double): Double = MathUtil.radToDeg(radians)

    fun formatSeconds(absSeconds: Int): String = MathUtil.formatSeconds(absSeconds)
}
