package util

// JVM (test-only target): diagnostics to stderr. Exists so commonMain compiles + runs under jvmTest/Kover.
actual object Log {
    actual fun warn(message: String) {
        System.err.println(message)
    }
}
