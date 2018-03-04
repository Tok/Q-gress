package agent.action


data class ActionItem(val text: String, val letter: String, val durationSeconds: Int, val qName: String) {
    companion object {
        val MOVE = ActionItem("moving", " ", 1, "Move")
        val WAIT = ActionItem("waiting", "o", 1, "Wait")
        val RECHARGE = ActionItem("recharge", "r", 1, "Recharge")
        val RECYCLE = ActionItem("recycle", "c", 1, "Recycle")
        val HACK = ActionItem("hacking", " !", 5, "Hack")
        val GLYPH = ActionItem("glyphing", "?", 40, "Glyph")
        val ATTACK = ActionItem("attacking", "-", 5, "Attack")
        val DEPLOY = ActionItem("deploying", "+", 10, "Deploy")
        val LINK = ActionItem("linking", " |", 10, "Link")
        fun values() = listOf(MOVE, WAIT, HACK, GLYPH, ATTACK, DEPLOY, LINK)
    }
}
