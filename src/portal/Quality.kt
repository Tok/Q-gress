package portal

enum class Quality(val chance: Double, val addLevels: Int) {
    BEST(0.62, +1),
    GOOD(0.5, 0),
    MORE(0.38, -1)
}
