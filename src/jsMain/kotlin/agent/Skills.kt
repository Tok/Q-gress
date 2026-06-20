package agent

import config.Constants
import util.Util

data class Skills(val speed: Double, val deployPrecision: Double, val glyphSkill: Double = 0.8, val reliability: Double) {
    fun inRangeSpeed() = speed / Constants.phi

    companion object {
        private const val minSpeed = 1.5
        private const val maxSpeed = minSpeed * Constants.phi
        fun createRandom() = Skills(randomSpeed(), deployPrecision(), randomGlyphSkill(), randomReliability())
        private fun randomSpeed() = (Util.random() * (maxSpeed - minSpeed)) + minSpeed //distance per tick
        private fun deployPrecision() = 0.7 + (Util.random() * 0.3) //0.7 to 1.0
        private fun randomGlyphSkill() = 0.5 + (Util.random() * 0.5) //0.5 to 1.0
        private fun randomReliability() = 0.5 + Util.random() / 2.0
        fun randomNpcSpeed() = randomSpeed() / Constants.phi
    }
}
