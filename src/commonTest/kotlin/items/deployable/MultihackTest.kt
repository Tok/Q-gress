package items.deployable

import Factory
import items.types.MultihackType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The multi-hack stacking math ([Multihack.additionalHacks]): each deployed multi-hack raises a portal's
 * hacks-before-burnout limit, but only the rarest counts at full effect — every additional one at half
 * (authentic Ingress). Common +4, Rare +8, Very Rare +12.
 */
class MultihackTest {

    private val owner = Factory.frog()
    private fun mh(type: MultihackType) = Multihack(type, owner)

    @Test
    fun noMultihacksAddNothing() {
        assertEquals(0, Multihack.additionalHacks(emptyList()))
    }

    @Test
    fun aSingleMultihackCountsAtFullEffect() {
        assertEquals(4, Multihack.additionalHacks(listOf(mh(MultihackType.COMMON))))
        assertEquals(12, Multihack.additionalHacks(listOf(mh(MultihackType.VERY_RARE))))
    }

    @Test
    fun theRarestCountsFullAndTheRestHalf() {
        // Very Rare (12, full) + Common (4 → +2) = 14
        assertEquals(14, Multihack.additionalHacks(listOf(mh(MultihackType.COMMON), mh(MultihackType.VERY_RARE))))
        // Very Rare (12, full) + Rare (8 → +4) + Common (4 → +2) = 18
        assertEquals(
            18,
            Multihack.additionalHacks(listOf(mh(MultihackType.COMMON), mh(MultihackType.RARE), mh(MultihackType.VERY_RARE))),
        )
        // Two Commons: 4 (full) + 4/2 = 6
        assertEquals(6, Multihack.additionalHacks(listOf(mh(MultihackType.COMMON), mh(MultihackType.COMMON))))
    }
}
