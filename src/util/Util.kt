package util

import World
import portal.Portal
import util.data.Coords
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object Util {
    private fun findNearestPortals(coords: Coords): Set<Pair<Double, Portal>> {
        return World.allPortals.map { it.location.distanceTo(coords) to it }.sortedBy { it.first }.toSet()
    }

    fun findNearestPortal(coords: Coords): Portal? {
        val nearest = findNearestPortals(coords)
        return if (nearest.isNotEmpty()) nearest.first().second else null
    }

    fun clip(value: Int, from: Int, to: Int): Int = max(from, min(to, value))
    fun clipDouble(value: Double, from: Double, to: Double): Double = max(from, min(to, value))

    fun random(): Double = js("Math.random();") as Double //native JS replacement for deprecated kotlin.js.Math.random()

    fun randomBool() = random() <= 0.5
    private fun randomDouble(max: Double) = random() * max
    fun randomInt(max: Int) = randomInt(0, max)
    fun randomInt(min: Int, max: Int): Int {
        val list = IntRange(min, max).toList()
        return list[(random() * list.size).toInt()]
    }

    fun <T> shuffle(items: List<T>): List<T> {
        val result = mutableListOf<T>()
        result.addAll(items)
        for (i in 0 until result.size) {
            val pos = randomInt(result.size - 1)
            val temp: T = result[i]
            result[i] = result[pos]
            result[pos] = temp
        }
        return result.toList()
    }

    fun <T> select(probabilityList: List<Pair<Double, T>>, default: T): T {
        val list = probabilityList.filterNot { it.first <= 0.0 }
        if (list.isEmpty()) {
            return default
        }
        val total = list.sumByDouble { it.first }
        val rand = randomDouble(total)
        var accu = 0.0
        list.sortedBy { it.first }.forEach {
            accu += it.first
            check(it.first > 0.0)
            if (accu >= rand) {
                return it.second
            }
        }
        throw IllegalArgumentException("Invalid Q-values: $probabilityList")
    }

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
                0.01 to separator + "Museum"
        )
        return name + Util.select(values, "")
    }

    fun generateAgentName(): String {
        val name = generateName(3, 6)
        if (name.length <= 4 && random() < 0.5) {
            return name + random().toString().subSequence(2, 4)
        }
        if (random() < 0.2) {
            return name + random().toString().subSequence(2, 3)
        }
        return name
    }

    private fun generateName(minLength: Int, maxLength: Int): String {
        val length = minLength + randomInt(maxLength - minLength)
        val firstLetter = select(generateFirstSelection(), ' ')
        val name = firstLetter + IntRange(1, length).map { select(generateSelection(), ' ') }.joinToString("")
        val temp = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase()
        return if (temp.endsWith('-')) temp.dropLast(1) else temp
    }

    /**
     * Relative letter frequency of english words
     * https://en.wikipedia.org/wiki/Letter_frequency#Relative_frequencies_of_letters_in_the_English_language
     */
    private fun generateSelection(): List<Pair<Double, Char>> {
        return listOf(
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
                0.074 to 'Z'
        )
    }

    /**
     * Relative frequency of the first letter of an English word
     * https://en.wikipedia.org/wiki/Letter_frequency#Relative_frequencies_of_letters_in_the_English_language
     */
    private fun generateFirstSelection(): List<Pair<Double, Char>> {
        return listOf(
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
                0.045 to 'Z'
        )
    }

    fun degToRad(degrees: Double): Double = degrees * PI / 180
    fun radToDeg(radians: Double): Double = radians * 180 / PI

    private fun fixTime(v: Int): String = if (v.toString().length <= 1) v.toString().padStart(2, '0') else v.toString()
    fun formatSeconds(absSeconds: Int): String {
        val seconds: Int = absSeconds % 60
        val minutes: Int = floor(absSeconds / 60.0).toInt() % 60
        val hours: Int = floor(absSeconds / 3600.0).toInt()
        return fixTime(hours) + ":" + fixTime(minutes) + ":" + fixTime(seconds)
    }
}
