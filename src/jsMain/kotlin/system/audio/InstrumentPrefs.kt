package system.audio
import util.Prefs

/**
 * Persists the per-instrument tuning (currently the explosion [KickDrum]) across reloads, mirroring
 * [VolumePrefs] / [AudioPrefs]. [load] runs at startup; [save] is called when an Instruments knob changes; the
 * TUNING LAB shows + resets the values.
 */
object InstrumentPrefs {
    private const val KEY = "qgress.instruments"

    fun load() {
        val o = (Prefs.read(KEY) ?: return).kick ?: return
        Prefs.apply(o.pitch) { KickDrum.setPitchMult(it) }
        Prefs.apply(o.decay) { KickDrum.setDecayMult(it) }
        Prefs.apply(o.click) { KickDrum.setClickMult(it) }
        Prefs.apply(o.drive) { KickDrum.setDrive(it) }
    }

    fun save() = Prefs.save(KEY, ::json)

    /** The instrument tuning as a plain object — persisted by [save] and shown by the TUNING LAB. */
    fun json(): dynamic {
        val kick: dynamic = js("({})")
        kick.pitch = KickDrum.pitchMult
        kick.decay = KickDrum.decayMult
        kick.click = KickDrum.clickMult
        kick.drive = KickDrum.drive
        val o: dynamic = js("({})")
        o.kick = kick
        return o
    }
}
