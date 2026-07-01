package config

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Shared-core tests for the pure tuning formulas in [ConfigMath] (runs on both jsNodeTest and jvmTest). The
 * World-coupled wrapper [Config] is characterized separately in jsTest. Reference screen is 1200×800 (the
 * Node-test [Dim]), so a 1200×800 map has areaRatio == 1.
 */
class ConfigMathTest {
    // pop = 180 × walkableArea × (1 + 1.2 × cityDensity) × touristMul × npcMultiplier, clamped [30, 1000].
    // areaRatio 1.0 == a reference-screen-sized map; walk 0.5 → walkableArea 0.5, city 1.6 → 180 × 0.5 × 1.6 = 144.
    @Test
    fun npcPopulationPinsTheBaseFormula() {
        assertEquals(240, ConfigMath.npcPopulation(areaRatio = 1.0, walkability = 0.5, tourist = false, npcMultiplier = 1.0))
    }

    @Test
    fun npcPopulationGetsTheTouristBonus() {
        assertEquals(384, ConfigMath.npcPopulation(1.0, 0.5, tourist = true, npcMultiplier = 1.0), "240 × 1.6 = 384")
    }

    @Test
    fun npcPopulationScalesWithTheMultiplier() {
        assertEquals(480, ConfigMath.npcPopulation(1.0, 0.5, tourist = false, npcMultiplier = 2.0), "the base 240 doubled")
    }

    @Test
    fun npcPopulationIsFlooredAndCapped() {
        assertEquals(ConfigMath.MIN_NONFACTION, ConfigMath.npcPopulation(0.0004, 0.5, false, 1.0), "tiny map → floor")
        assertEquals(ConfigMath.MAX_NONFACTION_CAP, ConfigMath.npcPopulation(417.0, 1.0, false, 3.0), "huge map → ceiling")
    }

    @Test
    fun targetPortalsScalesWithWalkableAreaClampedToTheBand() {
        // 200 portals / fully-walkable km²: 0.2 km² at half walkability → 200 × 0.2 × 0.5 = 20
        assertEquals(20, ConfigMath.targetPortals(areaKm2 = 0.2, walkability = 0.5, startPortals = 8, maxPortals = 89))
        // a big, open map exceeds the perf ceiling → clamped to maxPortals
        assertEquals(89, ConfigMath.targetPortals(areaKm2 = 0.5, walkability = 1.0, startPortals = 8, maxPortals = 89))
        // little walkable ground → below the start count → clamped UP to it
        assertEquals(8, ConfigMath.targetPortals(areaKm2 = 0.01, walkability = 0.2, startPortals = 8, maxPortals = 89))
        // walkability is clamped to [0,1]; 0 walkable → the start-count floor
        assertEquals(8, ConfigMath.targetPortals(areaKm2 = 1.0, walkability = 0.0, startPortals = 8, maxPortals = 89))
    }

    @Test
    fun maxMitigationFallsWithDynamismAndIsFloored() {
        assertEquals(IngressFacts.MITIGATION_CAP_PCT, ConfigMath.maxMitigation(0.0))
        assertEquals(50, ConfigMath.maxMitigation(0.6), "95 − 0.6 × 75 = 50")
        assertEquals(20, ConfigMath.maxMitigation(1.0), "95 − 75 = 20")
        assertEquals(15, ConfigMath.maxMitigation(2.0), "floored at 15")
    }

    @Test
    fun weaponDropAndXmpThresholdScaleWithDynamism() {
        assertEquals(1.0, ConfigMath.weaponDropMultiplier(0.0))
        assertEquals(20.0, ConfigMath.weaponDropMultiplier(1.0), "1 + 1 × 19")
        assertEquals(30, ConfigMath.attackXmpThreshold(0.0))
        assertEquals(8, ConfigMath.attackXmpThreshold(1.0), "30 − 22 = 8")
        assertEquals(0.6, ConfigMath.comebackAttackBonus(0.6))
    }
}
