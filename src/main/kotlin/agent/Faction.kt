package agent

import util.Util

enum class Faction(val abbr: String, val nickName: String, val color: String, val fieldStyle: String) {
    ENL("ENL", "Frog", "#03DC03", "rgba(3, 220, 3, "),
    RES("RES", "Smurf", "#0088FF", "rgba(0, 136, 255, ");

    fun isEnemy(faction: Faction): Boolean = (faction == ENL && this == RES) ||
            (faction == RES && this == ENL)

    fun enemy(): Faction = when (this) {
        ENL -> RES
        RES -> ENL
    }

    companion object {
        fun fromString(s: String?): Faction? = when (s?.toUpperCase()) {
            "RES" -> RES
            "ENL" -> ENL
            else -> null
        }

        fun all() = listOf(ENL, RES)
        fun random() = if (Util.random() < 0.5) ENL else RES
    }
}
