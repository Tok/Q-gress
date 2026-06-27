package system.ui

import agent.Faction
import ai.FactionPolicies
import ai.OverridePolicy
import ai.SliderVector
import ai.llm.LlmParser
import ai.llm.LlmPolicy
import ai.llm.WebLlmClient
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * The **LLM reasoning** section of the AI footer tab (PLAN Phase 6.3): for each LLM-driven faction, shows the
 * model status, the latest prompt it was sent, its raw reply, and what that reply parsed to (the favoured
 * actions) — or that it fell back to the heuristic. Reads [LlmPolicy.lastPrompt]/[lastReply] each frame, but
 * only re-renders a faction when its reply changed (parsing every frame would be wasteful). Hidden entirely
 * until an LLM actually drives a faction.
 */
object LlmReasoningPanel {
    private class View(val status: HTMLElement, val prompt: HTMLElement, val reply: HTMLElement, val parsed: HTMLElement)

    private var built = false
    private var section: HTMLElement? = null
    private val blocks = mutableMapOf<Faction, HTMLElement>()
    private val views = mutableMapOf<Faction, View>()
    private val lastReply = mutableMapOf<Faction, String>()

    fun update() {
        if (!ensure()) return
        var anyLlm = false
        Faction.all().forEach { faction ->
            val policy = llmOf(faction)
            blocks[faction]?.let { setVisible(it, policy != null) }
            if (policy != null) {
                anyLlm = true
                render(faction, policy)
            }
        }
        section?.let { setVisible(it, anyLlm) }
    }

    private fun llmOf(faction: Faction): LlmPolicy? {
        val policy = FactionPolicies.of(faction)
        val base = (policy as? OverridePolicy)?.inner ?: policy
        return base as? LlmPolicy
    }

    private fun render(faction: Faction, policy: LlmPolicy) {
        val view = views[faction] ?: return
        view.status.textContent = (policy.client as? WebLlmClient)?.status ?: "mock"
        if (lastReply[faction] == policy.lastReply) return // reply unchanged → skip the re-parse/render
        lastReply[faction] = policy.lastReply
        view.prompt.textContent = policy.lastPrompt
        view.reply.textContent = policy.lastReply.ifBlank { "(warming up — heuristic fallback meanwhile)" }
        val parsed = LlmParser.parse(policy.lastReply)
        view.parsed.textContent = if (parsed == null) {
            "unparsed → heuristic fallback"
        } else {
            SliderVector.ORDER.sortedByDescending { parsed[it] }
                .take(3).joinToString(", ") { "${it.description} ${(parsed[it] * 100).toInt()}%" }
        }
    }

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
        val sec = el("div", "aiSection")
        sec.appendChild(el("div", "aiSectionTitle").also { it.textContent = "LLM reasoning" })
        Faction.all().forEach { sec.appendChild(block(it)) }
        section = sec
        Footer.tab("ai").appendChild(sec)
        built = true
        return true
    }

    private fun block(faction: Faction): HTMLElement {
        val block = el("div", "llmBlock")
        block.appendChild(
            el("div", "llmHead").also {
                it.textContent = "${faction.abbr} · LLM"
                it.asDynamic().style.color = faction.color
            },
        )
        val status = labelled(block, "status")
        val prompt = labelled(block, "prompt", pre = true)
        val reply = labelled(block, "reply", pre = true)
        val parsed = labelled(block, "chose")
        views[faction] = View(status, prompt, reply, parsed)
        blocks[faction] = block
        return block
    }

    private fun labelled(parent: HTMLElement, key: String, pre: Boolean = false): HTMLElement {
        parent.appendChild(el("div", "llmKey").also { it.textContent = key })
        val value = el(if (pre) "pre" else "div", if (pre) "llmPre" else "llmVal")
        parent.appendChild(value)
        return value
    }

    private fun setVisible(e: HTMLElement, visible: Boolean) {
        e.asDynamic().style.display = if (visible) "" else "none"
    }
}
