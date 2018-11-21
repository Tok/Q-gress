package config

object Styles {
    enum class VectorStyle {
        CIRCLE, SQUARE
    }

    val fieldTransparency = 0.4

    val isDrawAgentRange = false
    val isDrawDestination = false

    val isDrawNoiseMap = false
    val isDrawPortalNames = true
    val isDrawCom = true
    val isDrawResoLevels = false
    val isDrawTopAgents = true

    val use3DBuildings = true

    val vectorStyle = VectorStyle.SQUARE
    val useColorVectors = false
    val isDrawObstructedVectors = false

    //settings with impact on performance
    val isDrawResoLineGradient = true
    val isFillMuDisplay = true
}
