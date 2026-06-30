package util

// JS shell: forward diagnostics to the browser console. See the commonMain `expect object Log`.
actual object Log {
    actual fun warn(message: String) {
        console.warn(message)
    }
}
