package agent.qvalue

object QDestinations {
    val MOVE_TO_RANDOM = QValue("toRandom", 0.1, "random")
    val MOVE_TO_NEAR = QValue("toNear", 0.2, "near")
    val MOVE_TO_UNCAPTURED = QValue("toUncaptured", 1.0, "uncaptured")
    val MOVE_TO_MOST_FRIENDLY = QValue("toMostFriendly", 0.8, "most friendly")
    val MOVE_TO_NEAR_ENEMY = QValue("toNearEnemy", 0.05, "near enemy")
    val MOVE_TO_WEAK_ENEMY = QValue("toWeakEnemy", 0.02, "weak enemy")
    val MOVE_TO_STRONG_ENEMY = QValue("toStrongEnemy", 0.02, "strong enemy")

    fun values() = listOf(
            MOVE_TO_RANDOM, MOVE_TO_NEAR,
            MOVE_TO_UNCAPTURED,
            MOVE_TO_MOST_FRIENDLY,
            MOVE_TO_NEAR_ENEMY, MOVE_TO_WEAK_ENEMY, MOVE_TO_STRONG_ENEMY
    )
}
