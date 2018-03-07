package config

import util.HtmlUtil

object Constants {
    val phi = 1.61803398874989484820
    val tau = 2.0 * kotlin.math.PI

    val hexChars = "0123456789ABCDEF"

    private val localLocation = "http://localhost:63342/"
    private val localToken = "Qgress/"
    private val location = "https://tok.github.io/"
    private val token = "Q-gress/"

    fun token() = if (HtmlUtil.isLocal()) localToken else token
    fun targetUrl() = if (HtmlUtil.isLocal()) localLocation else location
}
