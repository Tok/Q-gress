package config

import util.Util

object Time {
    const val minTickInterval = 20 //milliseconds
    private const val secondsPerTick = 1

    fun ticksToSeconds(ticks: Int) = ticks * secondsPerTick
    fun secondsToTicks(seconds: Int) = seconds / secondsPerTick

    fun ticksToTimestamp(ticks: Int) = Util.formatSeconds(ticksToSeconds(ticks))
}
