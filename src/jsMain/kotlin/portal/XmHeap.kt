package portal

import util.Util

data class XmHeap(private val cores: Triple<Int, Int, Int>, private var isCollected: Boolean = false) {
    val xm = cores.first + cores.second + cores.third
    fun isCollected() = isCollected
    fun collect() {
        isCollected = true
    }

    companion object {
        fun strayXmMinDistance(isPortalDrop: Boolean) = if (isPortalDrop) 13 else 21

        private const val minCapacity = 35
        private const val maxCapacity = 100
        const val capacity = maxCapacity - minCapacity
        private fun createCore(): Int = Util.randomInt(minCapacity, maxCapacity)
        private fun createCores() = Triple(createCore(), createCore(), createCore())
        fun create() = XmHeap(createCores())
    }
}
