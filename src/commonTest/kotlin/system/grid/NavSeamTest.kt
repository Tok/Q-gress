package system.grid

import Factory
import World
import agent.NonFaction
import extension.Grid
import extension.VectorField
import util.data.Cell
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
        World.resetNpcGrid()
        Nav.reset()
    }

    @Test
    fun portalCreationLeavesAnEmptyFieldWithDefaultSink() {
        // Default sink is NoOpFieldFlow (unbound headless) → no field delivered; the portal stays empty.
        val portal = Factory.portal()

        assertTrue(portal.vectors.isEmpty(), "a headless portal starts with an empty flow field (no-op sink)")
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

    @Test
    fun npcFieldsRouteOverTheUnmaskedGridWhilePortalsUseTheMaskedOne() {
        val masked: Grid = mapOf(Pos(0, 0) to Cell(Pos(0, 0), true, 0)) // stand-ins; only identity matters here
        val unmasked: Grid = mapOf(Pos(1, 1) to Cell(Pos(1, 1), true, 0))
        World.grid = masked
        World.npcGrid = unmasked
        val fake = RecordingFieldFlow(null)
        Nav.install(fake)

        NonFaction.getOrCreateVectorField(Pos(-500, -500)) // an NPC off-map destination
        assertEquals(unmasked, fake.lastGrid, "NPC flow fields flood the UNMASKED npc grid (they cross the moat)")

        Factory.portal() // a portal/agent field
        assertEquals(masked, fake.lastGrid, "portal/agent flow fields use the masked grid (stay in the arena)")

        World.grid = emptyMap()
        NonFaction.reset()
    }
}

/** A test [FieldFlow] sink that counts requests, records the grid it was asked to flood, and (optionally)
 *  delivers a fixed field inline. */
private class RecordingFieldFlow(private val deliver: VectorField?) : FieldFlow {
    var requests = 0
    var lastGrid: Grid? = null

    override fun compute(destination: Pos, grid: Grid, onReady: (VectorField) -> Unit) {
        requests++
        lastGrid = grid
        deliver?.let(onReady)
    }
}
