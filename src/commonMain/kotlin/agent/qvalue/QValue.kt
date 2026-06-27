package agent.qvalue

/**
 * One tunable behaviour weighting in the action substrate (PLAN Phase 6.0): a stable [id], its base [weight]
 * (multiplied into the 0..1 slider value by `ActionSelector`), and a UI [description]. Pure data in the
 * shared core — the tuning-list icon is resolved separately in jsMain ([QIcons]) so this carries no UI.
 */
open class QValue(val id: String, val weight: Double, val description: String) {
    val sliderId = id + "Slider"
}
