package agent.action

/**
 * [durationSeconds] is the action's busy window (1 s = 1 tick). It is only consulted where the handler checks
 * [Action.isBusy] — for the cooldown actions (HACK/GLYPH/LINK/RECHARGE/RECYCLE/VIRUS, [Agent.act] line "isBusy() ->")
 * and the move-into-range budget (ATTACK/DEPLOY) and the RECRUIT meeting. MOVE/EXPLORE end on ARRIVAL and WAIT is the
 * 1-tick transitional/initial state — their `durationSeconds` is never read (a nominal value, not a live knob).
 *
 * [isFallback] marks the idle/non-gameplay actions an agent does when there's nothing real to do — RECRUIT (chat up a
 * nearby NPC, capped) and EXPLORE (roam open ground). They're handled alike: no action pill in 3D (a coin-less bobbing
 * agent reads as "just ambling / mid-recruit"); WAIT is NOT a fallback (it shows an empty pill — its glyph is blank).
 *
 * The pure data only; the JS-canvas action-icon prerender lives in the jsMain [ActionIcons].
 */
data class ActionItem(val text: String, val durationSeconds: Int, val qName: String, val isFallback: Boolean = false) {
    companion object {
        val MOVE = ActionItem("moving", 60, "Move") // durationSeconds nominal — MOVE ends on arrival, not the timer
        val WAIT = ActionItem("waiting", 10, "Wait") // durationSeconds nominal — WAIT is the 1-tick transitional state
        val RECHARGE = ActionItem("recharging", 30, "Recharge")

        // The "meeting" after walking up (system-driven — see Recruiter); long so each recruit is a visible
        // commitment, with success rolled once at the end (Config.recruitmentBaseChance scaled to match).
        val RECRUIT = ActionItem("recruiting", 100, "Recruit", isFallback = true)
        val EXPLORE = ActionItem("exploring", 300, "Explore", isFallback = true) // durationSeconds nominal — ends on arrival
        val RECYCLE = ActionItem("recycling", 30, "Recycle")
        val HACK = ActionItem("hacking", 10, "Hack")
        val GLYPH = ActionItem("glyphing", 60, "Glyph")
        val ATTACK = ActionItem("attacking", 15, "Attack")
        val DEPLOY = ActionItem("deploying", 15, "Deploy")
        val CAPTURE = ActionItem("capturing", 15, "Capture")
        val LINK = ActionItem("linking", 30, "Link")
        val VIRUS = ActionItem("refactoring", 15, "Virus")
        fun values() = listOf(MOVE, WAIT, RECHARGE, RECRUIT, EXPLORE, RECYCLE, HACK, GLYPH, ATTACK, DEPLOY, CAPTURE, LINK, VIRUS)
    }
}
