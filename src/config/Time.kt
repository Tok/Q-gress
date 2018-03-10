package config

import util.Util

object Time {
    private val secondsPerTick = 1
    private val globalSpeedFactor = 1F

    val minTickInterval = 20 //milliseconds

    fun ticksToSeconds(ticks: Int) = ticks * secondsPerTick
    fun secondsToTicks(seconds: Int) = seconds / secondsPerTick

    fun ticksToTimestamp(ticks: Int) = Util.formatSeconds(ticksToSeconds(ticks))

    fun ticksPerFrame() = globalSpeedFactor * 100.0 / World.speed
}
