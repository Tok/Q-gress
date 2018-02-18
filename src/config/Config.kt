package config

object Config {
    val seed = 111

    val startPortals = 13
    val startFrogs = 50
    val startSmurfs = 50

    val maxFrogs = 100
    val maxSmurfs = 100
    val startNonFaction = 500 // ((maxFrogs + maxSmurfs) * 0.5 / Constants.phi).toInt()

    val isAutostart = true
    val vectorSmoothCount = 5
    val shadowBlurCount = 3

    val comMessageLimit = 8
    val topAgentsMessageLimit = 5

    val localLocation = "http://localhost:63342/"
    val localToken = "Qgress/"

    val location = "https://tok.github.io/"
    val token = "Q-gress/"
}
