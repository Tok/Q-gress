package util

import kotlinx.browser.document
import org.w3c.dom.url.URL

/**
 * Dev-tooling gate (PLAN: `?debug` tooling). Enabled by the `?debug` URL param — runtime diagnostics
 * only, off by default, never affects normal play. Sub-modes via `?debug=<mode>` (e.g. `capture`).
 *
 * Safe in Node (no browser → disabled), so the functional core stays deterministic in tests.
 *  - `?debug`            → [enabled] true, [mode] ""  (connectivity self-check log + stuck-agent viz)
 *  - `?debug=capture`    → [mode] "capture"           (grid-fixture export sweep)
 */
object Debug {
    private val raw: String? by lazy {
        if (!HtmlUtil.isRunningInBrowser()) null else URL(document.location?.href ?: "").searchParams.get("debug")
    }

    val enabled: Boolean get() = raw != null
    val mode: String get() = raw ?: ""
}
