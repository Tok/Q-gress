package system.audio

import agent.Faction
import glyph.Glyph
import kotlinx.browser.window
import system.ui.Bootstrap
import util.Prefs
import util.Rng

/** Minimal binding for the Web Speech API utterance — constructed WITH its text, like the reference. */
external class SpeechSynthesisUtterance(text: String) {
    var volume: Double
    var rate: Double
    var pitch: Double
    var voice: dynamic
}

/**
 * On-the-fly text-to-speech via the browser **Web Speech API** — synthesised live, no samples / assets (matches
 * the "generate everything on the fly" rule). A clinical scanner announcer for the sim. Mirrors the
 * spectral-plinko WebSpeechTTS (voice / rate / pitch / volume) with a **verbosity** gate on top:
 *  - OFF — silent.
 *  - MINIMAL (default) — faction on pick, location on start, "Q-Gress" on the title.
 *  - VERBOSE — also checkpoint leads, huge fields, portal discovery, recruitment.
 *  - GLYPH — everything, plus 1–3 random [Glyph]s read back on each successful glyph hack.
 *
 * Web Speech plays straight to the speakers and can't be routed through our Web Audio FX graph, so there are no
 * TTS FX (a browser limitation). Settings persist via [Prefs]; muted/safe headless.
 */
object Tts {
    enum class Verbosity(val label: String) { OFF("Off"), MINIMAL("Minimal"), VERBOSE("Verbose"), GLYPH("Glyph") }

    private const val PREFS_KEY = "qgress.tts"

    var enabled = true
        private set
    var verbosity = Verbosity.MINIMAL
        private set
    var volume = 0.8 // reference defaults
        private set
    var rate = 1.0
        private set
    var pitch = 1.0
        private set
    var voiceName: String? = null
        private set

    private var current: dynamic = null // hold the live utterance so Chrome doesn't GC it mid-speech (known bug)
    private var warnedNoVoices = false

    private fun available(): Boolean =
        !Bootstrap.isNotRunningInBrowser() && js("typeof window !== 'undefined' && 'speechSynthesis' in window").unsafeCast<Boolean>()

    private fun synth(): dynamic = window.asDynamic().speechSynthesis

    // --- announcements (gated by verbosity) -----------------------------------------------------------
    fun announceTitle() = say(TtsPhrases.title(), Verbosity.MINIMAL, pitch = 1.15, rate = 0.95)
    fun announceFaction(f: Faction) = say(TtsPhrases.faction(f), Verbosity.VERBOSE) // high verbosity only
    fun announceLocation(name: String) = say(TtsPhrases.location(name), Verbosity.MINIMAL)
    fun announceCheckpointLead(leader: Faction, leadMu: Int) = say(TtsPhrases.checkpointLead(leader, leadMu), Verbosity.VERBOSE)
    fun announceHugeField(owner: Faction, mu: Int) = say(TtsPhrases.hugeField(owner, mu), Verbosity.VERBOSE)
    fun announcePortalDiscovery(name: String) = say(TtsPhrases.portalDiscovery(name), Verbosity.VERBOSE)
    fun announceRecruitment(f: Faction) = say(TtsPhrases.recruitment(f), Verbosity.VERBOSE)

    /** Read back 1–3 random glyphs by name — only at the GLYPH tier, on a landed glyph hack. */
    fun announceGlyphHack(f: Faction) {
        if (verbosity != Verbosity.GLYPH) return
        val names = Glyph.randomSequence(Rng, 1 + Rng.randomInt(2)).map { it.spokenName } // 1..3
        say(TtsPhrases.glyphHack(f, names), Verbosity.GLYPH)
    }

    /** A user-initiated readout (portal select, location-name click) — speaks whenever TTS isn't *fully* muted
     *  (i.e. enabled and not OFF), independent of the verbosity tier. */
    fun sayOnDemand(text: String) {
        if (!enabled || verbosity == Verbosity.OFF || text.isBlank()) return
        speak(text, rate, pitch)
    }

