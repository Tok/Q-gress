package config

import agent.Faction
import util.HtmlUtil

object Config {
    const val minPortals = 3
    const val maxPortals = 89
    const val minFrogs = 2
    const val maxFrogs = 21
    const val minSmurfs = 2
    const val maxSmurfs = 21
    const val frogQuitRate = 0.1
    const val smurfQuitRate = 0.1
    const val factionChangeRate = 0.01
    const val portalRemovalRate = 0.1

    // --- Recruitment balance (Phase 5) ---------------------------------------
    // Recruiting used to be free, making "recruit-rush" a strictly-positive
    // throughput multiplier. Now it costs XM (competing with linking/deploying,
    // which also cost XM) and yields diminishing returns as the faction fills
    // toward its cap — so growing the roster is a real tradeoff, not free.
    const val recruitmentXmCost = 250 // XM spent per recruit attempt (cf. link = 250)
    const val recruitmentBaseChance = 0.05 // success chance at an empty roster; scales →0 at the cap

    var startPortals = 8 // initial portal count (chosen at onboarding — the "portal density"); scales by map size
    var quickStart = false // onboarding option: start with a full roster + AP so the early game moves
    fun startFrogs() = if (quickStart) 8 else minFrogs
    fun startSmurfs() = if (quickStart) 8 else minSmurfs
    fun initialAp() = if (quickStart) 2000000 else 0

    // NPC population is NOT a player setting — it's auto-derived from map area + location (see
    // [npcPopulation]) at world-gen, and held constant by 1-for-1 replacement on recruit (Recruiter),
    // so a game never runs out of people to recruit.
    var maxNonFaction = 500 // current target population (set by npcPopulation at world-gen)
    fun maxFor(faction: Faction? = null) = when (faction) {
        Faction.ENL -> maxFrogs
        Faction.RES -> maxSmurfs
        else -> maxNonFaction
    }

    const val MIN_NONFACTION = 30 // floor: always enough to recruit, even on a tiny/dense map (or a low multiplier)
    const val MAX_NONFACTION_CAP = 2000 // ceiling: keep huge/dense maps from spawning a perf-killing crowd
    private const val NPC_DENSITY = 360.0 // NPCs per one screenful (Dim.width × Dim.height) of walkable area
    private const val CITY_GAIN = 1.2 // built-up (low-walkable) areas pack in more people
    private const val TOURIST_MUL = 1.6 // famous tourist spots draw extra crowds

    /** Player NPC-density multiplier (0.1–3.0), chosen at onboarding — scales the auto population. */
    var npcMultiplier = 1.0

    /**
     * Appropriate NPC population for a [width]×[height] map at a location of the given [walkability]
     * (fraction of passable cells). Driven by the **walkable area** (where NPCs can roam), boosted by
     * **city density** (built-up = low-walkable ≈ denser city → more people; a proxy until we read real
     * building coverage) and, for a [tourist] hotspot, a crowd bonus. Scaled by [npcMultiplier], then
     * clamped to [[MIN_NONFACTION], [MAX_NONFACTION_CAP]] — always enough to recruit, never a perf-killing
     * crowd on a huge map. Grows with the play area.
     */
    fun npcPopulation(width: Int, height: Int, walkability: Double, tourist: Boolean = false): Int {
        val walk = walkability.coerceIn(0.0, 1.0)
        val areaRatio = (width.toDouble() * height) / (Dim.width.toDouble() * Dim.height)
        val walkableArea = areaRatio * walk // NPCs roam open ground → the primary driver
        val cityDensity = 1.0 - walk // proxy: the less walkable a (playable) area is, the more built-up
        val touristMul = if (tourist) TOURIST_MUL else 1.0
        val pop = NPC_DENSITY * walkableArea * (1.0 + CITY_GAIN * cityDensity) * touristMul * npcMultiplier
        return pop.toInt().coerceIn(MIN_NONFACTION, MAX_NONFACTION_CAP)
    }

    /** Building-shake intensity multiplier (0–2), tunable live from the menu "Building shake" slider. */
    var buildingShakeMultiplier = 1.0

    /**
     * Weapon-drop multiplier (XMP + Ultra-Strike yield per hack), tunable live from the menu "Weapon drops"
     * slider. `1.0` = the base [DropRates] rate; defaults to `10.0` so agents hoard firepower fast and flip
     * defended portals — i.e. a very dynamic, weapon-rich sim.
     */
    var weaponDropMultiplier = 10.0

    // Combat dynamism (0 = realistic/tanky shields, 1 = portals flip very easily). Drives the live
    // gameplay mitigation cap; tunable from the menu "Combat" slider. Leans dynamic by default — this is
    // an AI-vs-AI sim, dynamism matters more than realism. (The authentic 95% cap stays in IngressFacts.)
    var combatDynamism = 0.75

    /** Gameplay shield/link mitigation cap for the current dynamism: higher dynamism → lower cap → flips. */
    fun maxMitigation(): Int =
        (IngressFacts.MITIGATION_CAP_PCT - combatDynamism * 75.0).toInt().coerceIn(15, IngressFacts.MITIGATION_CAP_PCT)

    const val apMultiplier = 10

    const val isNpcSwarming = true
    const val npcXmSpawnRatio = 0.2

    val isSoundOn = !HtmlUtil.isLocal()
    const val isPlayInitialSound = false
    const val isSatOn = false

    const val isHighlighActionLimit = true
    const val vectorSmoothCount = 3
    const val shadowBlurCount = 3

    const val comMessageLimit = 8
    const val topAgentsMessageLimit = 8

    val ticksPerCheckpoint = Time.secondsToTicks(300)
    val ticksPerCycle = Time.secondsToTicks(1800)

    const val pathResolution = 10
}
