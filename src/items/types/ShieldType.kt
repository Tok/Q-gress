package items.types

enum class ShieldType(val abbr: String, val color: String, val mitigation: Int, val stickiness: Int, val deployCostXm: Int, val roll: Int) {
    COMMON("CS", "8cffbf", 30, 0, 250, 50),
    RARE("RS", "73a8ff", 40, 15, 500, 500),
    VERY_RARE("VRS", "b08cff", 60, 45, 1000, 1500),
    AXA("AXA", "b08cff", 70, 80, 1000, 1500)
}
