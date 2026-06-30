package config

// JVM (test-only target): headless host facts. Exists so commonMain compiles + runs under jvmTest/Kover.
// There is no browser/window here, so report not-a-browser, not-local, and fall back to the caller's defaults.
actual object Platform {
    actual fun isBrowser(): Boolean = false
    actual fun isLocal(): Boolean = false
    actual fun windowWidth(fallback: Int): Int = fallback
    actual fun windowHeight(fallback: Int): Int = fallback
}
