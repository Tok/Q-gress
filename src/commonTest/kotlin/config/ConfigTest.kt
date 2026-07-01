package config

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Characterization tests (PLAN non-functional track, phase A) for the pure tuning formulas in [Config].
 * They pin the current input→output behaviour so the upcoming refactor can't change it silently. Values
 * assume the Node-test [Dim] (1200×800), where a 1200×800 map has `areaRatio == 1`.
 *
 * The mutable knobs ([Config.npcMultiplier], [Config.startPortals], [Config.combatDynamism]) are saved and
 * restored around each test so the shared singleton isn't left perturbed for other suites.
 */
class ConfigTest {

    private var savedMultiplier = 0.0
    private var savedStartPortals = 0
    private var savedDynamism = 0.0
    private var savedSimWidth = 0
    private var savedSimHeight = 0

    @BeforeTest
    fun save() {
        savedMultiplier = Config.npcMultiplier
        savedStartPortals = Config.startPortals
        savedDynamism = Config.combatDynamism
        // targetPortals reads the live Sim area — pin it (other suites share + perturb the Sim singleton).
        savedSimWidth = Sim.width
        savedSimHeight = Sim.height
        Sim.setExactSize(Sim.sideForArea(Sim.SMALL_KM2), Sim.sideForArea(Sim.SMALL_KM2))
    }

    @AfterTest
    fun restore() {
        Config.npcMultiplier = savedMultiplier
        Config.startPortals = savedStartPortals
        Config.combatDynamism = savedDynamism
        Sim.setExactSize(savedSimWidth, savedSimHeight)
    }

    // --- npcPopulation -------------------------------------------------------
    // pop = 180 (density) × walkableArea × (1 + 1.2 × cityDensity) × touristMul × npcMultiplier, clamped [30,1000]
    // At 1200×800 areaRatio == 1, walk 0.5 → walkableArea 0.5, cityDensity 0.5 → 180 × 0.5 × 1.6 = 144.

    @Test
    fun npcPopulationPinsTheBaseFormula() {
        Config.npcMultiplier = 1.0
        assertEquals(144, Config.npcPopulation(1200, 800, walkability = 0.5), "density 180 × area 0.5 × city 1.6")
    }

    @Test
    fun npcPopulationGetsTheTouristCrowdBonus() {
        Config.npcMultiplier = 1.0
        val plain = Config.npcPopulation(1200, 800, walkability = 0.5)
        val tourist = Config.npcPopulation(1200, 800, walkability = 0.5, tourist = true)
        assertEquals(144, plain)
        assertEquals(230, tourist, "144 × 1.6 tourist multiplier = 230 (truncated)")
    }

    @Test
    fun npcPopulationScalesWithThePlayerMultiplier() {
        Config.npcMultiplier = 2.0
        assertEquals(288, Config.npcPopulation(1200, 800, walkability = 0.5), "the base 144 doubled")
    }

    @Test
    fun npcPopulationIsFlooredOnATinyMap() {
        Config.npcMultiplier = 1.0
        assertEquals(Config.MIN_NONFACTION, Config.npcPopulation(20, 20, walkability = 0.5), "always enough to recruit")
    }

    @Test
    fun npcPopulationIsCappedOnAHugeMap() {
        Config.npcMultiplier = 3.0
        assertEquals(Config.MAX_NONFACTION_CAP, Config.npcPopulation(20000, 20000, walkability = 1.0), "perf ceiling")
    }

    // --- targetPortals -------------------------------------------------------
    // 200 portals / fully-walkable km² (Sim.areaKm2 × walkability), clamped to [startPortals, maxPortals=89].

    @Test
    fun targetPortalsUsesTheWalkableAreaDensity() {
        Config.startPortals = 1 // low floor so the walkable-area term shows through
        val expected = (ConfigMath.PORTALS_PER_WALKABLE_KM2 * Sim.areaKm2() * 0.5).toInt().coerceIn(1, Config.maxPortals)
        assertEquals(expected, Config.targetPortals(walkability = 0.5), "density × walkable km², clamped")
    }

    @Test
    fun targetPortalsFloorsAtTheStartCountWithNoWalkableGround() {
        Config.startPortals = 8
        assertEquals(8, Config.targetPortals(walkability = 0.0), "no walkable area → the start-count floor")
    }

    @Test
    fun targetPortalsRisesWithWalkableGround() {
        Config.startPortals = 1
        assertTrue(Config.targetPortals(walkability = 1.0) > Config.targetPortals(walkability = 0.2), "more open ground → more portals")
    }

    // --- combat dynamism knobs ----------------------------------------------

    @Test
    fun maxMitigationFallsAsDynamismRises() {
        Config.combatDynamism = 0.0
        assertEquals(IngressFacts.MITIGATION_CAP_PCT, Config.maxMitigation(), "tanky shields at zero dynamism")
        Config.combatDynamism = 0.6
        assertEquals(50, Config.maxMitigation(), "95 − 0.6 × 75 = 50")
        Config.combatDynamism = 1.0
        assertEquals(20, Config.maxMitigation(), "95 − 75 = 20")
    }

    @Test
    fun maxMitigationIsFlooredAtFifteen() {
        Config.combatDynamism = 2.0 // out of the 0..1 band — exercise the clamp
        assertEquals(15, Config.maxMitigation())
    }

    @Test
    fun weaponDropMultiplierRisesWithDynamism() {
        Config.combatDynamism = 0.0
        assertEquals(1.0, Config.weaponDropMultiplier())
        Config.combatDynamism = 1.0
        assertEquals(20.0, Config.weaponDropMultiplier(), "1 + 1 × 19")
    }

    @Test
    fun attackXmpThresholdDropsAsDynamismRises() {
        Config.combatDynamism = 0.0
        assertEquals(30, Config.attackXmpThreshold(), "cautious: hoard 30 before assaulting")
        Config.combatDynamism = 1.0
        assertEquals(8, Config.attackXmpThreshold(), "trigger-happy: 30 − 22 = 8 (the floor)")
    }

    @Test
    fun attackXmpThresholdIsFlooredAtEight() {
        Config.combatDynamism = 2.0 // out of band — would compute below 8
        assertEquals(8, Config.attackXmpThreshold())
    }

    @Test
    fun comebackAttackBonusTracksDynamism() {
        Config.combatDynamism = 0.42
        assertEquals(0.42, Config.comebackAttackBonus())
    }
}
