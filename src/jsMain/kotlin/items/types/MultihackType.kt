package items.types

enum class MultihackType(val abbr: String, val color: String, val order: Int, val additionalHacks: Int, val xmCost: Int, val recyclingXm: Int) {
    COMMON("MH", "8cffbf", 2, 4, 400, 40),
    RARE("RMH", "73a8ff", 1, 8, 800, 80),
    VERY_RARE("VRMH", "b08cff", 0, 12, 1000, 100);
}
