package util.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * The bottom-tier DOM-building helpers shared by the footer panels — each used to keep its own
 * identical private `el(...)`. Top-level in `util.ui`, so panels in this package call them unqualified.
 */

/** Create a `<div>` with [cls] as its className. */
internal fun el(cls: String): HTMLElement = el("div", cls)

/** Create a `<[tag]>` element; sets [cls] as its className when non-empty. */
internal fun el(tag: String, cls: String): HTMLElement {
    val e = document.createElement(tag) as HTMLElement
    if (cls.isNotEmpty()) e.className = cls
    return e
}
