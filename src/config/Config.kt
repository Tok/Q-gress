package config

object Config {
    val startPortals = 5
    val startFrogs = 20
    val startSmurfs = 20

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
