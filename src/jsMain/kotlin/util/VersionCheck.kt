package util

import config.BuildInfo
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

/**
 * Detects when a newer build has been deployed so users pick it up without a hard-reload. GitHub Pages
 * caches the fixed-name `Q-Gress.js`, so the app polls the deploy-stamped `version.txt` with
 * `cache: 'no-store'` (which bypasses the HTTP cache) and compares it to its own [BuildInfo.GIT_SHA];
 * on a mismatch it shows a one-click "reload" banner. No-ops in dev (no `version.txt` / unknown sha).
 */
object VersionCheck {
    private const val POLL_MS = 180_000 // 3 minutes
    private var shown = false

    fun start() {
        if (BuildInfo.GIT_SHA == "unknown") return // local/dev build — nothing to compare against
        window.setInterval({ poll() }, POLL_MS)
    }

    private fun poll() {
        val opts: dynamic = js("({cache: 'no-store'})")
        window.asDynamic().fetch("version.txt", opts)
            .then { resp -> if (resp.ok as Boolean) resp.text() else null }
            .then { text ->
                val latest = (text as? String)?.trim().orEmpty()
                if (!shown && latest.isNotEmpty() && latest != BuildInfo.GIT_SHA) showBanner()
                null
            }
            .catch { _: dynamic -> null } // offline / dev: ignore
    }

    private fun showBanner() {
        shown = true
        val b = document.createElement("div") as HTMLElement
        b.className = "updateBanner"
        b.textContent = "↻ New version available — click to reload"
        b.onclick = { window.location.reload() }
        document.body?.appendChild(b)
    }
}
