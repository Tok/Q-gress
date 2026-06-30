package system.grid

import Factory
import World
import extension.VectorField
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The flow-field-compute seam (PLAN Phase-B groundwork): a new portal asks for its flow field through
 * [Nav.sink] instead of naming the coroutine-bound [Pathfinding] directly. Proves (a) the default headless
 * sink ([PathFieldFlow]'s skip branch in Node) lets portal creation run without touching coroutines, leaving
 * an empty field, and (b) logic reaches the installed sink and stores what it delivers. Mirrors
 * [system.effect.EffectsSeamTest] / [system.audio.AudioSeamTest].
 */
class NavSeamTest {

    @BeforeTest
    fun clean() {
        World.allPortals.clear()
    }

    @AfterTest
    fun restore() {
        World.allPortals.clear()
        Nav.reset()
    }

    @Test
    fun portalCreationLeavesAnEmptyFieldWithDefaultSink() {
        // Default Node sink is PathFieldFlow → not-browser + no headless compute = skip; the field stays empty.
        val portal = Factory.portal()

        assertTrue(portal.vectors.isEmpty(), "a headless portal starts with an empty flow field (skip branch)")
    }

    @Test
    fun portalCreationRoutesComputeToTheInstalledSink() {
        val delivered = VectorField(0, 0, 1, 1, doubleArrayOf(1.0), doubleArrayOf(0.0))
        val fake = RecordingFieldFlow(delivered)
        Nav.install(fake)

        val portal = Factory.portal()

        assertEquals(1, fake.requests, "portal creation requests exactly one flow-field compute through the sink")
        assertTrue(portal.vectors.isNotEmpty(), "the field the sink delivers is stored on the portal")
    }
}

/** A test [FieldFlow] sink that counts requests and (optionally) delivers a fixed field inline. */
private class RecordingFieldFlow(private val deliver: VectorField?) : FieldFlow {
    var requests = 0

    override fun compute(destination: Pos, onReady: (VectorField) -> Unit) {
        requests++
        deliver?.let(onReady)
    }
}
