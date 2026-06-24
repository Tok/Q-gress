package ai

import agent.Faction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObservationTest {

    @Test
    fun vectorHasTheDeclaredSize() {
        assertEquals(Observation.SIZE, Observation.observe(Faction.ENL).size)
    }

    @Test
    fun everyFeatureIsNormalised() {
        Faction.all().forEach { faction ->
            Observation.observe(faction).forEachIndexed { i, v ->
                assertTrue(v in 0.0..1.0, "feature $i for $faction was $v, expected 0..1")
            }
        }
    }
}
