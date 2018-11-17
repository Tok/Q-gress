package config

object Config {
    val startPortals = 8
    val startFrogs = 50
    val startSmurfs = 50

    val maxFrogs = 100
    val maxSmurfs = 100
    val startNonFaction = ((startFrogs + startSmurfs) * Constants.phi).toInt()

    val isAutostart = true
    val isHighlighActionLimit = true
    val vectorSmoothCount = 3
    val shadowBlurCount = 3

    val comMessageLimit = 8
    val topAgentsMessageLimit = 8
}
