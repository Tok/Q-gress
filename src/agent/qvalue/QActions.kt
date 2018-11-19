package agent.qvalue

import agent.action.ActionItem

object QActions {
    //anywhere
    val MOVE_ELSEWHERE = QValue("move", 0.005, "move elsewhere", ActionItem.getIcon(ActionItem.MOVE))
    val RECYCLE = QValue("recycle", 0.5, "recycle items", ActionItem.getIcon(ActionItem.RECYCLE))
    val RECHARGE = QValue("recharge", 0.5, "recharge portal", ActionItem.getIcon(ActionItem.RECHARGE))

    //at all portals
    val HACK = QValue("hack", 0.5, "hack portal", ActionItem.getIcon(ActionItem.HACK))

    //at friendly or neutral portals
    val DEPLOY = QValue("deploy", 0.5, "deploy portal", ActionItem.getIcon(ActionItem.DEPLOY))

    //at friendly portals
    val LINK = QValue("link", 0.5, "create link", ActionItem.getIcon(ActionItem.LINK))

    //at enemy portals
    val ATTACK = QValue("attack", 0.5, "attack portal", ActionItem.getIcon(ActionItem.ATTACK))

    fun values() = listOf(
            MOVE_ELSEWHERE,
            RECYCLE,
            RECHARGE,
            HACK,
            DEPLOY,
            LINK,
            ATTACK
    )
}
