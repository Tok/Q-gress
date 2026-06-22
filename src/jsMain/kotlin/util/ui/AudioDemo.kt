package util.ui

import agent.Faction
import config.Sim
import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import system.Checkpoint
import util.SoundUtil
import util.data.Pos

/**
 * Audio demo (`#audio`): a panel of buttons that trigger every (standalone) game sound, so the SFX
 * palette can be auditioned without playing. Level-keyed sounds use the selected L1–L8. (Link/field/
 * NPC sounds need live world objects, so they're not included here.)
 */
object AudioDemo {
    private var level = 8
    private val center get() = Pos(Sim.width / 2, Sim.height / 2)

    fun show() {
        SoundUtil.enableAudio() // first gesture (the page load into the demo) resumes the audio context
        val panel = document.createElement("div") as HTMLDivElement
        panel.id = "demoPanel"
        panel.addClass("demoPanel", "coda")
        panel.append(heading("AUDIO DEMO"))
        panel.append(link("#demo", "← Demos", "demoBack"))
        panel.append(levelRow())
        sounds().forEach { (label, action) -> panel.append(button(label, "demoButton", action)) }
        document.body?.append(panel)
    }

    private fun sounds(): List<Pair<String, () -> Unit>> = listOf(
        "Portal create" to { SoundUtil.playPortalCreationSound(center) },
        "Portal remove" to { SoundUtil.playPortalRemovalSound(center) },
        "Glass shatter" to { SoundUtil.playGlassShatterSound(center) },
        "Neutralize" to { SoundUtil.playNeutralizeSound(center) },
        "XMP" to { SoundUtil.playXmpSound(center, level) },
        "Hack" to { SoundUtil.playHackingSound(center, level) },
        "Glyph" to { SoundUtil.playGlyphingSound(center, level) },
        "Reso deploy" to { SoundUtil.playResoDeploySound(center, level) },
        "Mod deploy" to { SoundUtil.playModDeploySound(center, level) },
        "Shield deploy" to { SoundUtil.playShieldDeploySound(center, level) },
        "Shield remove" to { SoundUtil.playShieldRemoveSound(center, level) },
        "Virus ADA (ENL)" to { SoundUtil.playVirusSound(center, Faction.ENL) },
        "Virus JARVIS (RES)" to { SoundUtil.playVirusSound(center, Faction.RES) },
        "Upgrade" to { SoundUtil.playUpgradeSound(center, level) },
        "Downgrade" to { SoundUtil.playDowngradeSound(center, level) },
        "Deploy (legacy)" to { SoundUtil.playDeploySound(center, 50) },
        "Field down" to { SoundUtil.playFieldDownSound() },
        "Cycle" to { SoundUtil.playCycleSound() },
        "Fail" to { SoundUtil.playFailSound() },
        "Checkpoint" to { SoundUtil.playCheckpointSound(js("({})").unsafeCast<Checkpoint>()) },
        "Noise gen" to { SoundUtil.playNoiseGenSound() },
        "Offscreen portal" to { SoundUtil.playOffScreenLocationCreationSound() },
        "Thunder" to { SoundUtil.playThunderSound(0.0) },
    )

    private fun levelRow(): HTMLDivElement {
        val row = document.createElement("div") as HTMLDivElement
        row.addClass("demoRow")
        for (lvl in 1..8) row.append(button("L$lvl", "demoButton demoMini") { level = lvl })
        return row
    }

    private fun heading(text: String): HTMLDivElement {
        val d = document.createElement("div") as HTMLDivElement
        d.addClass("demoHeading")
        d.innerHTML = text
        return d
    }

    private fun button(label: String, cssClass: String, onClick: () -> Unit): HTMLButtonElement {
        val b = document.createElement("button") as HTMLButtonElement
        b.className = cssClass
        b.innerHTML = label
        b.onclick = {
            onClick()
            null
        }
        return b
    }

    private fun link(href: String, label: String, cssClass: String): HTMLAnchorElement {
        val a = document.createElement("a") as HTMLAnchorElement
        a.href = href
        a.className = cssClass
        a.innerHTML = label
        return a
    }
}
