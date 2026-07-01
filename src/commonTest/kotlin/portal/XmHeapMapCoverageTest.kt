package portal

import config.Dim
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The stray-XM data layer: an [XmHeap] (three cores summed to its [XmHeap.xm]; a one-way collected flag) and
 * the [XmMap] store (drop with a minimum-spacing guard, find within an agent's collection radius, purge the
 * collected on update, and a full clear).
 */
class XmHeapMapCoverageTest {

    @BeforeTest
    fun reset() {
        XmMap.clear()
        Rng.seed(21)
    }

    @AfterTest
    fun tidy() {
        XmMap.clear()
    }

    @Test
    fun xmHeapSumsItsCoresAndCollectsOnce() {
        val heap = XmHeap(Triple(10, 20, 30))
        assertEquals(60, heap.xm, "xm is the sum of the three cores")
        assertFalse(heap.isCollected(), "a fresh heap is uncollected")
        heap.collect()
        assertTrue(heap.isCollected(), "collect() flips the flag")
    }

    @Test
    fun xmHeapCompanionConstants() {
        assertEquals(13, XmHeap.strayXmMinDistance(isPortalDrop = true), "portal drops spread closer")
        assertEquals(21, XmHeap.strayXmMinDistance(isPortalDrop = false), "ambient drops spread further")
        assertEquals(65, XmHeap.capacity, "capacity = maxCapacity − minCapacity (100 − 35)")
    }

    @Test
    fun xmHeapCreateStaysWithinCoreBounds() {
        val heap = XmHeap.create()
        // Three cores, each in [35, 100) → total in [105, 300).
        assertTrue(heap.xm in 105 until 300, "a created heap's total lies within its three-core bounds, was ${heap.xm}")
    }

    @Test
    fun createStrayXmDropsThenBlocksTooCloseAndAllowsFarEnough() {
        XmMap.createStrayXm(Pos(500, 500), isPortalDrop = false)
        assertEquals(1, XmMap.all().size, "the first drop lands")
        // Within the 21px min distance for an ambient drop → suppressed.
        XmMap.createStrayXm(Pos(505, 500), isPortalDrop = false)
        assertEquals(1, XmMap.all().size, "a too-close second drop is skipped")
        // Well beyond the min distance → a second heap.
        XmMap.createStrayXm(Pos(500, 700), isPortalDrop = false)
        assertEquals(2, XmMap.all().size, "a far-enough drop is kept")
    }

    @Test
    fun findXmInRangeRespectsTheCollectionRadius() {
        XmMap.createStrayXm(Pos(1000, 1000), isPortalDrop = false)
        val onTop = XmMap.findXmInRange(Pos(1000, 1000))
        assertEquals(1, onTop.size, "a heap right under the agent is in range")
        val far = XmMap.findXmInRange(Pos(1000 + Dim.agentXmCollectionRadius.toInt() + 50, 1000))
        assertTrue(far.isEmpty(), "a heap beyond the collection radius is out of range")
    }

    @Test
    fun updateStrayXmPurgesCollectedHeapsAndClearWipesAll() {
        XmMap.createStrayXm(Pos(2000, 2000), isPortalDrop = false)
        XmMap.all().values.first().collect()
        XmMap.updateStrayXm()
        assertTrue(XmMap.all().isEmpty(), "update drops every collected heap")

        XmMap.createStrayXm(Pos(3000, 3000), isPortalDrop = false)
        assertEquals(1, XmMap.all().size)
        XmMap.clear()
        assertTrue(XmMap.all().isEmpty(), "clear wipes the store")
    }
}
