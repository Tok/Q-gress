package agent

import agent.Faction.ENL
import agent.Faction.RES
import util.Rng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Shared-core tests for [Faction] (runs on jsNodeTest + jvmTest, Kover-covered). [Faction] now lives in
 * commonMain, so its enmity/lookup/random helpers are pinned here rather than in the JS-only test tree.
 */
class FactionTest {

    @Test
    fun enemyFactionsAreCross() {
        assertTrue(ENL.isEnemy(RES))
        assertTrue(RES.isEnemy(ENL))
    }

    @Test
    fun aFactionIsNotItsOwnEnemy() {
        assertFalse(ENL.isEnemy(ENL))
        assertFalse(RES.isEnemy(RES))
    }

    @Test
    fun enemyReturnsTheOpposite() {
        assertEquals(ENL, RES.enemy())
        assertEquals(RES, ENL.enemy())
    }

    @Test
    fun fromStringIsCaseInsensitiveAndNullSafe() {
        assertEquals(ENL, Faction.fromString("ENL"))
        assertEquals(ENL, Faction.fromString("enl"))
        assertEquals(RES, Faction.fromString("Res"))
        assertNull(Faction.fromString(null), "null → null")
        assertNull(Faction.fromString("machina"), "unknown name → null")
    }

    @Test
    fun allListsBothFactions() {
        assertEquals(listOf(ENL, RES), Faction.all())
    }

    @Test
    fun randomYieldsBothFactionsAndNothingElse() {
        // Seeded so the assertion is deterministic; a small sample must cover both branches of the 0.5 split.
        Rng.seed(20260627)
        val draws = (1..200).map { Faction.random() }
        assertTrue(draws.all { it == ENL || it == RES }, "only ever a real faction")
        assertTrue(draws.contains(ENL) && draws.contains(RES), "both sides come up over a sample")
    }

    @Test
    fun displayMetadataIsPinned() {
        assertEquals("Frog", ENL.nickName)
        assertEquals("Smurf", RES.nickName)
        assertEquals("#03DC03", ENL.color)
        assertEquals("#0088FF", RES.color)
    }
}
