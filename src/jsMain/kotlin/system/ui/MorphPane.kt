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

    /** True while a morph pane exists — i.e. we arrived from a later step (vs. a fresh faction screen). */
    fun hasPane(): Boolean = pane != null

    private const val MORPH_AWAY_MS = 560 // a touch past the CSS transition, so the pane lingers through the wobble

    /** Fly the (existing) pane to [target] inside [overlay] from [fromRect], then remove it and run [onDone].
     *  Used to morph back onto a screen that owns its own glass (the faction CTA): the pane lands on the CTA's
     *  footprint and vanishes, then the CTA fades in. */
    fun morphAwayTo(overlay: HTMLElement, target: HTMLElement, fromRect: dynamic, onDone: () -> Unit) {
        morphInto(overlay, target, fromRect)
        window.setTimeout({
            reset()
            onDone()
        }, MORPH_AWAY_MS)
    }

    private const val STORE_KEY = "qg.morphFrom"

    /** Stash the pane's current footprint so the morph can continue across the page reload the location step
     *  triggers — the loading overlay reads it ([consumeReloadFrom]) and glides its panel down from here. */
    fun persistForReload() {
        val r = pane?.asDynamic()?.getBoundingClientRect() ?: return
        window.sessionStorage.setItem(STORE_KEY, "${r.left as Double},${r.top as Double},${r.width as Double},${r.height as Double}")
    }

    /** The footprint saved before the reload ([persistForReload]), as a {left,top,width,height} object, or
     *  null. One-shot: clears it so a later in-game Reset (no onboarding) doesn't inherit a stale morph. */
    fun consumeReloadFrom(): dynamic {
        val raw = window.sessionStorage.getItem(STORE_KEY) ?: return null
        window.sessionStorage.removeItem(STORE_KEY)
        val parts = raw.split(",").mapNotNull { it.toDoubleOrNull() }
        if (parts.size != 4) return null
        val o: dynamic = js("({})")
        o.left = parts[0]
        o.top = parts[1]
        o.width = parts[2]
        o.height = parts[3]
        return o
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
