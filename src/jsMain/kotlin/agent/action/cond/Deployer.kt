package agent.action.cond

import agent.Agent
import agent.Inventory
import agent.action.ActionItem
import config.Dim
import items.deployable.HeatSink
import items.deployable.Mod
import items.deployable.Multihack
import items.deployable.Resonator
import items.deployable.Shield
import items.level.ResonatorLevel
import portal.Octant
import portal.Portal
import portal.ResonatorSlot
import system.audio.Snd
import system.effect.Fx
import util.Log
import kotlin.math.max

object Deployer : ConditionalAction {
    override val actionItem = ActionItem.DEPLOY

    override fun isActionPossible(agent: Agent): Boolean {
        if (!isActionPortalFriendly(agent)) return false
        return canDeployAnyReso(agent) || canDeployMod(agent)
    }

    private fun canDeployAnyReso(agent: Agent): Boolean {
        if (!areMoreResosAllowed(agent)) return false
        val inventoryResos = inventoryResos(agent.inventory)
        if (inventoryResos.isEmpty()) return false
        val ownedInPortal = ownedInPortal(agent)
        return inventoryResos.toSet().any { canDeployReso(it, ownedInPortal, agent) }
    }

    // The best deployable mod the agent carries (shields first, then heat sinks, then multi-hacks; link amps
    // are inactive so they're never deployed).
    private fun deployableMod(agent: Agent): Mod? = agent.inventory.items
        .filterIsInstance<Mod>()
        .filter { it is Shield || it is HeatSink || it is Multihack }
        .minByOrNull {
            when (it) {
                is Shield -> 0
                is HeatSink -> 1
                else -> 2
            }
        }

    private fun canDeployMod(agent: Agent): Boolean =
        agent.actionPortal.isFriendlyTo(agent) && agent.actionPortal.hasFreeModSlot() && deployableMod(agent) != null

    override fun performAction(agent: Agent): Agent {
        // One thing per action: a resonator if one fits, else a mod into a free slot.
        if (!deployOneReso(agent)) deployOneMod(agent)
        return agent
    }

    // Deploy the single best resonator (highest level, empty slots first); false if none fits.
    private fun deployOneReso(agent: Agent): Boolean {
        val ownedInPortal = ownedInPortal(agent)
        val target = inventoryResos(agent.inventory).firstNotNullOfOrNull { reso ->
            // highest level first
            deployTargetFor(agent, ownedInPortal, reso)?.let { reso to it }
        } ?: return false
        actuallyDeployOne(agent, target.second, target.first)
        return true
    }

    private fun deployOneMod(agent: Agent) {
        val mod = deployableMod(agent)
        if (mod == null || !canDeployMod(agent)) {
            Log.warn("Deployment failed..")
            return
        }
        val portal = agent.actionPortal
        portal.deployMod(agent, mod)
        val lvl = mod.getLevel().coerceAtLeast(1)
        if (mod is Shield) Snd.sink.playShieldDeploySound(portal.location, lvl) else Snd.sink.playModDeploySound(portal.location, lvl)
        agent.action.start(ActionItem.DEPLOY)
    }

    // The octant a resonator should fill (empty slots first), or null if it can't be deployed here.
    // The same-level cap mirrors Portal.deploy's own gate so we never pick a target it will refuse
    // (which left the reso unplaced AND unconsumed → an endless deploy loop).
    private fun deployTargetFor(agent: Agent, ownedInPortal: List<Pair<Octant, ResonatorSlot>>, reso: Resonator): Octant? {
        val sameLevel = agent.actionPortal.slots.count { it.value.resonator?.level == reso.level }
        val ok = reso.level.level <= agent.getLevel() &&
            maxDeployable(ownedInPortal, reso) > 0 &&
            sameLevel < reso.level.deployablePerPlayer
        return if (!ok) null else deployableSlots(agent.actionPortal, reso).minByOrNull { if (it.second.isEmpty()) 0 else 1 }?.first
    }

    private fun actuallyDeployOne(agent: Agent, octant: Octant, reso: Resonator) {
        val portal = agent.actionPortal
        val distance = max(agent.distanceToPortal(portal), Dim.minDeploymentRange).toInt()
        portal.deploy(agent, mapOf(octant to reso), distance) // handles AP/XM + consumes the reso
        Snd.sink.playResoDeploySound(portal.location, reso.level.level)
        Fx.sink.recordDeploy("portal:${portal.id}", octant, agent.pos) // fly the rod in from the agent
        agent.action.start(ActionItem.DEPLOY)
    }

    private fun isActionPortalFriendly(agent: Agent) = !agent.actionPortal.isEnemyOf(agent)
    private fun areMoreResosAllowed(agent: Agent) = allowedResoLevels(agent).map { it.value }.sum() > 0

    private fun allowedResoLevels(agent: Agent): Map<ResonatorLevel, Int> = agent.actionPortal.findAllowedResoLevels(agent)

    private fun ownedInPortal(agent: Agent) = agent.actionPortal.slots.filter { it.value.isOwnedBy(agent) }.toList()
    private fun inventoryResos(inv: Inventory) =
        inv.items.filter { it is Resonator }.map { it as Resonator }.sortedByDescending { it.level }

    private fun maxDeployable(ownedInPortal: List<Pair<Octant, ResonatorSlot>>, reso: Resonator): Int {
        val owned =
            ownedInPortal.filter { slot -> slot.second.resonator?.level?.level ?: 0 >= reso.level.level }.count()
        return max(reso.level.deployablePerPlayer - owned, 0)
    }

    private fun deployableSlots(portal: Portal, reso: Resonator): List<Pair<Octant, ResonatorSlot>> = portal.slots
        .filter {
            it.value.isEmpty() ||
                it.value.resonator?.level?.level ?: 0 < reso.level.level
        }
        .toList()

    // Possible exactly when an actual target exists — same gate Portal.deploy uses (no loop).
    private fun canDeployReso(reso: Resonator, ownedInPortal: List<Pair<Octant, ResonatorSlot>>, agent: Agent): Boolean =
        deployTargetFor(agent, ownedInPortal, reso) != null
}
