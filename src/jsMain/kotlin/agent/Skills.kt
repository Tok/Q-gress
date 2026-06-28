package agent

import config.Constants
import util.Rng

data class Skills(
    val speed: Double,
    val deployPrecision: Double,
    val glyphSkill: Double = 0.8,
    val reliability: Double,
    val recruiting: Double = 0.5,
) {
    fun inRangeSpeed() = speed / Constants.phi

    /** Recruiting-weight multiplier: maps the 0..1 [recruiting] aptitude to a 0.5×–1.5× factor that averages
     *  ~1.0 for uniform aptitude, so this per-agent lever makes some agents better recruiters without shifting
     *  the overall recruiting pace (the team-level anti-snowball balancing stays in [action.cond.Recruiter]). */
    fun recruitingFactor() = 0.5 + recruiting

    companion object {
        // Doubled (was 1.5) so agents + NPCs cross the bigger km²-sized maps in reasonable time → more action,
        // less walking. Safe to scale: the arrival threshold is [inRangeSpeed] (speed/φ), so it tracks the step
        // size; the fixed deploy/attack ranges (34 / ≥8 px) still dwarf a single doubled step (~5 px). NPC speed
        // is [randomNpcSpeed] = this ÷ φ, so it doubles too.
        private const val minSpeed = 3.0
        private const val maxSpeed = minSpeed * Constants.phi
        fun createRandom() = Skills(randomSpeed(), deployPrecision(), randomGlyphSkill(), randomReliability(), randomRecruiting())
        private fun randomSpeed() = (Rng.random() * (maxSpeed - minSpeed)) + minSpeed // distance per tick
        private fun deployPrecision() = 0.7 + (Rng.random() * 0.3) // 0.7 to 1.0
        private fun randomGlyphSkill() = 0.5 + (Rng.random() * 0.5) // 0.5 to 1.0
        private fun randomReliability() = 0.5 + Rng.random() / 2.0
        private fun randomRecruiting() = Rng.random() // 0..1, avg 0.5 → recruitingFactor averages ~1.0×
        fun randomNpcSpeed() = randomSpeed() / Constants.phi
    }
}
