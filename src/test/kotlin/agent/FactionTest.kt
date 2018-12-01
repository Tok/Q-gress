package agent

import agent.Faction.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FactionTest {

    @Test
    fun enemyFactions() {
        assertTrue(ENL.isEnemy(RES))
        assertTrue(RES.isEnemy(ENL))
    }

    @Test
    fun findEnemyFaction() {
        assertEquals(ENL, RES.enemy())
        assertEquals(RES, ENL.enemy())
    }

    @Test
    fun findNoEnemyForNpcFaction() {
        assertEquals(NONE, NONE.enemy())
    }

    @Test
    fun friendlyFrogs() {
        assertFalse(ENL.isEnemy(ENL))
    }

    @Test
    fun friendlySmurfs() {
        assertFalse(RES.isEnemy(RES))
    }

    @Test
    fun friendlyNpcs() {
        assertFalse(ENL.isEnemy(NONE))
        assertFalse(RES.isEnemy(NONE))
        assertFalse(NONE.isEnemy(ENL))
        assertFalse(NONE.isEnemy(RES))
    }
}
