package agent

import Canvas
import agent.action.ActionItem

data class QValue(val id: String, val description: String, val weight: Double, val icon: Canvas? = null) {
    val sliderId = id + "Slider"
    companion object {
        //at neutral portal
        val CAPTURE = QValue("capture", "capture", 0.4, ActionItem.getIcon(ActionItem.DEPLOY))
        val HACK = QValue("hack", "hack portal", 0.5, ActionItem.getIcon(ActionItem.HACK))
        val RECYCLE = QValue("recycle", "recycle items", 0.3, ActionItem.getIcon(ActionItem.RECYCLE))
        val RECHARGE = QValue("recharge", "recharge portal", 0.2, ActionItem.getIcon(ActionItem.RECHARGE))

        //at friendly portal
        val DEPLOY = QValue("deploy", "deploy portal", 1.0, ActionItem.getIcon(ActionItem.DEPLOY))
        val LINK = QValue("link", "create link", 0.5, ActionItem.getIcon(ActionItem.LINK))

        //at enemy portal
        val ATTACK = QValue("attack", "attack portal", 1.0, ActionItem.getIcon(ActionItem.ATTACK))

        //general
        val ATTACK_SOMEHERE = QValue("attackSomething", "go attack enemy..", 0.020, ActionItem.getIcon(ActionItem.MOVE))
        val ATTACK_CLOSE = QValue("attackClosest", "..closest", 1.0)
        val ATTACK_LINKS = QValue("attackMostLinked", "..most linked", 0.5)
        val ATTACK_WEAK = QValue("attackWaekest", "..weakest", 0.8)

        val MOVE_ELSEWHERE = QValue("MoveElsewhere", "move elsewhere..", 0.005, ActionItem.getIcon(ActionItem.MOVE))
        val MOVE_TO_FRIENDLY = QValue("MoveToFriendlyPortal", "..to friendly portal", 0.8)
        val MOVE_TO_NEAR = QValue("MoveToNearestPortal", "..to nearest portal", 1.0)
        val MOVE_TO_RANDOM = QValue("MoveToRandomPortal", "..to random portal", 0.5)

        fun values() = listOf(
                CAPTURE, HACK, RECYCLE, RECHARGE,
                DEPLOY, LINK,
                ATTACK,
                ATTACK_SOMEHERE, ATTACK_CLOSE, ATTACK_LINKS, ATTACK_WEAK,
                MOVE_ELSEWHERE, MOVE_TO_NEAR, MOVE_TO_FRIENDLY, MOVE_TO_RANDOM
        )
    }
}
