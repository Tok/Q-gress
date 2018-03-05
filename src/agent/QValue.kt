package agent

data class QValue(val name: String, val weight: Double) {
    val sliderId = name + "Slider"
    companion object {
        //at neutral portal
        val CAPTURE = QValue(". capture portal", 0.4)
        val HACK = QValue(". hack portal", 0.5)
        val RECYCLE = QValue(". recycle items", 0.3)
        val RECHARGE = QValue(". recharge portal", 0.2)

        //at friendly portal
        val DEPLOY = QValue("+ deploy portal", 1.0)
        val LINK = QValue("+ create link", 0.5)

        //at enemy portal
        val ATTACK = QValue("* attack portal", 1.0)

        //general
        val ATTACK_SOMEHERE = QValue("-> move to enemy portal..", 0.003)
        val ATTACK_CLOSE = QValue("--> ..closest", 1.0)
        val ATTACK_LINKS = QValue("--> ..most linked", 0.5)
        val ATTACK_WEAK = QValue("--> ..weakest", 0.8)

        val MOVE_ELSEWHERE = QValue("-> move to another portal..", 0.005)
        val MOVE_TO_FRIENDLY = QValue("--> ..friendly", 0.8)
        val MOVE_TO_NEAR = QValue("--> ..nearest", 1.0)
        val MOVE_TO_RANDOM = QValue("--> ..random", 0.5)

        fun values() = listOf(
                CAPTURE, HACK, RECYCLE, RECHARGE,
                DEPLOY, LINK,
                ATTACK,
                ATTACK_SOMEHERE, ATTACK_CLOSE, ATTACK_LINKS, ATTACK_WEAK,
                MOVE_ELSEWHERE, MOVE_TO_NEAR, MOVE_TO_FRIENDLY, MOVE_TO_RANDOM
        )
    }
}
