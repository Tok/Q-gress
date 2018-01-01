package agent

import util.Util

data class Skills(val speed: Float, val deployPrecision: Double, val glyphSkill: Double = 0.8, val reliability: Double) {
    companion object {
        val maxSpeed = 8F
        val minSpeed = 5F
        fun createRandom() = Skills(randomSpeed(), deployPrecision(), randomGlyphSkill(), randomReliability())
        private fun randomSpeed() = (Util.random().toFloat() * (maxSpeed - minSpeed)) + minSpeed //distance per tick
        private fun deployPrecision() = 0.2 + (Util.random() * 0.8) //0.2 to 1.0
        private fun randomGlyphSkill() = Util.random() //0.0 to 1.0
        private fun randomReliability() = 0.5 + Util.random() / 2
    }
}
