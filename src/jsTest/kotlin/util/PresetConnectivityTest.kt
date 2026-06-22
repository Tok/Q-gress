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
    fun allPresetsAreSingleComponentAfterCarving() {
        if (PRESET_FIXTURES.isEmpty()) {
            println("[PresetConnectivityTest] no fixtures committed yet — run ?debug=capture")
            return
        }
        PRESET_FIXTURES.forEach { fx ->
            val carved = GridConnectivity.connectIslands(fx.toGrid())
            val r = GridConnectivity.report(carved, fx.w, fx.h)
            assertEquals(1, r.islands, "${fx.preset}: connectIslands must leave a single 4-connected component")
            assertTrue(r.walkability >= minWalkability, "${fx.preset}: walkability ${r.walkability} below the playable gate")
        }
    }

    @Test
    fun reportOnScreenDetourPresets() {
        // Informational (not yet a hard gate): presets whose on-screen regions only reach each other
        // via the off-screen ring — connectIslands joins to the outside but doesn't link them directly.
        // Drives the connectIslands improvement (PLAN); flip to a hard assert once that lands.
        if (PRESET_FIXTURES.isEmpty()) return
        val offenders = PRESET_FIXTURES.mapNotNull { fx ->
            val carved = GridConnectivity.connectIslands(fx.toGrid())
            val n = GridConnectivity.report(carved, fx.w, fx.h).onScreenIslands
            if (n > 1) "${fx.preset} ($n)" else null
        }
        if (offenders.isNotEmpty()) {
            println("[PresetConnectivityTest] off-screen-detour presets (on-screen islands): ${offenders.joinToString()}")
        }
    }

    @Test
    fun harnessFlagsOffScreenDetour() {
        // On-screen [0,3)×[0,2) with a wall column at x=1 → two halves joined only via the off-screen
        // ring. connectIslands is a no-op (already one component through the ring) but the on-screen
        // split remains — exactly the hazard the audit must surface.
        val onScreen = listOf(true, false, true, true, false, true) // rows (.#.)(.#.)
        val fx = GridFixture("SYNTH", 3, 2, 1, GridFixture.rleEncode(onScreen))
        val carved = GridConnectivity.connectIslands(fx.toGrid())
        val r = GridConnectivity.report(carved, fx.w, fx.h)
        assertEquals(1, r.islands, "whole grid is one component via the ring")
        assertEquals(2, r.onScreenIslands, "on-screen halves stay split — the detour hazard")
    }
}
