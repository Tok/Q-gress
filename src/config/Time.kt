package config

object Time {
    val minTickInterval = 20 //milliseconds
    val secondsPerTick = 1

    val globalSpeedFactor = 0.5F
    fun ticksPerFrame() = globalSpeedFactor * 100.0 / World.speed
}
