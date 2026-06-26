package agent

/**
 * Pure anti-snowball / comeback formulas, extracted from [Balance] (which supplies the live [World] counts)
 * into the shared functional core (`commonMain`). All inputs are plain numbers, so there is no `World`/`Config`
 * coupling — JVM-unit-tested + Kover-covered.
 */
object BalanceMath {
    /** How far [mine] is behind [theirs] as a 0..1 share of the total (0 = even/ahead, 1 = fully shut out).
     *  The total≤0 guard yields 0 (even, not NaN) when neither side holds anything. */
    fun shareDeficit(mine: Int, theirs: Int): Double {
        val total = (mine + theirs).toDouble()
        return if (total <= 0.0) 0.0 else ((theirs - mine) / total).coerceIn(0.0, 1.0)
    }

    /** Recruit-chance multiplier: `< 1` for the LARGER roster ([mine] > [theirs]), `> 1` for the smaller, so
     *  team sizes self-balance. Clamped to [[min], [max]]. */
    fun recruitFactor(mine: Int, theirs: Int, min: Double, max: Double): Double = ((theirs + 1).toDouble() / (mine + 1)).coerceIn(min, max)

    /** Resonator-damage multiplier (`≥ 1`) for an attacker at the given [deficit] (0..1, the worse of the MU
     *  and portal deficits): `1 + comebackMax × comebackBonus × deficit²` — gentle when close, powerful when
     *  near-defeated, exactly `1.0` when even or ahead. */
    fun attackBoost(deficit: Double, comebackMax: Double, comebackBonus: Double): Double =
        1.0 + comebackMax * comebackBonus * deficit * deficit

    /** Diminishing-returns recruit chance: [baseChance] × the roster's remaining headroom (`1 − fillRatio`),
     *  so a near-full roster recruits at ~0 and an empty one at the full base. */
    fun recruitSuccessProbability(fillRatio: Double, baseChance: Double): Double = baseChance * (1.0 - fillRatio)
}
