package agent.qvalue

import agent.action.ActionItem

object QActions {
    // Icons use the high-res (supersampled) render so they stay crisp in the tuning list — the 1× icon
    // is ~12px and blurs when the browser scales it for HiDPI; we display it small via CSS.
    // anywhere
    val MOVE_ELSEWHERE = QValue("move", 0.01, "move elsewhere", ActionItem.getHiResIcon(ActionItem.MOVE))
    val RECRUIT = QValue("recruit", 0.0005, "recruit agents", ActionItem.getHiResIcon(ActionItem.RECRUIT))
    val EXPLORE = QValue("explore", 0.0002, "explore portals", ActionItem.getHiResIcon(ActionItem.EXPLORE))
    val RECYCLE = QValue("recycle", 1.0, "recycle items", ActionItem.getHiResIcon(ActionItem.RECYCLE))
    val RECHARGE = QValue("recharge", 1.0, "recharge portals", ActionItem.getHiResIcon(ActionItem.RECHARGE))

    // at all portals
    val HACK = QValue("hack", 1.0, "hack portal", ActionItem.getHiResIcon(ActionItem.HACK))
    val GLYPH = QValue("glyph", 1.0, "glyph portal", ActionItem.getHiResIcon(ActionItem.GLYPH))

    // at friendly portals
    val DEPLOY = QValue("deploy", 1.0, "deploy portal", ActionItem.getHiResIcon(ActionItem.DEPLOY))

    // at neutral portals
    val CAPTURE = QValue("capture", 1.0, "capture portal", ActionItem.getHiResIcon(ActionItem.CAPTURE))

    // at friendly portals
    val LINK = QValue("link", 1.0, "create link", ActionItem.getHiResIcon(ActionItem.LINK))

    // at enemy portals
    val ATTACK = QValue("attack", 1.0, "attack portals", ActionItem.getHiResIcon(ActionItem.ATTACK))

    fun values() = listOf(
        MOVE_ELSEWHERE,
        EXPLORE,
        RECRUIT,
        ATTACK,
        LINK,
        DEPLOY,
        CAPTURE,
        HACK,
        GLYPH,
        RECHARGE,
        RECYCLE,
    )
}
