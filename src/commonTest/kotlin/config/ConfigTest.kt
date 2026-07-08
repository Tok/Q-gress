package config

import agent.Faction
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
    private var savedStartStage = StartStage.MID

    @BeforeTest
    fun save() {
        savedMultiplier = Config.npcMultiplier
        savedStartPortals = Config.startPortals
        savedDynamism = Config.combatDynamism
        savedStartStage = Config.startStage
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
        Config.startStage = savedStartStage
        Sim.setExactSize(savedSimWidth, savedSimHeight)
    }

    // --- npcPopulation -------------------------------------------------------
    // pop = 300 (density) × walkableArea × (1 + 1.2 × cityDensity) × touristMul × npcMultiplier, clamped [30, cap]
    // where cap is 1000 (tiny…mid maps) or 2000 (large/giant). At 1200×800 areaRatio == 1, walk 0.5 →
    // walkableArea 0.5, cityDensity 0.5 → 300 × 0.5 × 1.6 = 240.

    @Test
    fun npcPopulationPinsTheBaseFormula() {
        Config.npcMultiplier = 1.0
        assertEquals(240, Config.npcPopulation(1200, 800, walkability = 0.5), "density 300 × area 0.5 × city 1.6")
    }

    @Test
    fun npcPopulationGetsTheTouristCrowdBonus() {
        Config.npcMultiplier = 1.0
        val plain = Config.npcPopulation(1200, 800, walkability = 0.5)
        val tourist = Config.npcPopulation(1200, 800, walkability = 0.5, tourist = true)
        assertEquals(240, plain)
        assertEquals(384, tourist, "240 × 1.6 tourist multiplier = 384 (truncated)")
    }

    @Test
    fun npcPopulationScalesWithThePlayerMultiplier() {
        Config.npcMultiplier = 2.0
        assertEquals(480, Config.npcPopulation(1200, 800, walkability = 0.5), "the base 240 doubled")
    }

    @Test
    fun npcPopulationIsFlooredOnATinyMap() {
        Config.npcMultiplier = 1.0
        assertEquals(Config.MIN_NONFACTION, Config.npcPopulation(20, 20, walkability = 0.5), "always enough to recruit")
    }

    @Test
    fun npcCapIs1000OnMidAndBelowBut2000OnLargeAndGiant() {
        Config.npcMultiplier = 3.0 // over-populate so the ceiling always bites
        val mid = Sim.sideForArea(Sim.MID_KM2)
        val large = Sim.sideForArea(Sim.LARGE_KM2)
        val giant = Sim.sideForArea(Sim.GIANT_KM2)
        assertEquals(Config.MAX_NONFACTION_CAP, Config.npcPopulation(mid, mid, walkability = 1.0), "mid map keeps the 1000 cap")
        assertEquals(Config.MAX_NONFACTION_CAP_LARGE, Config.npcPopulation(large, large, walkability = 1.0), "large map → 2000 cap")
        assertEquals(Config.MAX_NONFACTION_CAP_LARGE, Config.npcPopulation(giant, giant, walkability = 1.0), "giant map → 2000 cap")
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

    // --- starting roster by stage -------------------------------------------

    @Test
    fun startRosterIsASingleAgentAtTheColdOpen() {
        Config.startStage = StartStage.START
        assertEquals(1, Config.startFrogs(), "a normal start is one agent per side")
        assertEquals(1, Config.startSmurfs())
        assertEquals(StartStage.START.initialAp, Config.initialAp(), "AP tracks the stage")
    }

    @Test
    fun midStageSeedsTheSuggestedSquad() {
        Config.startStage = StartStage.MID
        assertEquals(Sim.suggestedAgents(Sim.areaKm2()), Config.startFrogs(), "mid-game seeds the size-scaled squad")
    }

    @Test
    fun endStageSeedsTheFullRosterCap() {
        Config.startStage = StartStage.END
        assertEquals(Config.rosterCap(), Config.startSmurfs(), "end-game seeds the full size roster")
    }

    // --- maxFor ---------------------------------------------------------------

    @Test
    fun maxForFactionsIsTheRosterCapButNpcsAreTheirOwnPool() {
        assertEquals(Config.rosterCap(), Config.maxFor(Faction.ENL), "ENL cap is the size-scaled roster cap")
        assertEquals(Config.rosterCap(), Config.maxFor(Faction.RES), "RES cap is the size-scaled roster cap")
        assertEquals(Config.maxNonFaction, Config.maxFor(null), "no faction → the NPC population target")
    }
}
