package util

/**
 * A **seedable mulberry32 PRNG stream** plus the weighted-random [select] that drives AI choices. The game runs
 * TWO independent streams built on this class so that one can't perturb the other:
 *  - [Rng] — agents, portals, drops, world-gen: the deterministic training/eval sequence.
 *  - [NpcRng] — ambient NPC movement/destination choices.
 * Keeping ambient NPCs off the [Rng] stream means NPC tuning never shifts the agent-driven sequence, so headless
 * matches stay reproducible (and JVM↔JS-stable) across NPC-behaviour changes. (Kotlin Int arithmetic wraps 2^32.)
 */
class RngStream {
    private var rngState = 0
    private var theSeed = 0
    private var seeded = false

    /** Seed the stream (resets the sequence). */
    fun seed(value: Int) {
        theSeed = value
        rngState = value
        seeded = true
    }

    /** The seed driving this stream (for sharing). */
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

    companion object {
        private const val MULBERRY_INC = 0x6D2B79F5
        private const val UINT32 = 4294967296.0
    }
}

/**
 * The primary game RNG — agents, portals, drops, world-gen. Seeding it also reseeds [NpcRng] (with a derived
 * seed) so a given world seed stays fully reproducible while the two streams remain independent. [Util] delegates
 * here, so the ~150 `Rng.random()` / `Rng.select()` call sites are unchanged.
 */
object Rng {
    private val stream = RngStream()

    /** Seed the game RNG (resets the sequence + the derived NPC stream). Call before world generation. */
    fun seed(value: Int) {
        stream.seed(value)
        NpcRng.seedFrom(value)
    }

    fun currentSeed(): Int = stream.currentSeed()
    fun random(): Double = stream.random()
    fun randomBool(): Boolean = stream.randomBool()
    fun randomInt(max: Int): Int = stream.randomInt(max)
    fun randomInt(min: Int, max: Int): Int = stream.randomInt(min, max)
    fun <T> shuffle(items: Set<T>): Set<T> = stream.shuffle(items)
    fun <T> shuffle(items: List<T>): List<T> = stream.shuffle(items)
    fun <T> select(probabilityList: List<Pair<Double, T>>, default: T): T = stream.select(probabilityList, default)
}

/**
 * The ambient-NPC RNG stream — [agent.NonFaction] movement + destination choices. Independent of [Rng] so NPC
 * tuning never shifts the agent-driven sequence. Reseeded (with a derived value) whenever [Rng] is seeded, so a
 * world seed still fully reproduces the NPCs.
 */
object NpcRng {
    private const val DERIVE = 1013904223 // decorrelate the NPC stream from the game seed (an LCG increment)
    private val stream = RngStream()

    fun seedFrom(gameSeed: Int) = stream.seed(gameSeed xor DERIVE)
    fun random(): Double = stream.random()
    fun randomInt(max: Int): Int = stream.randomInt(max)
    fun randomInt(min: Int, max: Int): Int = stream.randomInt(min, max)
}

/** A fresh random 32-bit seed (used when no seed is supplied) — the one platform-specific bit of [RngStream]. */
expect fun freshSeed(): Int
