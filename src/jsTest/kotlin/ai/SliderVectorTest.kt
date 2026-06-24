package ai

import agent.qvalue.QActions
import agent.qvalue.QDestinations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SliderVectorTest {

    @Test
    fun coversEveryActionAndDestinationSlot() {
        val ordered = QActions.values() + QDestinations.values()
        assertEquals(ordered.size, SliderVector.SIZE)
        // every QValue resolves to a distinct, in-range slot
        val indices = ordered.map { SliderVector.indexOf(it) }
        assertEquals(ordered.indices.toList(), indices, "slots follow the QActions-then-QDestinations order")
        assertEquals(ordered.size, indices.toSet().size, "no two QValues share a slot")
    }

    @Test
    fun uniformSetsEverySlot() {
        val v = SliderVector.uniform(0.4)
        QActions.values().forEach { assertEquals(0.4, v[it]) }
        QDestinations.values().forEach { assertEquals(0.4, v[it]) }
    }

    @Test
    fun decodeRoundTripsAndClamps() {
        val raw = DoubleArray(SliderVector.SIZE) { it.toDouble() / SliderVector.SIZE } // 0..<1, all valid
        assertTrue(SliderVector.decode(raw).toArray().contentEquals(raw))
        // out-of-range values are clamped to 0..1
        val wild = DoubleArray(SliderVector.SIZE) { if (it % 2 == 0) -3.0 else 5.0 }
        SliderVector.decode(wild).toArray().forEach { assertTrue(it in 0.0..1.0) }
    }

    @Test
    fun decodeRejectsWrongLength() {
        assertFailsWith<IllegalArgumentException> { SliderVector.decode(DoubleArray(SliderVector.SIZE - 1)) }
    }

    @Test
    fun withReplacesOneSlotImmutably() {
        val base = SliderVector.uniform(0.1)
        val tweaked = base.with(QActions.HACK, 0.9)
        assertEquals(0.9, tweaked[QActions.HACK])
        assertEquals(0.1, tweaked[QActions.ATTACK], "other slots untouched")
        assertEquals(0.1, base[QActions.HACK], "the original is unchanged")
        assertEquals(1.0, base.with(QActions.HACK, 2.0)[QActions.HACK], "the new value is clamped")
    }
}
