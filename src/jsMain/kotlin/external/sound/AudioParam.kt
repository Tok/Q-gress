package external.sound

/*
 * https://developer.mozilla.org/en-US/docs/Web/API/AudioParam
 */
external object AudioParam {
    var value: Double
    fun exponentialRampToValueAtTime(value: Double, endTime: Double)
    fun linearRampToValueAtTime(value: Double, endTime: Double)
    fun cancelAndHoldAtTime(cancelTime: Double)
    fun cancelScheduledValues(startTime: Double)
    fun setTargetAtTime(value: Double, startTime: Double, timeConstant: Double)
    fun setValueAtTime(value: Double, startTime: Double)
}
