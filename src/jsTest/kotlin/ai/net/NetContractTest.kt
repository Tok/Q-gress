package ai.net

import ai.Observation
import ai.SliderVector
import kotlin.test.Test
import kotlin.test.assertEquals

/** Guards the net I/O dims pinned in [NetArch] against the real observation/slider layouts they mirror. */
class NetContractTest {
    @Test
    fun netIoMatchesObservationAndSliderLayouts() {
        assertEquals(Observation.SIZE, NetArch.INPUTS, "net input dim must equal the observation feature count")
        assertEquals(SliderVector.SIZE, NetArch.OUTPUTS, "net output dim must equal the behaviour-slider count")
    }
}