    /** Audition the current voice/tuning from the AUDIO tab — reads a random glyph sequence, regardless of the
     *  verbosity gate. */
    fun test() {
        val names = Glyph.randomSequence(Rng, 1 + Rng.randomInt(2)).map { it.spokenName } // 1..3 glyphs
        speak(names.joinToString(". ") + ".", rate, pitch)
    }

    // --- core -----------------------------------------------------------------------------------------
    private fun say(text: String, min: Verbosity, rate: Double = this.rate, pitch: Double = this.pitch) {
        if (!enabled || verbosity == Verbosity.OFF || verbosity.ordinal < min.ordinal) return
        speak(text, rate, pitch)
    }

    private fun speak(text: String, rate: Double, pitch: Double) {
        if (!available()) return
        val s = synth()
        warnIfNoVoices(s)
        s.cancel() // announcements never overlap — the latest wins
        val u = SpeechSynthesisUtterance(text)
        u.volume = volume
        u.rate = rate
        u.pitch = pitch
        val voice = voiceFor(s)
        if (voice != null) u.voice = voice
        current = u // keep a reference alive past this call (Chrome GCs utterances mid-speech otherwise)
        s.resume() // some browsers leave the speech queue paused — a no-op when it isn't
        s.speak(u)
    }

    // A silent TTS is almost always "no system speech engine" (notably Chrome/Firefox on Linux without
    // speech-dispatcher + espeak/festival). Surface that once so it's diagnosable rather than mysteriously mute.
    private fun warnIfNoVoices(s: dynamic) {
        if (warnedNoVoices) return
        if (((s.getVoices()?.length as? Int) ?: 0) == 0) {
            warnedNoVoices = true
            console.warn("[tts] no speech voices installed — TTS will be silent (Linux: install speech-dispatcher + espeak)")
        }
    }

    private fun voiceFor(s: dynamic): dynamic {
        val voices = s.getVoices()
        val n = (voices?.length as? Int) ?: 0
        if (n == 0) return null
        voiceName?.let { name -> for (i in 0 until n) if (voices[i].name == name) return voices[i] }
        for (i in 0 until n) if ((voices[i].lang as String).startsWith("en")) return voices[i]
        return voices[0]
    }

    /** (name → lang) for every installed voice — feeds the AUDIO-tab voice picker. */
    fun voices(): List<Pair<String, String>> {
        if (!available()) return emptyList()
        val v = synth().getVoices()
        val n = (v?.length as? Int) ?: 0
        return (0 until n).map { (v[it].name as String) to (v[it].lang as String) }
    }

    fun stop() {
        if (available()) synth().cancel()
    }

    // --- settings (persisted) -------------------------------------------------------------------------
    fun setEnabled(on: Boolean) = mutate { enabled = on }.also { if (!on) stop() }
    fun setVerbosity(v: Verbosity) = mutate { verbosity = v }
    fun setVolume(x: Double) = mutate { volume = x.coerceIn(0.0, 1.0) }
    fun setRate(x: Double) = mutate { rate = x.coerceIn(0.1, 2.0) }
    fun setPitch(x: Double) = mutate { pitch = x.coerceIn(0.0, 2.0) }
    fun setVoice(name: String?) = mutate { voiceName = name }

    private inline fun mutate(change: () -> Unit) {
        change()
        save()
    }

    private fun save() = Prefs.save(PREFS_KEY) {
        val o = js("{}")
        o.enabled = enabled
        o.verbosity = verbosity.name
        o.volume = volume
        o.rate = rate
        o.pitch = pitch
        o.voice = voiceName
        o
    }

    /** Restore persisted settings — call once at startup. */
    fun loadPrefs() {
        val o = Prefs.read(PREFS_KEY) ?: return
        (o.enabled as? Boolean)?.let { enabled = it }
        (o.verbosity as? String)?.let { v -> Verbosity.values().firstOrNull { it.name == v }?.let { verbosity = it } }
        (o.volume as? Double)?.let { volume = it }
        (o.rate as? Double)?.let { rate = it }
        (o.pitch as? Double)?.let { pitch = it }
        voiceName = o.voice as? String
    }
}
