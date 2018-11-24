package agent

import config.Constants
import util.Util

data class Skills(val speed: Float, val deployPrecision: Double, val glyphSkill: Double = 0.8, val reliability: Double) {
    fun inRangeSpeed() = speed / Constants.phi
    companion object {
        const val minSpeed = 2F
        const val maxSpeed = 3F
        fun createRandom() = Skills(randomSpeed(), deployPrecision(), randomGlyphSkill(), randomReliability())
        private fun randomSpeed() = (Util.random().toFloat() * (maxSpeed - minSpeed)) + minSpeed //distance per tick
        private fun deployPrecision() = 0.7 + (Util.random() * 0.3) //0.7 to 1.0
        private fun randomGlyphSkill() = 0.5 + (Util.random() * 0.5) //0.5 to 1.0
        private fun randomReliability() = 0.5 + Util.random() / 2
    }
}
