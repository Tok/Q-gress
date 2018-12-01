package portal

import config.Dim
import util.data.Coords

object XmMap {
    private val strayXm = mutableMapOf<Coords, XmHeap>()

    fun updateStrayXm() {
        val unCollectedXm = strayXm.filterNot { it.value.isCollected() }
        strayXm.clear()
        strayXm.putAll(unCollectedXm)
    }

    fun createStrayXm(location: Coords, isPortalDrop: Boolean) {
        if (strayXm.keys.none { it.distanceTo(location) < XmHeap.strayXmMinDistance(isPortalDrop) }) {
            strayXm[location] = XmHeap.create()
        }
    }

    fun findXmInRange(pos: Coords) =
            strayXm.filter { it.key.distanceTo(pos) <= Dim.agentXmCollectionRadius }

    fun draw() {
        strayXm.forEach { (pos, heap) -> heap.draw(pos) }
    }
}
