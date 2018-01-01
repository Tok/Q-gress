package agent.action


data class ActionItem(val text: String, val letter: String, val durationSeconds: Int) {
    companion object {
        val MOVE = ActionItem("moving", " ", 1)
        val WAIT = ActionItem("waiting", "o", 1)
        val HACK = ActionItem("hacking", " !", 5)
        val GLYPH = ActionItem("glyphing", "?", 40)
        val ATTACK = ActionItem("attacking", "-", 5)
        val DEPLOY = ActionItem("deploying", "+", 10)
        val LINK = ActionItem("linking", " |", 10)
        fun values() = listOf<ActionItem>(MOVE, WAIT, HACK, GLYPH, ATTACK, DEPLOY, LINK)
    }
}
