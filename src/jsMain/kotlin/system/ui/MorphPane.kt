package system.ui

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

/**
 * The morphing onboarding glass pane. A single persistent glass rectangle that animates (lerps its
 * left/top/width/height) from one onboarding step's panel to the next, so the frame appears to **morph**
 * between screens instead of hard-cutting. The step panels themselves are transparent (`.onboardPanel`);
 * this pane is the only glass, sitting behind their content (`z-index` lets the content paint on top).
 *
 * The faction step keeps its own glass; the pane is born on the first transition out of it (morphing from
 * the captured CTA footprint) and [reset] when onboarding ends or returns to the faction screen.
 *
 * Scope note: onboarding's final step reloads the page (deep-link navigation), so the loading overlay is a
 * fresh document — the morph spans the in-memory steps (faction → drivers → map-size → location), not the
 * reload boundary into the loading screen.
 */
object MorphPane {
    private var pane: HTMLElement? = null

    /**
     * Place the morph pane behind [target] inside [overlay]. With [fromRect] (a step transition) it starts
     * at the previous panel's footprint and animates to [target]'s; without it (first appearance) it snaps.
     */
    fun morphInto(overlay: HTMLElement, target: HTMLElement, fromRect: dynamic) {
        val p = ensure()
        overlay.insertBefore(p, overlay.firstChild) // behind the step's content (which paints above via z-index)
        if (fromRect == null) {
            applyBounds(p, target.asDynamic().getBoundingClientRect()) // first appearance — snap, no morph-from-nothing
            return
        }
        applyBounds(p, fromRect) // start at the previous panel's footprint…
        p.asDynamic().offsetWidth // force reflow so the start bounds commit before we change them
        // …then, next frame, morph to the new panel's footprint (the CSS transition does the lerp).
        window.requestAnimationFrame { applyBounds(p, target.asDynamic().getBoundingClientRect()) }
    }

    /** Drop the pane (onboarding ended, or returned to the faction screen, which owns its own glass). */
    fun reset() {
        pane?.remove()
        pane = null
    }

    private fun ensure(): HTMLElement {
        pane?.let { return it }
        val p = document.createElement("div") as HTMLElement
        p.className = "morphPane"
        pane = p
        return p
    }

    private fun applyBounds(p: HTMLElement, r: dynamic) {
        p.style.left = "${r.left as Double}px"
        p.style.top = "${r.top as Double}px"
        p.style.width = "${r.width as Double}px"
        p.style.height = "${r.height as Double}px"
    }
}
