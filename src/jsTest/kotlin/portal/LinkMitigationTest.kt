package portal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [Portal.linkMitigationFor] — the pure link-defense curve extracted from `linkMitigation` (PLAN
 * non-functional track, phase B). Damage reduction rises with link count along an arctan that saturates near
 * 400/9 × π/2 ≈ 69.8%, so the first links matter most and links alone can never make a portal invulnerable.
 */
class LinkMitigationTest {

    @Test
    fun noLinksGiveNoMitigation() {
        assertEquals(0, Portal.linkMitigationFor(0))
    }

    @Test
    fun mitigationRisesWithDiminishingReturns() {
        // Pinned points along the curve (round(400/9 × atan(count/e))).
        assertEquals(16, Portal.linkMitigationFor(1), "the first link is worth a lot")
        assertEquals(55, Portal.linkMitigationFor(8))
        // The marginal gain shrinks: 0→1 adds 16, but 7→8 adds far less.
        val gainFirst = Portal.linkMitigationFor(1) - Portal.linkMitigationFor(0)
        val gainEighth = Portal.linkMitigationFor(8) - Portal.linkMitigationFor(7)
        assertTrue(gainFirst > gainEighth, "diminishing returns: the first link beats the eighth")
    }

    @Test
    fun mitigationIsMonotonicAndBoundedByTheAsymptote() {
        var prev = -1
        (0..50).forEach { count ->
            val m = Portal.linkMitigationFor(count)
            assertTrue(m >= prev, "non-decreasing in link count")
            assertTrue(m <= 70, "never exceeds the ≈69.8% asymptote (links alone can't make a portal immune)")
            prev = m
        }
        assertEquals(70, Portal.linkMitigationFor(1000), "saturates at the rounded asymptote for huge link counts")
    }
}
