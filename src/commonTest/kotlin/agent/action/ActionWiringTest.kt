package agent.action

import agent.action.cond.Attacker
import agent.action.cond.Deployer
import agent.action.cond.Glypher
import agent.action.cond.Hacker
import agent.action.cond.Linker
import agent.action.cond.Recharger
import agent.action.cond.Recruiter
import agent.action.cond.Recycler
import agent.action.cond.Refactorer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards each [agent.action.cond.ConditionalAction] against being wired to the wrong [ActionItem] — the bug
 * where [Recharger] (recharges portals) was set to RECYCLE (the power-cube action), so it showed the wrong
 * icon. A regression here means an action's label/icon no longer matches what it does.
 */
class ActionWiringTest {
    @Test
    fun eachActionUsesItsMatchingActionItem() {
        assertEquals(ActionItem.ATTACK, Attacker.actionItem)
        assertEquals(ActionItem.DEPLOY, Deployer.actionItem)
        assertEquals(ActionItem.GLYPH, Glypher.actionItem)
        assertEquals(ActionItem.HACK, Hacker.actionItem)
        assertEquals(ActionItem.LINK, Linker.actionItem)
        assertEquals(ActionItem.RECHARGE, Recharger.actionItem, "Recharger recharges portals → RECHARGE, not RECYCLE")
        assertEquals(ActionItem.RECRUIT, Recruiter.actionItem)
        assertEquals(ActionItem.RECYCLE, Recycler.actionItem, "Recycler spends power cubes for XM → RECYCLE")
        assertEquals(ActionItem.VIRUS, Refactorer.actionItem)
    }
}
