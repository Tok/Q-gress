package util

// JS: a fresh 32-bit seed from Math.random (| 0 truncates to a signed 32-bit int).
actual fun freshSeed(): Int = js("(Math.random() * 4294967296) | 0") as Int
