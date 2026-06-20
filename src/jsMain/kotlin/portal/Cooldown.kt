package portal

enum class Cooldown(val seconds: Int) {
    BURNOUT(14400), FIVE(300), THREE(240), TWO(120), ONE(60), HALF(30), MIN(10), NONE(0);

    fun isHackable() = this == NONE
    companion object {
        fun valueOf(seconds: Int): Cooldown = values().findLast { it.seconds >= seconds } ?: NONE
    }
}
