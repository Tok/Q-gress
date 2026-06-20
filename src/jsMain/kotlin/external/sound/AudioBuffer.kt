package external.sound

import org.khronos.webgl.Float32Array

/*
 * https://developer.mozilla.org/en-US/docs/Web/API/AudioBuffer
 */
external class AudioBuffer {
    val length: Int
    val sampleRate: Int
    fun getChannelData(channel: Int): Float32Array
}
