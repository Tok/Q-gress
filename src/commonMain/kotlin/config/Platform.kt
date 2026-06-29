package config

/**
 * The tiny platform seam for the (otherwise pure) `config` core: the few host facts `config` needs that
 * only the browser shell can answer. The jsMain `actual` delegates to `system.ui.Bootstrap` + the window;
 * headless callers get the fallbacks. Mirrors the `util.Rng` `expect freshSeed()` ↔ `FreshSeed` actual.
 */
expect object Platform {
    /** Running in a real browser (vs. the Node test runtime / headless eval). */
    fun isBrowser(): Boolean

    /** Running against the local dev host (drives dev token / target URL + muting). */
    fun isLocal(): Boolean

    /** Browser window inner width, or [fallback] when headless. */
    fun windowWidth(fallback: Int): Int

    /** Browser window inner height, or [fallback] when headless. */
    fun windowHeight(fallback: Int): Int
}
