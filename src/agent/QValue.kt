package agent

data class QValue(val name: String, val factor: Double) {
    val sliderId = name + "Slider"
    val unitLabel = "% "
    companion object {
        val CAPTURE = QValue("Capture", 1.0)
        val HACK = QValue("Hack", 1.0)
        val DEPLOY = QValue("Deploy", 1.0)
        val LINK = QValue("Link", 1.0)
        val RECHARGE = QValue("Recharge", 0.2)
        val RECYCLE = QValue("Recycle", 0.2)
        val ATTACK = QValue("Attack", 1.0)

        val ATTACK_SOMEHERE = QValue("Attack Another Portal", 0.01)
        val ATTACK_CLOSE = QValue("- Closest", 1.0)
        val ATTACK_LINKS = QValue("- Most Linked", 1.0)
        val ATTACK_WEAK = QValue("- Weakest", 1.0)

        val MOVE_ELSEWHERE = QValue("Move To Another Portal", 0.01)
        val MOVE_TO_FRIENDLY = QValue("- Friendly", 1.0)
        val MOVE_TO_NEAR = QValue("- Nearest", 1.0)
        val MOVE_TO_RANDOM = QValue("- Random", 1.0)
        fun values() = listOf(
                CAPTURE, HACK, DEPLOY, LINK,
                ATTACK, RECHARGE, RECYCLE,
                ATTACK_SOMEHERE, ATTACK_CLOSE, ATTACK_LINKS, ATTACK_WEAK,
                MOVE_ELSEWHERE, MOVE_TO_NEAR, MOVE_TO_FRIENDLY, MOVE_TO_RANDOM
        )
    }
}
