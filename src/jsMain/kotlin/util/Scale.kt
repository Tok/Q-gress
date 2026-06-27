package util

import kotlin.math.pow

/**
 * The shared musical scale the sim plays in. The 8 portal/XMP levels map to 8 notes (C2 root;
 * **level 8 = the lowest**), so a portal's level is audible across every level-keyed sound. The key
 * flips **major when the player's faction leads, minor when behind** (`setLeading`, driven by the live
 * MU each tick) — the soundtrack brightens/darkens with the score. Split out of [SoundUtil] (size).
 */
object Scale {
    private val MINOR = intArrayOf(0, 2, 3, 5, 7, 8, 10, 12) // C natural minor (behind)
    private val MAJOR = intArrayOf(0, 2, 4, 5, 7, 9, 11, 12) // C major (leading)
    private const val ROOT_HZ = 65.41 // C2 — the lowest note (level 8)
    private var leading = false

    /** Brighten (major) when the player's faction leads, else minor. Cheap; call freely. */
    fun setLeading(ahead: Boolean) {
        leading = ahead
    }

    /** Whether the scale is currently major (the player's faction leads) — for the AUDIO tab's read-only key display. */
    fun isLeading() = leading

    /** Frequency (Hz) of [level]'s note (level 8 = root); [octaveUp] transposes it up by octaves. */
    fun noteFor(level: Int, octaveUp: Int = 0): Double {
        val scale = if (leading) MAJOR else MINOR
        val semis = scale[8 - level.coerceIn(1, 8)] + octaveUp * 12
        return ROOT_HZ * 2.0.pow(semis / 12.0)
    }
}
