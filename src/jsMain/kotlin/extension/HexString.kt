package extension;

fun Int.toHexString(): String {
    check(this >= 0)
    check(this <= 0xFF)
    val hexChars = "0123456789ABCDEF"
    return hexChars[this ushr 4].toString() +
            hexChars[this and 0x0F].toString()
}
