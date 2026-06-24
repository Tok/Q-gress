package ai.net

import ai.Observation
import ai.SliderVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Guards the baked champion genome: it must decode for this build's layout and run a valid forward pass. */
class ChampionTest {

    @Test
    fun bakedChampionDecodesAndRuns() {
        val net = GenomeIO.decode(Champion.JSON)
        val out = net.forward(DoubleArray(Observation.SIZE) { 0.5 })

        assertEquals(SliderVector.SIZE, out.size, "champion drives all the slider slots")
        assertTrue(out.all { it in 0.0..1.0 }, "outputs are valid 0..1 slider weights")
    }

    @Test
    fun champWasTrainedToBeatTheBaseline() {
        assertTrue((GenomeIO.fitnessOf(Champion.JSON) ?: 0.0) > 0.0, "the baked champion has a positive fitness margin")
    }
}
