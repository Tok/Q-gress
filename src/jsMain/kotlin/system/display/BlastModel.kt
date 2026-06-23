package system.display

import kotlin.math.sqrt

/**
 * The **one** XMP blast-energy model, shared by the gameplay shatter ([ShatterFx]) and the title
 * wordmark ([TitleWordmark]). One origin — the **mushroom-cloud centre, *above* the terrain** — and
 * one law: a piece is shoved **radially out from that 3D centre**, with energy ∝ XMP level and
 * **falling off with 3D distance** from the centre (so a piece's height matters, not just its
 * ground offset). Pure (vec3 in → vec3 out); callers add their own jitter / spawn velocity and,
 * where they want debris to *arc up*, lift the result (see [ShatterFx]).
 */
object BlastModel {
    private const val CLOUD_BASE_Z = 12.0 // cloud-centre height above ground at L1…
    private const val CLOUD_PER_LEVEL_Z = 4.0 // …rising with level (tracks the [XmpBurst] mushroom rise)

    /** Cloud-centre height above the terrain for an XMP of [level] (1..8) — the blast origin's z lift. */
    fun cloudHeight(level: Int) = CLOUD_BASE_Z + CLOUD_PER_LEVEL_Z * level.coerceIn(1, 8)

    /** Level → energy multiplier: L1 keeps [floor] of full energy (still throws pieces), L8 = 1.0. */
    fun levelGain(level: Int, floor: Double) = floor + (1.0 - floor) * (level.coerceIn(1, 8) / 8.0)

    /**
     * Outward impulse on a [piece] from a blast centred at [origin] (both 3D scene metres).
     * Direction = radial from the cloud centre (normalized `piece − origin`); magnitude =
     * [baseSpeed] · [levelGain]([level], [levelFloor]) · [refDist] / ([refDist] + dist). Returns
     * `(vx, vy, vz)`. Degenerate (piece exactly at the centre) → straight up at full magnitude.
     */
    fun blastImpulse(origin: DoubleArray, piece: DoubleArray, level: Int, baseSpeed: Double, refDist: Double, levelFloor: Double): DoubleArray {
        val dx = piece[0] - origin[0]
        val dy = piece[1] - origin[1]
        val dz = piece[2] - origin[2]
        val dist = sqrt(dx * dx + dy * dy + dz * dz)
        val mag = baseSpeed * levelGain(level, levelFloor) * refDist / (refDist + dist)
        if (dist < 1e-6) return doubleArrayOf(0.0, 0.0, mag)
        return doubleArrayOf(dx / dist * mag, dy / dist * mag, dz / dist * mag)
    }
}
