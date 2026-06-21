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

    var startPortals = 5 // initial portal count (chosen at onboarding — the "portal density")
    var quickStart = false // onboarding option: start with a full roster + AP so the early game moves
    fun startFrogs() = if (quickStart) 8 else minFrogs
    fun startSmurfs() = if (quickStart) 8 else minSmurfs
    fun initialAp() = if (quickStart) 2000000 else 0

    private const val maxNonFaction = 300
    fun maxFor(faction: Faction? = null) = when (faction) {
        Faction.ENL -> maxFrogs
        Faction.RES -> maxSmurfs
        else -> maxNonFaction
    }

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
    const val useOffscreenEdgeDestinations = false
}
