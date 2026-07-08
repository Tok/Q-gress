package agent

import Factory
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The AP→level table in [Agent.getLevel]: each band maps a mid-range action-point total to its level (1..16).
 * Mid-range values avoid the overlapping band boundaries (e.g. 10000 sits in both `0..10000` and `10000..30000`;
 * `when` takes the first, so we probe the interiors instead).
 */
class AgentLevelTest {

    private fun levelAt(ap: Int): Int {
        val agent = Factory.frog()
        agent.ap = ap
        return agent.getLevel()
    }

    @Test
    fun everyApBandMapsToItsLevel() {
        assertEquals(1, levelAt(5_000))
        assertEquals(2, levelAt(20_000))
        assertEquals(3, levelAt(50_000))
        assertEquals(4, levelAt(100_000))
        assertEquals(5, levelAt(200_000))
        assertEquals(6, levelAt(400_000))
        assertEquals(7, levelAt(800_000))
        assertEquals(8, levelAt(1_500_000))
        assertEquals(9, levelAt(3_000_000))
        assertEquals(10, levelAt(5_000_000))
        assertEquals(11, levelAt(7_000_000))
        assertEquals(12, levelAt(10_000_000))
        assertEquals(13, levelAt(14_000_000))
        assertEquals(14, levelAt(20_000_000))
        assertEquals(15, levelAt(30_000_000))
        assertEquals(16, levelAt(50_000_000), "beyond the top band → the max level")
    }
}
