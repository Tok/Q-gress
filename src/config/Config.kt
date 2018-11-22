package config

import agent.Faction
import util.HtmlUtil

object Config {
    val startPortals = if (HtmlUtil.isLocal()) 8 else 8
    val startFrogs = if (HtmlUtil.isLocal()) 4 else 4
    val startSmurfs = if (HtmlUtil.isLocal()) 4 else 4

    val maxFrogs = 100
    val maxSmurfs = 100
    val maxNonFaction = maxFrogs + maxSmurfs
    fun maxFor(faction: Faction) = when (faction) {
        Faction.ENL -> maxFrogs
        Faction.RES -> maxSmurfs
        else -> maxNonFaction
    }

    const val apMultiplier = 10

    val isNpcSwarming = true

    val isSoundOn = true
    val isSatOn = false

    val isAutostart = true
    val isHighlighActionLimit = true
    val vectorSmoothCount = 8
    val shadowBlurCount = 3

    val comMessageLimit = 8
    val topAgentsMessageLimit = 8


    const val pathResolution = 10
}
