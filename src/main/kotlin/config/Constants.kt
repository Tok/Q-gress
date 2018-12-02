package config

import util.HtmlUtil

object Constants {
    const val phi = 1.61803398874989484820
    const val tau = 2.0 * kotlin.math.PI

    private const val localLocation = "http://localhost:63342/"
    private const val localToken = "Qgress/"
    private const val location = "https://tok.github.io/"
    private const val token = "Q-gress/"

    fun token() = if (HtmlUtil.isLocal()) localToken else token
    fun targetUrl() = if (HtmlUtil.isLocal()) localLocation else location
}
