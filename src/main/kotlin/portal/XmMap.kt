package portal

import config.Dim
import util.data.Pos

object XmMap {
    private val strayXm = mutableMapOf<Pos, XmHeap>()

    fun updateStrayXm() {
        val unCollectedXm = strayXm.filterNot { it.value.isCollected() }
        strayXm.clear()
        strayXm.putAll(unCollectedXm)
    }

    fun createStrayXm(location: Pos, isPortalDrop: Boolean) {
        if (strayXm.keys.none { it.distanceTo(location) < XmHeap.strayXmMinDistance(isPortalDrop) }) {
            strayXm[location] = XmHeap.create()
        }
    }

    fun findXmInRange(pos: Pos) =
            strayXm.filter { it.key.distanceTo(pos) <= Dim.agentXmCollectionRadius }

    fun draw() {
        strayXm.forEach { (pos, heap) -> heap.draw(pos) }
    }
}
