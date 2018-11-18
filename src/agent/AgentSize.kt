package agent

import util.Util

data class AgentSize(val offset: Int) {
    companion object {
        private fun randomOffset(): Int {
            val rand = Util.random()
            return if (rand < 0.05) 1 else if (rand < 0.2) -1 else 0
        }
        fun createRandom(): AgentSize {
            return AgentSize(randomOffset())
        }
    }
}
