package system.audio

import kotlinx.browser.localStorage
import util.HtmlUtil

/**
 * Persists the master-volume level + mute intent to `localStorage`, so they survive the page reloads on the
 * title→onboarding→world-gen path (each re-inits [SoundUtil], losing in-memory state). Split out to keep
 * SoundUtil under the size limit. No-ops / safe defaults headless or when storage is unavailable.
 */
object VolumePrefs {
    private const val VOL_KEY = "qgress.volume"
    private const val MUTE_KEY = "qgress.muted"

    /** (saved non-mute level or null, muted intent). */
    fun load(): Pair<Double?, Boolean> {
        if (HtmlUtil.isNotRunningInBrowser()) return null to false
        return runCatching {
            localStorage.getItem(VOL_KEY)?.toDoubleOrNull()?.takeIf { it > 0.0 } to (localStorage.getItem(MUTE_KEY) == "true")
        }.getOrDefault(null to false)
    }

    fun save(volume: Double, muted: Boolean) {
        if (HtmlUtil.isNotRunningInBrowser()) return
        runCatching {
            localStorage.setItem(VOL_KEY, volume.toString())
            localStorage.setItem(MUTE_KEY, muted.toString())
        }
    }
}
