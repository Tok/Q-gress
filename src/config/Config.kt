package config

object Config {
    val seed = 111

    val startPortals = 10
    val startFrogs = 10
    val startSmurfs = 10

    val maxFrogs = 500
    val maxSmurfs = 500
    val startNonFaction = 300 // ((maxFrogs + maxSmurfs) * 0.5 / Constants.phi).toInt()

    val isAutostart = true
    val vectorSmoothCount = 5
    val shadowBlurCount = 3

    val comMessageLimit = 8
    val topAgentsMessageLimit = 5
}
