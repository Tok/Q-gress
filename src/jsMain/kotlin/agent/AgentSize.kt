package agent

import util.Rng

data class AgentSize(val offset: Int) {
    companion object {
        private fun randomOffset(): Int {
            val rand = Rng.random()
            // return if (rand < 0.05) 1 else if (rand < 0.2) -1 else 0
            return if (rand < 0.03) 1 else 0
        }

        fun createRandom(): AgentSize = AgentSize(randomOffset())
    }
}
