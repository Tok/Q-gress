package ai.net

import kotlinx.browser.window
import system.ui.Bootstrap

/**
 * Persists/loads a trained net for the live game (PLAN Phase 6.2). The **active** genome is whatever's been
 * [save]d to `localStorage` (e.g. a freshly trained or pasted-in winner), falling back to the baked
 * [Champion]. So the **Neural net** driver always has a real net to install, and an improved one survives a
 * reload once saved. Headless ([Bootstrap] off) there's no `localStorage`, so it just yields the champion.
 */
object NetStore {
    private const val KEY = "qgress.net.genome"

    /** Persist a genome JSON (no-op headless). Validate with [GenomeIO.decode] before calling. */
    fun save(genomeJson: String) {
        if (Bootstrap.isNotRunningInBrowser()) return
        window.localStorage.setItem(KEY, genomeJson)
    }

    /** Drop any saved override, reverting to the baked [Champion]. */
    fun clear() {
        if (Bootstrap.isRunningInBrowser()) window.localStorage.removeItem(KEY)
    }

    /** The active genome JSON: the saved override if present, else the baked [Champion]. */
    fun activeJson(): String {
        if (Bootstrap.isNotRunningInBrowser()) return Champion.JSON
        return window.localStorage.getItem(KEY) ?: Champion.JSON
    }

    /** Decode the active genome into a [Net]; falls back to the baked [Champion] if a saved override is corrupt. */
    fun loadNet(): Net = runCatching { GenomeIO.decode(activeJson()) }.getOrElse { GenomeIO.decode(Champion.JSON) }

    /**
     * The baked champion for a specific [arch] from the [ChampionLibrary] (the onboarding per-arch pick /
     * random-arch NN matches) — ignores the single saved override, which is tied to one arch. Falls back to
     * the default champion if that arch's genome is somehow corrupt.
     */
    fun loadNet(arch: NetArch): Net =
        runCatching { GenomeIO.decode(ChampionLibrary.jsonFor(arch)) }.getOrElse { GenomeIO.decode(ChampionLibrary.defaultJson()) }
}
