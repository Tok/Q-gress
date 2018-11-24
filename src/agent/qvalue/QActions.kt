package agent.qvalue

import agent.action.ActionItem

object QActions {
    //anywhere
    val MOVE_ELSEWHERE = QValue("move", 0.01, "move elsewhere", ActionItem.getIcon(ActionItem.MOVE))
    val RECRUIT = QValue("recruit", 0.001, "recruit agents", ActionItem.getIcon(ActionItem.RECRUIT))
    val RECYCLE = QValue("recycle", 1.0, "recycle items", ActionItem.getIcon(ActionItem.RECYCLE))
    val RECHARGE = QValue("recharge", 1.0, "recharge portal", ActionItem.getIcon(ActionItem.RECHARGE))

    //at all portals
    val HACK = QValue("hack", 1.0, "hack portal", ActionItem.getIcon(ActionItem.HACK))
    val GLYPH = QValue("glyph", 1.0, "glyph portal", ActionItem.getIcon(ActionItem.GLYPH))

    //at friendly portals
    val DEPLOY = QValue("deploy", 1.0, "deploy portal", ActionItem.getIcon(ActionItem.DEPLOY))

    //at neutral portals
    val CAPTURE = QValue("capture", 1.0, "capture portal", ActionItem.getIcon(ActionItem.DEPLOY))

    //at friendly portals
    val LINK = QValue("link", 1.0, "create link", ActionItem.getIcon(ActionItem.LINK))

    //at enemy portals
    val ATTACK = QValue("attack", 1.0, "attack portal", ActionItem.getIcon(ActionItem.ATTACK))

    fun values() = listOf(
            MOVE_ELSEWHERE,
            RECYCLE,
            RECHARGE,
            RECRUIT,
            HACK,
            GLYPH,
            DEPLOY,
            CAPTURE,
            LINK,
            ATTACK
    )
}
