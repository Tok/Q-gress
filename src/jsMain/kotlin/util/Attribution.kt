package util

import kotlinx.browser.document

/**
 * MapLibre map-credit (attribution) helpers. The keyless tile sources require visible attribution, so it's
 * always present — but the compact control loads **expanded**; [collapse] tucks it back into its "ⓘ" once the
 * game starts so the credit doesn't sit open over the play area (clicking the ⓘ re-expands it).
 */
object Attribution {
    fun collapse() {
        val attribs = document.getElementsByClassName("maplibregl-ctrl-attrib")
        for (i in 0 until attribs.length) {
            val el = attribs.item(i)?.asDynamic() ?: continue
            el.classList.remove("maplibregl-compact-show") // class-toggled compact control
            el.open = false // …or the <details>-based one
        }
    }
}
