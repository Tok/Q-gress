package util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Offline connectivity audit of the committed preset snapshots (see GridCapture / PRESET_FIXTURES).
 * Each fixture is the raw, pre-carve passability; the test runs the real [GridConnectivity.connectIslands]
 * and checks the resulting gameplay grid. With no fixtures yet, the per-preset checks no-op (loudly),
 * and [harnessFlagsOffScreenDetour] proves the audit logic regardless.
 */
class PresetConnectivityTest {
    private val minWalkability = 0.12 // matches the live HtmlUtil gate (mostly-water → unplayable)

    @Test
    fun allPresetsFullyConnectedForGameplay() {
        if (PRESET_FIXTURES.isEmpty()) {
            println("[PresetConnectivityTest] no fixtures committed yet — run ?debug=capture")
            return
        }
        PRESET_FIXTURES.forEach { fx ->
            // Use the gameplay carver (w/h overload): seals pockets AND joins on-screen regions.
            val r = GridConnectivity.report(GridConnectivity.connectIslands(fx.toGrid(), fx.w, fx.h), fx.w, fx.h)
            assertEquals(1, r.islands, "${fx.preset}: must leave a single 4-connected component")
            assertEquals(1, r.onScreenIslands, "${fx.preset}: on-screen regions must all connect without an off-screen detour")
            assertTrue(r.walkability >= minWalkability, "${fx.preset}: walkability ${r.walkability} below the playable gate")
        }
    }

    @Test
    fun harnessFlagsAndCarverFixesOffScreenDetour() {
        // On-screen [0,3)×[0,2) with a wall column at x=1 → two halves joined only via the off-screen ring.
        val onScreen = listOf(true, false, true, true, false, true) // rows (.#.)(.#.)
        val fx = GridFixture("SYNTH", 3, 2, 1, GridFixture.rleEncode(onScreen))
        // Whole-grid carve alone: one component via the ring, but on-screen halves stay split (the hazard).
        val ringOnly = GridConnectivity.report(GridConnectivity.connectIslands(fx.toGrid()), fx.w, fx.h)
        assertEquals(1, ringOnly.islands)
        assertEquals(2, ringOnly.onScreenIslands)
        // Gameplay carve: on-screen halves are joined directly.
        val gameplay = GridConnectivity.report(GridConnectivity.connectIslands(fx.toGrid(), fx.w, fx.h), fx.w, fx.h)
        assertEquals(1, gameplay.onScreenIslands, "on-screen carve must join the split halves")
    }
}
