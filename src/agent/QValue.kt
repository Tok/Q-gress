package agent

data class QValue(val name: String) {
    val sliderId = name + "Slider"
    val unitLabel = "% " + name
    companion object {
        val HACK = QValue("Hack")
        val DEPLOY = QValue("Deploy")
        val LINK = QValue("Link")
        val ATTACK = QValue("Attack")
        val RECHARGE = QValue("Recharge")
        val RECYCLE = QValue("Recycle")
        val CAPTURE = QValue("Capture")
        val ATTACK_CLOSE = QValue("Attack Close")
        val ATTACK_LINKS = QValue("Attack Links")
        val ATTACK_WEAK = QValue("Attack Weak")
        val MOVE_TO_FRIENDLY = QValue("Move To Friendly")
        val MOVE_TO_NEAR = QValue("Move To Near")
        val MOVE_TO_RANDOM = QValue("Move To Random")
        fun values() = listOf(
                HACK, DEPLOY, LINK, ATTACK,
                RECHARGE, RECYCLE, CAPTURE,
                ATTACK_CLOSE, ATTACK_LINKS, ATTACK_WEAK,
                MOVE_TO_FRIENDLY, MOVE_TO_NEAR, MOVE_TO_RANDOM
        )
    }
}
