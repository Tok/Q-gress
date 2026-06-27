package system.display

/** Render a Double as a GLSL float literal (always with a decimal point, so `2` becomes `2.0`). */
internal fun Double.glsl(): String {
    val s = this.toString()
    return if (s.contains('.') || s.contains('e') || s.contains('E')) s else "$s.0"
}
