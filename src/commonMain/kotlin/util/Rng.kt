package util

/**
 * The game's single randomness source — a **seedable mulberry32 PRNG** plus the weighted-random [select] that
 * drives every AI choice. Lives in the shared functional core (`commonMain`) so a seeded world/match is
 * reproducible on every platform and the picker is JVM-unit-tested + Kover-covered. The one platform bit — a
 * fresh 32-bit seed when none is set — is [freshSeed] (expect/actual). [Util] delegates here, so the ~150
 * `Util.random()` / `Util.select()` call sites are unchanged. (Kotlin Int arithmetic wraps mod 2^32.)
 */
object Rng {
    private const val MULBERRY_INC = 0x6D2B79F5
    private const val UINT32 = 4294967296.0

    private var rngState = 0
    private var theSeed = 0
    private var seeded = false

    /** Seed the RNG (resets the sequence). Call before world generation to reproduce a world. */
    fun seed(value: Int) {
        theSeed = value
        rngState = value
        seeded = true
    }

    /** The seed driving the current world (for sharing). */
    fun currentSeed(): Int = theSeed

    fun random(): Double {
        if (!seeded) seed(freshSeed())
        val a = rngState + MULBERRY_INC
        rngState = a
        var t = (a xor (a ushr 15)) * (1 or a)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        return (t xor (t ushr 14)).toUInt().toDouble() / UINT32
    }

    fun randomBool() = random() <= 0.5

    private fun randomDouble(max: Double) = random() * max
    fun randomInt(max: Int) = randomInt(0, max)
    fun randomInt(min: Int, max: Int): Int {
        val list = IntRange(min, max).toList()
        return list[(random() * list.size).toInt()]
    }

    fun <T> shuffle(items: Set<T>): Set<T> = shuffle(items.toList()).toSet()
    fun <T> shuffle(items: List<T>): List<T> {
        val result = items.toMutableList()
        for (i in result.indices) {
            val pos = randomInt(result.size - 1)
            val temp: T = result[i]
            result[i] = result[pos]
            result[pos] = temp
        }
        return result.toList()
    }

    /** Weighted-random pick: each entry's chance is proportional to its (positive) weight; non-positive
     *  weights are ineligible and [default] is returned when none are eligible. */
    fun <T> select(probabilityList: List<Pair<Double, T>>, default: T): T {
        val list = probabilityList.filterNot { it.first <= 0.0 }
        if (list.isEmpty()) {
            return default
        }
        val total = list.sumOf { it.first }
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
}

/** A fresh random 32-bit seed (used when no seed is supplied) — the one platform-specific bit of [Rng]. */
expect fun freshSeed(): Int
