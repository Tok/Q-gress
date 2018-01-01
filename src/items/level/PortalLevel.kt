package items.level

enum class PortalLevel(val value: Int, val display: String, val color: String) {
    ZERO(0, " ", "#000000"),
    ONE(1, "1", "#FECE5A"),
    TWO(2, "2", "#FFA630"),
    THREE(3, "3", "#FF7315"),
    FOUR(4, "4", "#E40000"),
    FIVE(5, "5", "#FD2992"),
    SIX(6, "6", "#EB26CD"),
    SEVEN(7, "7", "#C124E0"),
    EIGHT(8, "8", "#9627F4");

    override fun toString() = display

    companion object {
        fun findByValue(value: Int): PortalLevel = PortalLevel.values().find { it.value == value }!!
    }
}
