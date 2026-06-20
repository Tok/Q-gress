package config

import util.HtmlUtil

object Styles {
    val fieldTransparency = 0.4

    val isDrawAgentRange = false
    val isDrawDestination = false

    val isDrawNoiseMap = false
    val isDrawPortalNames = true
    val isDrawCom = true
    val isDrawResoLevels = false
    val isDrawTopAgents = true

    val use3DBuildings = true

    fun vectorStyle() = if (HtmlUtil.isShowSatelliteMap()) VectorStyle.SQUARE else VectorStyle.CIRCLE
    fun isColorVectors() = !HtmlUtil.isShowSatelliteMap()
    val isDrawObstructedVectors = false

    //settings with impact on performance
    val isDrawResoLineGradient = true
    val isFillMuDisplay = true
}
