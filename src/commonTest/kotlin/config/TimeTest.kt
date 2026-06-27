package config

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Shared-core tests for [Time] — the tick↔second↔timestamp conversions (runs on jsNodeTest + jvmTest). One tick
 * is one second, so the conversions are identities; the value is pinning that and the HH:MM:SS formatting seam
 * (which delegates to [util.MathUtil.formatSeconds]).
 */
class TimeTest {
    @Test
    fun tickSecondConversionsAreIdentities() {
        assertEquals(90, Time.ticksToSeconds(90), "1 second per tick")
        assertEquals(90, Time.secondsToTicks(90))
        assertEquals(0, Time.ticksToSeconds(0))
    }

    @Test
    fun timestampFormatsHoursMinutesSeconds() {
        assertEquals("00:00:00", Time.ticksToTimestamp(0))
        assertEquals("00:01:05", Time.ticksToTimestamp(65), "65 s → 1 min 5 s")
        assertEquals("01:01:01", Time.ticksToTimestamp(3661), "3661 s → 1 h 1 min 1 s")
    }

    @Test
    fun minTickIntervalIsPinned() {
        assertEquals(20, Time.minTickInterval, "the simulation's minimum frame interval in ms")
    }
}
