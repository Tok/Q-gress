package agent.qvalue

import agent.action.ActionItem

object QActions {
    //anywhere
    val MOVE_ELSEWHERE = QValue("move", 0.01, "move elsewhere", ActionItem.getIcon(ActionItem.MOVE))
    val RECYCLE = QValue("recycle", 1.0, "recycle items", ActionItem.getIcon(ActionItem.RECYCLE))
    val RECHARGE = QValue("recharge", 1.0, "recharge portal", ActionItem.getIcon(ActionItem.RECHARGE))
    val RECRUIT = QValue("recruit", 0.005, "recruit agents", ActionItem.getIcon(ActionItem.RECRUIT))

    //at all portals
    val HACK = QValue("hack", 0.5, "hack portal", ActionItem.getIcon(ActionItem.HACK))

    //at friendly portals
    val DEPLOY = QValue("deploy", 1.0, "deploy portal", ActionItem.getIcon(ActionItem.DEPLOY))

    //at neutral portals
    val CAPTURE = QValue("capture", 50.0, "capture portal", ActionItem.getIcon(ActionItem.DEPLOY))

    //at friendly portals
    val LINK = QValue("link", 5.0, "create link", ActionItem.getIcon(ActionItem.LINK))

    //at enemy portals
    val ATTACK = QValue("attack", 1.0, "attack portal", ActionItem.getIcon(ActionItem.ATTACK))

    fun values() = listOf(
            MOVE_ELSEWHERE,
            RECYCLE,
            RECHARGE,
            RECRUIT,
            HACK,
            DEPLOY,
            CAPTURE,
            LINK,
            ATTACK
    )
}
