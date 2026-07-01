package agent

import Factory
import World
import config.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BalanceTest {

    @BeforeTest
    @AfterTest
    fun clean() {
        World.allAgents.clear()
        World.allPortals.clear()
    }

    private fun addAgents(faction: Faction, n: Int) = repeat(n) { World.allAgents.add(Factory.agent(faction)) }
    private fun addPortals(faction: Faction, n: Int) = repeat(n) { World.allPortals.add(Factory.portal(faction)) }

    @Test
    fun largerFactionRecruitsLessSmallerFactionMore() {
        addAgents(Faction.ENL, 4)
        addAgents(Faction.RES, 1)
        assertTrue(Balance.recruitFactor(Faction.ENL) < 1.0, "the bigger roster recruits below base")
        assertTrue(Balance.recruitFactor(Faction.RES) > 1.0, "the smaller roster recruits above base")
    }

    @Test
    fun recruitFactorIsClampedToTheConfiguredBand() {
        addAgents(Faction.ENL, 20) // hugely outnumbering RES (0)
        assertEquals(Config.recruitFactorMin, Balance.recruitFactor(Faction.ENL), "leader clamped to the floor")
        assertEquals(Config.recruitFactorMax, Balance.recruitFactor(Faction.RES), "underdog clamped to the ceiling")
    }

    @Test
    fun factionBehindOnPortalsAttacksHarder() {
        addPortals(Faction.ENL, 1)
        addPortals(Faction.RES, 3)
        assertTrue(Balance.attackBoost(Faction.ENL) > 1.0, "the side behind on portals hits harder")
        assertEquals(1.0, Balance.attackBoost(Faction.RES), "the leader gets no boost (only the underdog is helped)")
    }

    @Test
    fun evenPortalsMeanNoAttackBoost() {
        addPortals(Faction.ENL, 2)
        addPortals(Faction.RES, 2)
        assertEquals(1.0, Balance.attackBoost(Faction.ENL))
        assertEquals(1.0, Balance.attackBoost(Faction.RES))
    }

    @Test
    fun attackBoostGrowsWithTheSquareOfTheDeficit() {
        // Pin the rubber-band to known coefficients: boost = 1 + comebackMax × dynamism × deficit² = 1 + deficit².
        val savedMax = Config.comebackMax
        val savedDyn = Config.combatDynamism
        try {
            Config.comebackMax = 2.0
            Config.combatDynamism = 0.5 // comebackAttackBonus() == combatDynamism
            addPortals(Faction.RES, 9) // ENL has 0 → deficit = 9/9 = 1.0
            assertEquals(2.0, Balance.attackBoost(Faction.ENL), 1e-9, "fully shut out: 1 + 1.0²")
            World.allPortals.clear()
            addPortals(Faction.ENL, 1)
            addPortals(Faction.RES, 9) // deficit = (9 − 1)/10 = 0.8
            assertEquals(1.64, Balance.attackBoost(Faction.ENL), 1e-9, "1 + 0.8² = 1.64 (steeply less than at 1.0)")
        } finally {
            Config.comebackMax = savedMax
            Config.combatDynamism = savedDyn
        }
    }

    @Test
    fun leadShareIsZeroWhenNeitherSideHoldsAnyMu() {
        // No fields → calcTotalMu == 0 for both → the share-deficit total<=0 guard yields 0 (even, not NaN).
        assertEquals(0.0, Balance.leadShare(Faction.ENL))
        assertEquals(0.0, Balance.leadShare(Faction.RES))
    }
}
