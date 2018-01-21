package config

object Config {
    val seed = 111

    val startPortals = 8
    val startFrogs = 13
    val startSmurfs = 13

    val maxFrogs = 100
    val maxSmurfs = 100
    val startNonFaction = 500 // ((maxFrogs + maxSmurfs) * 0.5 / Constants.phi).toInt()

    val isAutostart = true
    val vectorSmoothCount = 5
    val shadowBlurCount = 3

    val comMessageLimit = 8
    val topAgentsMessageLimit = 5
}
