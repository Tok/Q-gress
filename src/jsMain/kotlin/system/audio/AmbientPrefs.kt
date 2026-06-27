package system.audio
import util.Prefs

/**
 * Persists the [AmbientBed] state (on / level / cutoff) across reloads, mirroring [VolumePrefs] / [AudioPrefs].
 * [load] runs at startup (restores the bed, incl. re-starting it if it was on); [save] on any Ambient change;
 * shown + reset by the TUNING LAB.
 */
object AmbientPrefs {
    private const val KEY = "qgress.ambient"

    fun load() {
        val o = Prefs.read(KEY) ?: return
        Prefs.apply(o.level) { AmbientBed.setLevel(it) }
        Prefs.apply(o.cutoff) { AmbientBed.setCutoff(it) }
        Prefs.apply(o.distance) { AmbientBed.setDistance(it) }
        if (o.enabled == false) AmbientBed.setEnabled(false) // default on (the field hum is automatic)
    }

    fun save() = Prefs.save(KEY, ::json)

    /** The ambient-bed state as a plain object — persisted by [save] and shown by the TUNING LAB. */
    fun json(): dynamic {
        val o: dynamic = js("({})")
        o.enabled = AmbientBed.enabled
        o.level = AmbientBed.level
        o.cutoff = AmbientBed.cutoffHz
        o.distance = AmbientBed.distance
        return o
    }
}
