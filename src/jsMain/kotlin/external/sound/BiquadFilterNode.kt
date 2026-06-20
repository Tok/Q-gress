package external.sound

/*
 * https://developer.mozilla.org/en-US/docs/Web/API/BiquadFilterNode
 */
external class BiquadFilterNode : AudioNode {
    var type: String // "highpass", "lowpass", "bandpass", ...
    val frequency: AudioParam
}
