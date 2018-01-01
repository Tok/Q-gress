package external.sound


/*
 * https://developer.mozilla.org/en-US/docs/Web/API/StereoPannerNode/StereoPannerNode
 */
external class StereoPannerNode(context: AudioContext, options: AudioNodeOptions) : AudioNode {
    val pan: AudioParam
}
