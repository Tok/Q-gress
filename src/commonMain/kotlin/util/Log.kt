package util

/**
 * The tiny logging seam for the (otherwise pure) game core: a diagnostic warning channel the core can call
 * without naming a platform console. The jsMain `actual` forwards to the browser `console`; headless/JVM
 * callers get `stderr`. Mirrors the `util.Rng` `expect freshSeed()` ↔ `FreshSeed` actual and `config.Platform`.
 */
expect object Log {
    /** Emit a developer diagnostic (an unexpected-but-recoverable state, e.g. acting before the world is ready). */
    fun warn(message: String)
}
