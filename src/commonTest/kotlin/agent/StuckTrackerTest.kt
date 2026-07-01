package agent

import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Detection core for the stuck/loop recovery: net displacement over a full sample window under one
 * deployment range (34) ⇒ stuck. Covers frozen-in-place, small-radius looping, and the negative case
 * (steady progress), plus the min-samples warm-up and entity forgetting.
 */
class StuckTrackerTest {

    private val key = "agent:test"

    @BeforeTest
    fun setup() = StuckTracker.reset()

    @AfterTest
    fun teardown() = StuckTracker.reset()

    private fun feed(samples: Int, posAt: (Int) -> Pos) {
        repeat(samples) { i -> StuckTracker.sample(listOf(key to posAt(i))) }
    }

    @Test
    fun frozenInPlaceIsStuck() {
        feed(150) { Pos(100, 100) } // never moves
        assertTrue(StuckTracker.isStuck(key))
    }

    @Test
    fun smallRadiusLoopIsStuck() {
        // oscillate within ~±10 units (well under the 34 deployment range) — start ≈ end every window
        feed(150) { i -> if (i % 2 == 0) Pos(100, 100) else Pos(108, 106) }
        assertTrue(StuckTracker.isStuck(key))
    }

    @Test
    fun steadyProgressIsNotStuck() {
        feed(150) { i -> Pos(100 + i * 5, 100) } // marches away ~5 units/tick → huge net displacement
        assertFalse(StuckTracker.isStuck(key))
    }

    @Test
    fun notJudgedBeforeMinSamples() {
        feed(29) { Pos(100, 100) } // frozen, but below the 30-sample warm-up
        assertFalse(StuckTracker.isStuck(key))
        StuckTracker.sample(listOf(key to Pos(100, 100))) // 30th sample → now judged
        assertTrue(StuckTracker.isStuck(key))
    }

    @Test
    fun entityNotSampledIsForgotten() {
        feed(150) { Pos(100, 100) }
        assertTrue(StuckTracker.isStuck(key))
        StuckTracker.sample(emptyList()) // this entity stopped travelling → dropped from the stuck set
        assertFalse(StuckTracker.isStuck(key))
    }

    @Test
    fun inPlaceOscillationAcrossBriefGapsIsStuck() {
        // The MOVE↔WAIT-in-place symptom: present (MOVE, frozen) every other tick, absent (the 1-tick WAIT) between.
        // The brief gaps must NOT reset the window, or this never reaches the sample count and slips past recovery.
        repeat(90) { i -> if (i % 2 == 0) StuckTracker.sample(listOf(key to Pos(100, 100))) else StuckTracker.sample(emptyList()) }
        StuckTracker.sample(listOf(key to Pos(100, 100))) // end on a present tick so it's in the stuck set
        assertTrue(StuckTracker.isStuck(key), "oscillating in and out of MOVE is still caught")
    }

    @Test
    fun aRealStopBeyondTheGraceResetsTheWindow() {
        feed(150) { Pos(100, 100) }
        assertTrue(StuckTracker.isStuck(key))
        repeat(4) { StuckTracker.sample(emptyList()) } // absent past the grace → genuinely stopped → window dropped
        StuckTracker.sample(listOf(key to Pos(100, 100))) // returns frozen, but must warm up from scratch
        assertFalse(StuckTracker.isStuck(key), "not instantly re-flagged after a real stop")
    }
}
