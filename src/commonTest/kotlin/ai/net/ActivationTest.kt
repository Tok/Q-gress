package ai.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActivationTest {
    @Test
    fun applyPerFunction() {
        assertEquals(0.0, Activation.RELU.apply(-3.0)) // clamps negatives
        assertEquals(2.0, Activation.RELU.apply(2.0))
        assertEquals(5.0, Activation.LINEAR.apply(5.0))
        assertEquals(0.5, Activation.SIGMOID.apply(0.0), 1e-9) // 1/(1+e^0)
        assertEquals(0.0, Activation.TANH.apply(0.0), 1e-9)
        assertTrue(Activation.TANH.apply(10.0) > 0.99, "tanh saturates toward 1")
    }

    @Test
    fun signedMarksNegativeCapableFunctions() {
        assertTrue(Activation.TANH.signed)
        assertTrue(Activation.LINEAR.signed)
        assertFalse(Activation.RELU.signed)
        assertFalse(Activation.SIGMOID.signed)
    }

    @Test
    fun fromNameIsTolerantAndDefaultsToTanh() {
        assertEquals(Activation.RELU, Activation.from("relu")) // case-insensitive
        assertEquals(Activation.SIGMOID, Activation.from("SIGMOID"))
        assertEquals(Activation.TANH, Activation.from(null)) // default
        assertEquals(Activation.TANH, Activation.from("nonsense")) // default
    }
}
