package agent.action

/**
 * The pure hack/glyph spin durations (seconds) — the gameplay timing half of the (three.js) `HackFx` collar
 * animation. Lifted to commonMain so [agent.action.cond.Hacker]/[agent.action.cond.Glypher] can compute the
 * base spin time without naming the renderer; `HackFx` re-exports these so its visual side stays single-sourced.
 * The *render-speed* scaling (sim animation speed) is applied at the audio shell ([system.audio.BrowserAudio]),
 * not here.
 */
object HackTiming {
    /** Seconds a normal hacked collar spins (real hacks are a few s; the cooldown lets us stretch it). */
    const val HACK_S = 4.5

    private const val GLYPH_BASE_S = 4.0 // glyph floor (~a skilled glypher); + per level below
    private const val GLYPH_PER_LEVEL_S = 0.8 // higher portals take more glyphs → longer (skill TBD)

    /** Glyph spin time grows with portal [level] (more glyphs to draw). Real range is wider (agent skill,
     *  not yet modelled) — a skilled glypher could clear a low portal in ~5s. */
    fun glyphDuration(level: Int) = GLYPH_BASE_S + level.coerceIn(1, 8) * GLYPH_PER_LEVEL_S
}
