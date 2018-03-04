package agent

data class QValue(val name: String) {
    val sliderId = name + "Slider"
    val unitLabel = "% " + name
    companion object {
        val HACK = QValue("Hack")
        val DEPLOY = QValue("Deploy")
        val LINK = QValue("Link")
        val ATTACK = QValue("Attack")
        fun values() = listOf(HACK, DEPLOY, LINK, ATTACK)
    }
}
