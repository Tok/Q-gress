package agent

import util.Util

enum class Faction(val abbr: String, val nickName: String, val color: String, val fieldStyle: String) {
    NONE("NONE", "None", "#FFFFFF", "rgba(255, 255, 255, "),
    ENL("ENL", "Frog", "#03DC03", "rgba(3, 220, 3, "),
    RES("RES", "Smurf", "#0088FF", "rgba(0, 136, 255, ");

    companion object {
        fun createRandom() = if (Util.random() < 0.5) ENL else RES
    }
}
