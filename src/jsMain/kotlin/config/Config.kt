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

    // Anti-snowball recruiting (Balance.recruitFactor): the LARGER faction recruits less, the SMALLER more,
    // so team sizes self-correct instead of the leader running away (recruiting was the dominant snowball).
    // The recruit chance is multiplied by (enemyRoster+1)/(myRoster+1), clamped to this band.
    var recruitFactorMin = 0.3 // a much-larger faction recruits at ≥30% of base
    var recruitFactorMax = 3.0 // a much-smaller faction recruits at ≤300% of base

    // Min resonators an owned portal needs before it can link out (Portal.isLinkable). Low (Ingress-like:
    // any owned portal links) so fields form even on a fast-flipping board; raise toward 8 for tankier,
    // harder-to-field play. The field-formation lever.
    var linkMinResos = 1

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

    // Combat dynamism (0 = realistic/tanky shields, slow board … 1 = portals flip very easily). The SINGLE
    // combat knob (menu "Combat dynamics" slider) — it drives shield mitigation, weapon-drop rate, how eagerly
    // agents attack, and the underdog comeback. Default ≈0.45: measured (over seeded SimRunner matches) as the
    // sweet spot — portals are tanky enough to HOLD + field + score, yet the board still churns and MU swings
    // (true dynamism = MU volatility, not just fast capturing). Higher → churnier but fields rarely survive to
    // score. (The authentic 95% mitigation cap stays in IngressFacts.)
    var combatDynamism = 0.45

    /** Gameplay shield/link mitigation cap for the current dynamism: higher dynamism → lower cap → flips. */
    fun maxMitigation(): Int =
        (IngressFacts.MITIGATION_CAP_PCT - combatDynamism * 75.0).toInt().coerceIn(15, IngressFacts.MITIGATION_CAP_PCT)

    /** Weapon-drop multiplier (XMP + Ultra-Strike yield per hack) vs the base [DropRates] rate: `1×` … `20×`
     *  as dynamism rises, so a dynamic sim hands out the firepower needed to flip defended portals. */
    fun weaponDropMultiplier(): Double = 1.0 + combatDynamism * 19.0

    /** XMPs an agent hoards before committing to an assault: `30` (cautious) … `8` (trigger-happy) as
     *  dynamism rises — lower means assaults start sooner and portals flip more often. */
    fun attackXmpThreshold(): Int = (30 - combatDynamism * 22).toInt().coerceAtLeast(8)

    /** Comeback bonus (`Balance.attackBoost`): the faction behind on portals deals up to this fraction MORE
     *  resonator damage at a full deficit, so a losing side can turn the board. Scales with dynamism. */
    fun comebackAttackBonus(): Double = combatDynamism

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

    // Headless flow-field compute (PLAN Phase 6.1 / the SimRunner). Off by default: in the browser fields
    // are computed async (PathUtil.computeFieldAsync) and in plain Node unit tests we skip them entirely
    // (agents bee-line). A headless match flips this on so Portal/NonFaction compute fields synchronously
    // (PathUtil.computeFieldSync) — deterministic pathfinding without the coroutine event loop. Requires
    // World.grid to be initialised (the match harness sets it up first).
    var headlessFieldCompute = false
}
