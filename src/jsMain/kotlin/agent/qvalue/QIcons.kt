package agent.qvalue

import agent.action.ActionItem
import extension.Canvas
import system.ui.Bootstrap

/**
 * The tuning-list icon for each action [QValue] — the UI half split out of the (now pure, commonMain)
 * [QValue]. Uses the high-res (supersampled) render so the small CSS-scaled icon stays crisp; the 1× icon
 * is ~12px and blurs when the browser scales it for HiDPI. Headless (Node tests / future headless matches)
 * every icon is null — the hi-res icon maps are empty without a browser and resolving one would throw.
 * Only [QActions] carry icons; [QDestinations] have none.
 */
object QIcons {
    private val byId: Map<String, Canvas> = if (Bootstrap.isRunningInBrowser()) {
        mapOf(
            QActions.MOVE_ELSEWHERE.id to ActionItem.getHiResIcon(ActionItem.MOVE),
            QActions.RECYCLE.id to ActionItem.getHiResIcon(ActionItem.RECYCLE),
            QActions.RECHARGE.id to ActionItem.getHiResIcon(ActionItem.RECHARGE),
            QActions.HACK.id to ActionItem.getHiResIcon(ActionItem.HACK),
            QActions.GLYPH.id to ActionItem.getHiResIcon(ActionItem.GLYPH),
            QActions.DEPLOY.id to ActionItem.getHiResIcon(ActionItem.DEPLOY),
            QActions.CAPTURE.id to ActionItem.getHiResIcon(ActionItem.CAPTURE),
            QActions.LINK.id to ActionItem.getHiResIcon(ActionItem.LINK),
            QActions.ATTACK.id to ActionItem.getHiResIcon(ActionItem.ATTACK),
            QActions.VIRUS.id to ActionItem.getHiResIcon(ActionItem.VIRUS),
        )
    } else {
        emptyMap()
    }

    /** The tuning-list icon for [value], or null (destinations, or headless). */
    fun iconFor(value: QValue): Canvas? = byId[value.id]
}
