package agent.qvalue

object QActions {
    // anywhere
    val MOVE_ELSEWHERE = QValue("move", 0.01, "move elsewhere")

    // Retired sliders: RECRUIT and EXPLORE — both are now faction-neutral SYSTEM PROCESSES in system/Cycle
    // (recruiting via Recruiter.expectedRecruits → recruitmentTick; portal discovery via managePortalDensity),
    // not agent-tunable cranks. Both were dull/unfair to tune (recruiting especially was the dominant snowball).
    val RECYCLE = QValue("recycle", 1.0, "recycle items")
    val RECHARGE = QValue("recharge", 1.0, "recharge portals")

    // at all portals
    val HACK = QValue("hack", 1.0, "hack portal")
    val GLYPH = QValue("glyph", 1.0, "glyph portal")

    // at friendly portals — weighted ABOVE hack/attack so agents consolidate captured ground into
    // fully-deployed, linked portals (the path to fields/MU) instead of just hacking + capturing forever.
    // (These still only fire when actually possible — a deployable reso to hand, a key + an uncrossed target.)
    val DEPLOY = QValue("deploy", 2.0, "deploy portal")

    // at neutral portals
    val CAPTURE = QValue("capture", 1.5, "capture portal")

    // at friendly portals — the field-maker; weighted highest so a linkable portal actually gets linked
    val LINK = QValue("link", 3.0, "create link")

    // at enemy portals
    val ATTACK = QValue("attack", 1.0, "attack portals")
    val VIRUS = QValue("virus", 1.0, "use virus")

    fun values() = listOf(
        MOVE_ELSEWHERE,
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
