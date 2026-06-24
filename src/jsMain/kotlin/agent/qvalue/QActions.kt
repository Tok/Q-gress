package agent.qvalue

import agent.action.ActionItem
import util.HtmlUtil

object QActions {
    // Icons use the high-res (supersampled) render so they stay crisp in the tuning list — the 1× icon
    // is ~12px and blurs when the browser scales it for HiDPI; we display it small via CSS. The icon is
    // UI-only, so headless (Node tests / future headless matches) we pass null — the hi-res icon maps are
    // empty without a browser and resolving one would throw.
    private fun icon(item: ActionItem) = if (HtmlUtil.isRunningInBrowser()) ActionItem.getHiResIcon(item) else null

    // anywhere
    val MOVE_ELSEWHERE = QValue("move", 0.01, "move elsewhere", icon(ActionItem.MOVE))
    val RECRUIT = QValue("recruit", 0.0005, "recruit agents", icon(ActionItem.RECRUIT))
    val EXPLORE = QValue("explore", 0.0002, "explore portals", icon(ActionItem.EXPLORE))
    val RECYCLE = QValue("recycle", 1.0, "recycle items", icon(ActionItem.RECYCLE))
    val RECHARGE = QValue("recharge", 1.0, "recharge portals", icon(ActionItem.RECHARGE))

    // at all portals
    val HACK = QValue("hack", 1.0, "hack portal", icon(ActionItem.HACK))
    val GLYPH = QValue("glyph", 1.0, "glyph portal", icon(ActionItem.GLYPH))

    // at friendly portals
    val DEPLOY = QValue("deploy", 1.0, "deploy portal", icon(ActionItem.DEPLOY))

    // at neutral portals
    val CAPTURE = QValue("capture", 1.0, "capture portal", icon(ActionItem.CAPTURE))

    // at friendly portals
    val LINK = QValue("link", 1.0, "create link", icon(ActionItem.LINK))

    // at enemy portals
    val ATTACK = QValue("attack", 1.0, "attack portals", icon(ActionItem.ATTACK))
    val VIRUS = QValue("virus", 1.0, "use virus", icon(ActionItem.VIRUS))

    fun values() = listOf(
        MOVE_ELSEWHERE,
        EXPLORE,
        RECRUIT,
        ATTACK,
        VIRUS,
        LINK,
        DEPLOY,
        CAPTURE,
        HACK,
        GLYPH,
        RECHARGE,
        RECYCLE,
    )
}
