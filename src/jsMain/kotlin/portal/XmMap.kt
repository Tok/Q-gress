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
        val minDist = XmHeap.strayXmMinDistance(isPortalDrop)
        if (strayXm.keys.none { it.distanceTo2(location) < minDist * minDist }) { // squared compare, no sqrt
            strayXm[location] = XmHeap.create()
        }
    }

    // Collection check runs every tick per agent → squared distance vs the radius² (skips the sqrt).
    private val collectionRadius2 = Dim.agentXmCollectionRadius * Dim.agentXmCollectionRadius
    fun findXmInRange(pos: Pos) = strayXm.filter { it.key.distanceTo2(pos) <= collectionRadius2 }

    /** Drop all stray XM — for the headless match harness resetting state between matches (ai.SimRunner). */
    fun clear() = strayXm.clear()

    /** All uncollected stray-XM heaps (read-only) — for 3D rendering. */
    fun all(): Map<Pos, XmHeap> = strayXm
}
