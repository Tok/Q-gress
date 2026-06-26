package system

/**
 * The event log store. Each [Entry] carries an [Importance] (so the EVENT LOG tab can filter to only the
 * events that matter) and one or more coloured [Segment]s (faction colours for fields/captures/recruiting,
 * a neutral red for faction-agnostic world events like portal discovery/removal, …). A generous backlog is
 * kept; the panel renders a window of it (more when the footer is expanded).
 */
object Com {
    /** Whether an event is "key" ([MAJOR], always shown) or routine ([MINOR], hidden by the "only key" filter). */
    enum class Importance { MINOR, MAJOR }

    /** A coloured run of text within a log line ([color] null = the default line colour). */
    data class Segment(val text: String, val color: String? = null)

    /** One log line: its [importance] + the coloured [segments] that make it up. */
    data class Entry(val importance: Importance, val segments: List<Segment>)

    /** Faction-agnostic world events (portal discovery / removal) read in this neutral red, not a faction hue. */
    const val NEUTRAL = "#ff7a6b"

    const val CAP = 250 // backlog kept in memory; the panel shows a window of it

    private val entries = ArrayDeque<Entry>()

    fun messageCount() = entries.size
    fun clear() = entries.clear()

    /** Log a single-colour line. */
    fun addMessage(text: String, importance: Importance = Importance.MINOR, color: String? = null) =
        addMessage(Entry(importance, listOf(Segment(text, color))))

    /** Log a multi-segment (multi-colour) line — e.g. a checkpoint with each faction's MU in its own colour. */
    fun addMessage(entry: Entry) {
        entries.addLast(entry)
        while (entries.size > CAP) entries.removeFirst()
    }

    /** Snapshot of the log (oldest→newest), for the DOM event-log panel. */
    fun entries(): List<Entry> = entries.toList()
}
