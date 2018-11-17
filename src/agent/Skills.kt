package agent

import config.Constants
import util.Util

data class Skills(val speed: Float, val deployPrecision: Double, val glyphSkill: Double = 0.8, val reliability: Double) {
    fun inRangeSpeed() = speed / Constants.phi
    companion object {
        private const val maxSpeed = 5F
        private const val minSpeed = 2F
        fun createRandom() = Skills(randomSpeed(), deployPrecision(), randomGlyphSkill(), randomReliability())
        private fun randomSpeed() = (Util.random().toFloat() * (maxSpeed - minSpeed)) + minSpeed //distance per tick
        private fun deployPrecision() = 0.2 + (Util.random() * 0.8) //0.2 to 1.0
        private fun randomGlyphSkill() = Util.random() //0.0 to 1.0
        private fun randomReliability() = 0.5 + Util.random() / 2
    }
}
