package util

import kotlinx.browser.document
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.w3c.dom.HTMLElement

/**
 * Keeps MapLibre's bottom-right control cluster (zoom / compass / pitch + the ⓘ attribution) usable over the
 * footer. `#initialMap` has `z-index: 8` (a stacking context), so the controls were trapped *below* the
 * body-level footer (`z-index: 60`) no matter how high their own z-index — `position: fixed` changes geometry,
 * not stacking-context membership, so CSS alone can't escape it. [lift] reparents the cluster to `<body>` so
 * the stylesheet's `fixed; z-index: 999` finally competes at root level and floats it above the footer.
 * Visibility tracks the satellite/street toggle ([setVisible]) since the cluster only drives `#initialMap`.
 */
internal object MapControls {
    private const val INVISIBLE = "invisible"
    private var lifted: HTMLElement? = null

    fun lift(containerId: String) {
        if (lifted != null) return
        val container = document.getElementById(containerId) ?: return
        val ctrl = container.asDynamic().querySelector(".maplibregl-ctrl-bottom-right") as? HTMLElement ?: return
        document.body?.appendChild(ctrl)
        lifted = ctrl
    }

    fun setVisible(visible: Boolean) {
        val el = lifted ?: return
        if (visible) el.removeClass(INVISIBLE) else el.addClass(INVISIBLE)
    }
}
