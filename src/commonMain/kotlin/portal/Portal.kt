package portal
import World
import agent.Agent
import agent.Balance
import agent.Faction
import config.Config
import config.Dim
import config.IngressFacts
import config.Sim
import config.Time
import extension.*
import items.deployable.HeatSink
import items.deployable.Mod
import items.deployable.Multihack
import items.deployable.Resonator
import items.deployable.Shield
import items.level.PortalLevel
import items.level.ResonatorLevel
import system.Com
import system.audio.Snd
import system.effect.Fx
import system.grid.Nav
import util.*
import util.Rng
import util.data.*
import kotlin.math.*

data class Portal(
    val name: String,
    val location: Pos,
    val heatMap: GridMap,
    var vectors: VectorField, // filled asynchronously after creation (Nav.sink.compute)
    val slots: Slots,
    val links: MutableSet<Link>,
    val fields: MutableSet<Field>,
    var owner: Agent?,
    val mods: MutableMap<ModSlot, Mod> = mutableMapOf(), // up to 4 mod slots (shields / heat sinks / link amps)
) {
    internal val lastHacks: MutableMap<String, MutableList<Int>> = mutableMapOf() // per-agent hack history ([PortalHacks])
    internal var lastFlipTick: Int? = null // tick of the last virus flip — drives the flip-immunity window ([isFlippable])
    val id: String = "P-" + location.x + ":" + location.y + "-" + name
    fun isDeprecated() = slots.isEmpty()

    fun isUncaptured() = owner == null
    fun isEnemyOf(agent: Agent) = owner != null && owner?.faction != agent.faction
    fun isFriendlyTo(agent: Agent) = owner != null && owner?.faction == agent.faction

    private fun isCoveredByField() = World.allFields().any { it.isCoveringPortal(this) }

    // Owned by the linker's faction + at least [Config.linkMinResos] resonators. The old rule required FULL
    // deployment (all 8), which under a dynamic, flipping board was almost never reached → fields never formed
    // ("cat-and-mouse capturing"). Ingress lets any owned portal link; a low floor lets fields actually happen.
    private fun isLinkable(linker: Agent): Boolean = this.owner?.faction == linker.faction && numberOfResosLeft() >= Config.linkMinResos
    private fun isInside(): Boolean = findConnectedPortals().none { connected ->
        connected.fields.filter { it.isConnectedTo(this) }.count() > 1
    }

    fun canHack(hacker: Agent): Boolean = PortalHacks.canHack(this, hacker)
    fun canLinkOut(linker: Agent) = isLinkable(linker) &&
        (links.isEmpty() || links.count() < MAX_OUTGOING_LINKS) &&
        !isCoveredByField() &&
        isInside()

    // Level comes from the deployed resonators only: a neutral portal (no resonators) has NO level (0)
    // and can't be attacked/shattered; a captured portal is at least level 1.
    internal fun calculateLevel() = if (numberOfResosLeft() == 0) {
        0
    } else {
        clipLevel(slots.values.sumOf { it.resonator?.level?.level ?: 0 } / IngressFacts.RESO_SLOTS)
    }

    fun getLevel() = if (World.isReady) {
        PortalLevel.findByValue(calculateLevel())
    } else {
        PortalLevel.ZERO
    }

    fun x() = location.x
    fun y() = location.y

    private fun getAllResos() = this.slots.map { it.value.resonator }.filterNotNull()
    fun numberOfResosLeft() = this.slots.count { it.value.resonator != null }
    private fun isFullyDeployed() = numberOfResosLeft() == IngressFacts.RESO_SLOTS
    private fun averageResoLevel(): Double {
        val resos = getAllResos()
        return resos.map { it.level.level }.sum() / resos.count().toDouble()
    }

    fun filledSlots() = slots.map { it.value }.filterNot { it.resonator == null }
    fun resoMap() = slots.mapNotNull { (octant, slot) -> slot.resonator?.let { octant to it } }.toMap()

    private fun linkMitigation(): Int = PortalMath.linkMitigationFor(findIncomingFrom().count() + links.count())

    private fun modMitigation(): Int = mods.values.filterIsInstance<Shield>().sumOf { it.type.mitigation }

    /** Total incoming-damage reduction (links + deployed shields), at the live gameplay cap
     *  ([Config.maxMitigation], from the Combat-dynamism setting; authentic 95 lives in IngressFacts). */
    fun totalMitigation(): Int = min(Config.maxMitigation(), linkMitigation() + modMitigation())

    /** Hack-cooldown multiplier from deployed heat sinks: rarest applies full, each subsequent halved. */
    fun cooldownFactor(): Double {
        val reductions = mods.values.filterIsInstance<HeatSink>()
            .map { it.type.cooldownReduction / 100.0 }
            .sortedDescending()
        var total = 0.0
        var weight = 1.0
        reductions.forEach {
            total += it * weight
            weight *= 0.5
        }
        return (1.0 - total).coerceIn(MIN_COOLDOWN_FACTOR, 1.0)
    }

    fun hasFreeModSlot(): Boolean = mods.size < ModSlot.values().size
    fun modCount(): Int = mods.size

    /**
     * Knock the mod in [slot] out of the portal (an XMP/Ultra-Strike stripped it — see
     * [items.XmpBurster.knockMods]). Drops it, plays the shield-removal FX, awards AP. Returns the
     * removed mod, or null if the slot was empty. The dropped mod just vanishes on the next sync.
     */
    fun stripMod(slot: ModSlot, destroyer: Agent?): Mod? {
        val mod = mods.remove(slot) ?: return null
        destroyer?.addAp(MOD_DESTROY_AP)
        // Subtle "plop" as the item pops out of its slot (any mod — shield, heat sink, …).
        Snd.sink.playKnockOutSound(location)
        Com.addMessage("$destroyer knocked a ${mod.abbr} off $this.", Com.Importance.MINOR, destroyer?.faction?.color)
        return mod
    }

    /** Slot a mod (shield / heat sink) into the first free mod slot (charges XM, awards AP, consumes). */
    fun deployMod(deployer: Agent, mod: Mod) {
        val free = ModSlot.values().firstOrNull { !mods.containsKey(it) } ?: return
        mods[free] = mod
        // A heat sink instantly clears the portal-wide hack cooldown on attach (it also shortens the window
        // going forward via [cooldownFactor]). Wiping the per-agent hack history resets everyone's timer +
        // any burnout to zero, so the portal is hackable again immediately.
        if (mod is HeatSink) lastHacks.clear()
        deployer.removeXm(modCost(mod))
        deployer.addAp(MOD_DEPLOY_AP)
        deployer.inventory.items.remove(mod)
        Com.addMessage("$deployer deployed a ${mod.abbr} on $this.", Com.Importance.MINOR, deployer.faction.color)
    }

    private fun modCost(mod: Mod): Int = when (mod) {
        is Shield -> mod.type.deployCostXm
        is HeatSink -> mod.type.deployCostXm
        is Multihack -> mod.type.deployCostXm
        else -> 0
    }

    /** Off the virus flip-immunity window? A portal can be flipped (in either direction) only once per
     *  [FLIP_IMMUNITY_S] of sim time; a never-flipped portal is always flippable. */
    fun isFlippable(): Boolean = lastFlipTick?.let { World.tick - it >= Time.secondsToTicks(FLIP_IMMUNITY_S) } ?: true

    /**
     * Virus flip (ADA / JARVIS): the portal changes hands — it is **not** destroyed. The **item type**
     * decides the result faction ([flipsTo]), not [agent]'s faction: the new owner becomes the (nearest)
     * agent of [flipsTo], and **all of the slot content** (every resonator + mod) stays in place, just
     * re-owned. So [agent] may flip an enemy portal to its own colour OR its own portal to the enemy
     * colour. Only the links/fields are torn down (they'd be cross-faction now), the orb re-skins to the
     * new colour **without** the capture shatter (see [system.display.CaptureFx]), and the portal goes
     * flip-immune for [FLIP_IMMUNITY_S] ([isFlippable]).
     */
    fun refactor(agent: Agent, flipsTo: Faction) {
        val newOwner = if (flipsTo == agent.faction) {
            agent
        } else {
            // friendly-flip: hand the portal to the nearest agent of the target faction (both factions
            // always have agents — the `?: agent` is only a theoretical empty-roster guard).
            World.allAgents.filter { it.faction == flipsTo }.minByOrNull { it.pos.distanceTo(location) } ?: agent
        }
        owner = newOwner
        slots.values.filter { it.resonator != null }.forEach { it.owner = newOwner }
        // The portal changed faction, so every link/field it touches is now cross-faction → destroy them all
        // (incoming + outgoing + anchored fields). Without this the old faction's links survive on the flipped
        // portal — e.g. a green portal virus-flipped to blue would still show its incoming GREEN links.
        destroyAllLinksAndFields()
        lastFlipTick = World.tick // start the flip-immunity window
        Fx.sink.refactorPortal("portal:$id") // re-skin the orb to the new faction, no shatter
        agent.addAp(VIRUS_AP)
        Com.addMessage("$agent refactored $this to $flipsTo.", Com.Importance.MAJOR, flipsTo.color)
    }

    private fun findStrongestReso(): Resonator? {
        val resos = getAllResos()
        if (resos.isEmpty()) {
            return null
        } else {
            return resos.sortedBy { it.energy * it.level.level }.first()
        }
    }

    fun findStrongestResoPos(): Pos? = findStrongestReso()?.position

    // Health is over ALL 8 octants: an empty slot contributes no energy, so a portal only reads 100% when
    // every octant holds a full resonator. Each resonator's % is already relative to its level's max energy
    // (so health is level-aware), and a half-deployed portal reads as half-health.
    fun calcHealth(): Int {
        if (getAllResos().isEmpty()) return 0 // neutral portal — no resonators
        val health = getAllResos().sumOf { it.calcHealthPercent() } / Octant.values().size
        return MathUtil.clip(health, 0, 100)
    }

    // AUTHENTIC Ingress link-range formula: 160 × (avg resonator level)⁴ metres (see config.IngressFacts)
    // — DON'T change the 160/⁴ to tune; it's the original. (Note: currently returns a lambda, not the value.)
    fun calculateLinkingRangeInMeters() = {
        val x = averageResoLevel()
        if (isFullyDeployed()) 160 * x * x * x * x else 0.0
    }

    fun findRandomPointNearPortal(distance: Int): Pos {
        // Sample points on the ring until one lands on passable ground, but cap the tries: a portal boxed
        // in by impassable cells (or off the grid, as in a headless match) would otherwise recurse forever
        // → stack overflow. Falling back to the portal centre is always safe.
        repeat(NEARBY_POINT_TRIES) {
            val angle = Rng.random() * PI
            val point = location.copy(x = location.x + (distance * cos(angle)).toInt(), y = location.y + (distance * sin(angle)).toInt())
            if (World.grid[point.toShadow()]?.isPassable ?: false) return point
        }
        return location
    }

    fun findConnectedPortals(): List<Portal> = findOutgoingTo() + findIncomingFrom()

    fun findLinkableForKeys(linker: Agent): List<Portal> {
        val keyset = linker.inventory.findUniqueKeys()
        // No crossing links: the new line must not intersect any EXISTING link. (Previously filtered
        // through Link.isNotExisting, which is always false for already-placed links → a no-op; the
        // real check was only in Linker. Now enforced here too.)
        val existingLines = World.allLines()
        val nonIntersecting: List<Portal> = keyset.map { it.portal }.filter { destination ->
            val line = Line(location, destination.location)
            existingLines.none { it.doesIntersect(line) }
        }
        return nonIntersecting.filter { it.isLinkable(linker) }
    }

    // VERBOSE TTS: call out a field covering a big slice of the play box (rare → not spammy).
    private fun announceIfHugeField(field: Field, faction: Faction) {
        if (field.calculateMu().toLong() > Sim.width.toLong() * Sim.height / HUGE_FIELD_FRACTION) {
            Snd.sink.announceHugeField(faction, field.calculateMu())
        }
    }

    fun createLink(linker: Agent, target: Portal) {
        val newLink: Link? = Link.create(this, target, linker)
        if (newLink != null) {
            // create link
            links.add(newLink)
            linker.inventory.consumeKeyToPortal(target)
            Com.addMessage("$linker created a link from $this to $target", Com.Importance.MINOR, linker.faction.color)
            Snd.sink.playLinkingSound(newLink)
            linker.addAp(IngressFacts.AP_CREATE_LINK) // 313 (was 187 — that's the DESTROY-link value)
            linker.removeXm(LINK_XM_COST)

            // create fields
            val connectedToTarget = target.findConnectedPortals()
            val connectedToHere = this.findConnectedPortals()
            val anchors = connectedToTarget.filter { connectedToHere.contains(it) }
            anchors.forEach { anchor ->
                if (Field.isPossible(this, target, anchor)) {
                    val newField = Field.create(this, target, anchor, linker)
                    if (newField != null) {
                        Com.addMessage("$linker created a field at $this. +$newField", Com.Importance.MAJOR, linker.faction.color)
                        Snd.sink.playFieldingSound(newField)
                        fields.add(newField)
                        linker.addAp(IngressFacts.AP_CREATE_FIELD) // 1250, flat (size drives MU, not AP)
                        announceIfHugeField(newField, linker.faction) // VERBOSE TTS
                    }
                }
            }
        }
    }

    fun tryHack(hacker: Agent): HackResult = PortalHacks.tryHack(this, hacker)

    fun tryGlyph(glypher: Agent): HackResult = PortalHacks.tryGlyph(this, glypher)

    fun isOwnedByEnemy(agent: Agent) = owner?.faction != null && owner?.faction != agent.faction
    fun deploy(deployer: Agent, resos: Map<Octant, Resonator>, distance: Int) {
        check(!isOwnedByEnemy(deployer))

        val isCapture = owner == null
        if (isCapture) {
            owner = deployer
            Com.addMessage("$deployer captured $this.", Com.Importance.MAJOR, deployer.faction.color)
        }

        val initialResoCount = slots.count { it.value.resonator != null }
        val firstResoCount = max(resos.size, (IngressFacts.RESO_SLOTS - initialResoCount))
        resos.asIterable().forEachIndexed { index, (octant, resonator) ->
            val level = resonator.level
            val oldReso = slots[octant]
            // A player can NEVER upgrade their OWN resonator — only fill an empty slot or replace a
            // teammate's lower one. That's what forces a level-8 portal to need 8 different agents.
            if (oldReso?.isOwnedBy(deployer) ?: false) return
            val oldLevel = oldReso?.resonator?.level?.level
            check(oldLevel ?: 0 < level.level)
            // The authentic per-level deploy caps (L8/L7:1, L6/L5:2, L4-L2:4, L1:8) are PER PLAYER, not
            // per portal: count only the slots THIS agent already owns at this level, so many agents can
            // each add their own top-level resonator (8×L8 → a level-8 portal).
            val sameLevelCount = slots.count { it.value.isOwnedBy(deployer) && it.value.resonator?.level == level }

            val isUnableToDeployMoreOfTheSame = sameLevelCount >= level.deployablePerPlayer
            if (isUnableToDeployMoreOfTheSame) { // should only happen rarely
                return
            }

            deployer.addAp(deployAp(isCapture, index, firstResoCount, initialResoCount, oldReso))
            deployer.removeXm(level.level * RESO_DEPLOY_XM_PER_LEVEL)
            val oldDistance = oldReso?.distance
            // Clamp into the legal deploy band: a stored upgrade distance — or the agent's per-tick step
            // granularity landing it a hair outside max range — could otherwise trip ResonatorSlot.deployReso's
            // [min, max] range checks. Clamping here keeps the slot invariant and the rod geometry consistent.
            val newDistance = ((if (oldDistance == 0) distance else oldDistance) ?: distance)
                .coerceIn(Dim.minDeploymentRange.toInt(), Dim.maxDeploymentRange.toInt())
            slots[octant]?.deployReso(deployer, resonator, newDistance)
            val xx = location.x + octant.calcXOffset(newDistance)
            val yy = location.y + octant.calcYOffset(newDistance)
            resonator.deploy(this, octant, Pos(xx, yy))
        }
        deployer.inventory.consumeResos(resos.map { it.value })
    }

    // AP for placing the resonator at [index] of a deploy batch: capturing / first-fill / completing the
    // portal / upgrading a teammate's rod (replacing an occupied slot — self-upgrade returns early in [deploy]).
    private fun deployAp(isCapture: Boolean, index: Int, firstResoCount: Int, initialResoCount: Int, oldReso: ResonatorSlot?): Int = when {
        isCapture && index == 0 -> IngressFacts.AP_CAPTURE_PORTAL
        index < firstResoCount -> IngressFacts.AP_DEPLOY_RESONATOR
        index == firstResoCount && firstResoCount + initialResoCount == IngressFacts.RESO_SLOTS -> IngressFacts.AP_COMPLETE_PORTAL
        oldReso?.resonator != null -> IngressFacts.AP_UPGRADE_RESONATOR
        else -> 0
    }

    private fun findOutgoingTo(): List<Portal> = links.map { it.destination }
    private fun findIncomingLinks(): List<Link> = World.allLinks().filter { it.destination == this }
    private fun findIncomingFrom(): List<Portal> = findIncomingLinks().map { it.origin }
    private fun destroyAllLinksAndFields(destroyer: Agent? = null) {
        World.allLinks().filter { it.destination == this }.forEach { link ->
            destroyer?.addAp(Link.destroyAp)
            link.origin.links.remove(link)
        }
        links.forEach {
            destroyer?.addAp(Link.destroyAp)
        }
        links.clear()
        World.allFields().filter { it.primaryAnchor == this }.forEach { it ->
            destroyer?.addAp(Field.destroyAp)
            it.origin.fields.remove(it)
        }
        World.allFields().filter { it.secondaryAnchor == this }.forEach { it ->
            destroyer?.addAp(Field.destroyAp)
            it.origin.fields.remove(it)
        }
        fields.forEach {
            destroyer?.addAp(Field.destroyAp)
        }
        fields.clear()
    }

    /** Portal defense: an ENEMY portal zaps the [agent] with a retaliation bolt + XM damage (friendly /
     *  neutral portals do nothing). Higher level + shields zap harder. */
    fun retaliate(agent: Agent) {
        val defender = owner ?: return
        if (defender.faction == agent.faction) return
        val level = getLevel().value
        agent.removeXm(PortalMath.retaliationDamage(level, totalMitigation()))
        Fx.sink.fireBolt(location, level, agent.pos, defender.faction.color)
        Snd.sink.playThunderSound((agent.pos.x / Sim.width * 2.0 - 1.0).coerceIn(-1.0, 1.0))
    }

    fun destroy(destroyer: Agent? = null) {
        val droppedMods = mods.values.toList()
        val lvl = getLevel().value
        owner = null
        slots.forEach {
            it.value.clear()
        }
        mods.clear()
        if (droppedMods.isNotEmpty()) {
            Fx.sink.dropMods(location, lvl, droppedMods) // mods tumble out when the portal goes down
            droppedMods.filterIsInstance<Shield>().firstOrNull()?.let { Snd.sink.playShieldRemoveSound(location, it.getLevel()) }
        }
        destroyAllLinksAndFields(destroyer)
        World.allAgents.forEach { agent ->
            if (agent.actionPortal == this) {
                agent.actionPortal = World.randomPortal()
                agent.action.end() // its target vanished → re-select next tick (never park in WAIT)
            }
        }
    }

    fun remove() {
        // Capture before destroy() clears owner/resonators.
        val shardColor = owner?.faction?.color ?: "#bbbbbb"
        val level = getLevel().value
        val resoLevels = resoMap().mapValues { it.value.getLevel() } // capture before destroy clears the slots
        val heaviness = (0.1 + level * 0.06).coerceAtMost(0.7)
        destroy()
        Fx.sink.shatterPortal(location, shardColor, level, resoLevels) // glass shards + resonators fall
        Snd.sink.playGlassShatterSound(location, heaviness, 0.8)
        World.allAgents.forEach { agent ->
            val portalKeys: List<PortalKey>? = agent.inventory.findKeys().filter { key -> key.portal == this }.toList()
            if (portalKeys != null) {
                agent.inventory.items.removeAll(portalKeys)
            }
        }
        World.allPortals.remove(this)
    }

    fun removeReso(octant: Octant, destroyer: Agent?) {
        val lvl = getLevel().value
        val resoLevel = slots[octant]?.resonator?.getLevel()
        this.slots[octant]?.clear()
        if (resoLevel != null) {
            Fx.sink.dropResonator(location, lvl, octant.ordinal, resoLevel) // the destroyed rod falls out
        }
        val leftResos = numberOfResosLeft()
        when {
            leftResos <= 0 -> {
                // No destroyer → the last resonator drained on its own (cycle decay / dominance erosion), not an
                // attack. Phrase it passively and colour it by the faction that lost the portal, never red.
                val color = (destroyer?.faction ?: owner?.faction)?.color ?: Com.NEUTRAL
                val message = if (destroyer != null) "$destroyer neutralized $this." else "$this decayed to neutral."
                Com.addMessage(message, Com.Importance.MAJOR, color)
                destroy(destroyer)
            }
            leftResos <= LINK_TEARDOWN_RESO_THRESHOLD -> destroyAllLinksAndFields(destroyer)
        }
    }

    fun findAllowedResoLevels(deployer: Agent): Map<ResonatorLevel, Int> = if (owner == null || owner?.faction == deployer.faction) {
        ResonatorLevel.values().map { level ->
            level to level.deployablePerPlayer - this.slots.count { slot ->
                slot.value.isOwnedBy(deployer) && slot.value.resonator?.level?.level == level.level
            }
        }.toMap()
    } else {
        mapOf()
    }

    fun leakXm(): Pair<Pos, Int> {
        val fluct = Rng.randomInt(LEAK_XM_JITTER)
        val offset = if (Rng.randomBool()) fluct else -fluct
        return location to if (getLevel().toInt() <= LEAK_LEVEL_SPLIT) {
            (calculateLevel() * LOW_LEVEL_LEAK_XM_PER_LEVEL) + offset
        } else {
            (calculateLevel() * HIGH_LEVEL_LEAK_XM_PER_LEVEL) + offset
        }
    }

    fun decay() {
        val allResos = getAllResos()
        allResos.forEach { it.decay() }
        val newResos = getAllResos()
        if (newResos.isEmpty()) {
            destroy()
        }
    }

    /** Per-checkpoint dominance erosion: a portal owned by the LEADING faction loses extra resonator energy
     *  proportional to how far ahead that faction is on MU (× [Config.dominanceDecay]). An over-extended
     *  empire crumbles → the board reopens → the lead can change. No-op when even/behind or the lever is off. */
    fun erodeByDominance() {
        val faction = owner?.faction ?: return
        val scale = Config.dominanceDecay * Balance.leadShare(faction)
        if (scale <= 0.0) return
        getAllResos().forEach { it.decay(scale) }
        if (getAllResos().isEmpty()) destroy()
    }

    override fun toString() = name
    override fun equals(other: Any?) = other is Portal && id == other.id
    override fun hashCode() = id.hashCode() * 31

    companion object {
        /** The portal nearest to [pos] (null if none exist). Was Portal.nearestTo. */
        fun nearestTo(pos: Pos): Portal? {
            val nearest = World.allPortals.map { it.location.distanceTo(pos) to it }.sortedBy { it.first }.toSet()
            return if (nearest.isNotEmpty()) nearest.first().second else null
        }

        /** The friendly portals [agent] can recharge: the one it's standing at and working (its action
         *  portal) below full-ish health (≤ [CHARGEABLE_HEALTH_MAX] — the casual top-up, no key needed, as
         *  in Ingress), plus keyed REMOTE ones only when badly hurt (≤ [REMOTE_CHARGEABLE_HEALTH_MAX] — the
         *  emergency hold). Both bars are deliberately narrow: cycle decay leaves EVERY friendly portal a
         *  bit below full, so a looser gate keeps a recharge on offer everywhere — and away from the action
         *  portal only recycle/move (base 0.01) compete, so agents would tarpit at each damaged portal they
         *  pass (or recharge↔recycle in place off a keyed portal) instead of ever travelling. */
        fun findChargeable(agent: Agent, keys: List<PortalKey>): List<Portal> {
            val keyed = keys.map { it.portal }.toSet()
            return World.factionPortals(agent.faction).filter {
                val health = it.calcHealth()
                if (it == agent.actionPortal && agent.isAtActionPortal()) {
                    health <= CHARGEABLE_HEALTH_MAX
                } else {
                    keyed.contains(it) && health <= REMOTE_CHARGEABLE_HEALTH_MAX
                }
            }
        }

        // Hacks allowed before burnout. Above the authentic 4 (a poor-man's multi-hack) so agents restock
        // XMPs / Ultra-Strikes / power cubes (→ XM) fast enough to mount a portal-felling assault — the
        // supply side of "shielded portals barely change". A real multi-hack mod can refine this later.
        const val MAX_HACKS = 6
        private const val NEARBY_POINT_TRIES = 16 // ring-sampling cap in findRandomPointNearPortal (no infinite recursion)
        private const val MOD_DEPLOY_AP = 125
        private const val MOD_DESTROY_AP = 75 // AP for knocking a mod off (cf. resonator destroy)
        private const val MIN_COOLDOWN_FACTOR = 0.05 // heat sinks can't reduce cooldown below 5%
        private const val VIRUS_AP = 1000 // AP for flipping a portal with a virus
        private const val FLIP_IMMUNITY_S = 3600 // 1h: a flipped portal can't be flipped again (either direction)

        // A SIM cap (not authentic Ingress, which limits links by keys/range) — the hard outbound-link ceiling
        // per portal. Raising it (a future link-amp mod) enables deeper nested fields (see PLAN).
        private const val MAX_OUTGOING_LINKS = 8
        private const val LINK_XM_COST = 250 // XM an agent spends to place one link
        private const val RESO_DEPLOY_XM_PER_LEVEL = 20 // XM to deploy one resonator, per its level
        private const val CHARGEABLE_HEALTH_MAX = 90 // an in-range friendly portal below this health% can be recharged
        private const val REMOTE_CHARGEABLE_HEALTH_MAX = 50 // keyed remote recharge only below this health% (see findChargeable)
        private const val LINK_TEARDOWN_RESO_THRESHOLD = 2 // at/below this many resos left, links + fields drop

        // Passive stray-XM leak a portal emits each cycle (agents refuel from it): calculateLevel × the per-level
        // payout, jittered ± up to LEAK_XM_JITTER. Low-level portals leak more per level than high-level ones.
        private const val LEAK_XM_JITTER = 300
        private const val LEAK_LEVEL_SPLIT = 4.5 // ≤ L4 uses the low-level payout, ≥ L5 the high-level one
        private const val LOW_LEVEL_LEAK_XM_PER_LEVEL = 1000
        private const val HIGH_LEVEL_LEAK_XM_PER_LEVEL = 750
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))

        private const val UNIQUE_NAME_TRIES = 8 // regen a fresh natural name this many times before suffixing

        /** A portal name not already in use on the board: prefer a freshly-generated natural name; if the board
         *  is dense enough that several collide, fall back to a numeral suffix (`Foo 2`). */
        private fun uniqueName(candidate: String): String {
            val taken = World.allPortals.mapTo(HashSet()) { it.name }
            if (candidate !in taken) return candidate
            repeat(UNIQUE_NAME_TRIES) {
                val fresh = PortalNameGen.generate()
                if (fresh !in taken) return fresh
            }
            var n = 2
            while ("$candidate $n" in taken) n++
            return "$candidate $n"
        }

        // The pure link-defense curve, retaliation damage, hack cooldown + burnout math now live in the shared
        // core ([PortalMath]); callers below use it directly.

        // Non-blocking: the portal is built with an empty flow field, then Nav.sink.compute
        // fills portal.vectors off-thread (heatMap stays empty — it's never read externally). Agents
        // fall back to a straight-line heading while vectors is empty (Agent.moveCloserToDestinationPortal).
        fun create(location: Pos): Portal {
            val slots: Slots = Octant.values().map { it to ResonatorSlot.create() }.toMap().toMutableMap()
            val portal = Portal(
                uniqueName(Names.sink.nameFor(location) ?: PortalNameGen.generate()),
                location,
                emptyMap(), // heatMap (vestigial GridMap — never read)
                VectorField.EMPTY, // vectors — filled once the flow field computes
                slots,
                mutableSetOf(),
                mutableSetOf(),
                null,
            )
            Snd.sink.playPortalCreationSound(location) // NoOpAudio headless
            Nav.sink.compute(location) { field ->
                // async in-browser / inline headless-sync / skipped
                portal.vectors = field
                Fx.sink.flashVectorField("portal:${portal.id}") // NoOpEffects headless
            }
            if (World.isReady) Snd.sink.announcePortalDiscovery(portal.name) // VERBOSE TTS (in-game spawns only, not world-gen)
            return portal
        }

        private const val SPREAD_CANDIDATES = 8
        private const val HUGE_FIELD_FRACTION = 6 // "huge field" = MU over 1/this of the play box

        // Best-candidate (Mitchell's) sampling: of several valid candidates, take the one farthest from
        // existing portals, so portals spread to cover the sim space optimally (edges fill before the
        // centre clusters). Each candidate already respects the min-distance gate (createRandomForPortal).
        // Used for the initial spawns AND the Explore action.
        fun createRandom(): Portal {
            // Grid-driven for both browser and headless: sample passable cells (the real passability map) and
            // spread off the existing portals' positions — no rectangular Rng(Sim.width) box, no trial-and-error.
            // (Bare unit tests with no World.grid fall back to a simple position via createRandomForPortal.)
            if (!World.hasGrid()) return create(Positions.createRandomForPortal())
            val candidates = Positions.portalCandidates() // ONE grid scan; sample from it (cheap)
            if (candidates.isEmpty()) return create(Positions.createRandomForPortal())
            fun pick() = candidates[(Rng.random() * candidates.size).toInt()]
            val existing = World.allPortals.map { it.location }
            if (existing.isEmpty()) return create(pick())
            var best = pick()
            var bestDist = existing.minOf { it.distanceTo(best) }
            repeat(SPREAD_CANDIDATES - 1) {
                val candidate = pick()
                val nearest = existing.minOf { it.distanceTo(candidate) }
                if (nearest > bestDist) {
                    bestDist = nearest
                    best = candidate
                }
            }
            return create(best)
        }
    }
}
