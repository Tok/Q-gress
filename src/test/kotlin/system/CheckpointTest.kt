package system

import agent.Faction
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckpointTest {

    @Test
    fun totalMindUnits() {
        val cp = Checkpoint(5, 3, false)
        assertEquals(8, cp.total())
    }

    @Test
    fun factionMindUnits() {
        val cp = Checkpoint(5, 3, false)
        assertEquals(5, cp.mu(Faction.ENL))
        assertEquals(3, cp.mu(Faction.RES))
        assertEquals(0, cp.mu(Faction.NONE))
    }
}
