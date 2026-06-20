package portal

enum class Quality(val chance: Double, val addLevels: Int) {
    BEST(0.1, +2),
    TOP(0.3, +1),
    GOOD(0.5, 0),
    MORE(0.7, -1)
}
