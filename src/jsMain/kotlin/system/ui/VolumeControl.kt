package system.ui

import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import system.audio.Sound

/**
 * Wires a speaker [icon] + volume [slider] to behave identically on the title screen ([Onboarding]) and
 * the in-game toolbar ([util.HtmlUtil]): dragging the slider sets the master volume, clicking the icon
 * mutes/unmutes (zeroing or restoring the slider), and the icon glyph swaps to a muted speaker whenever
 * the slider sits at 0. The glyph tracks the *slider* (not [Sound.isMuted]) so it reads correctly
 * before the first user gesture, when the master volume is still 0 pending audio-enable.
 */
object VolumeControl {

    fun build(icon: HTMLElement, slider: HTMLInputElement) {
        icon.title = "Mute / unmute"
        refreshIcon(icon, slider)
        icon.onclick = {
            slider.value = Sound.toggleMute().toString()
            refreshIcon(icon, slider)
            null
        }
        slider.oninput = {
            Sound.setMasterVolume(slider.valueAsNumber)
            refreshIcon(icon, slider)
            null
        }
    }

    /** Set [icon]'s glyph to match the [slider]'s value (0 → muted speaker). */
    fun refreshIcon(icon: HTMLElement, slider: HTMLInputElement) {
        icon.innerHTML = if (slider.valueAsNumber <= 0.0) Icons.VOLUME_MUTED else Icons.VOLUME
    }
}
