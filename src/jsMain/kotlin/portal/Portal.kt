package portal

import World
import agent.Agent
import agent.action.ActionItem
import config.DropRates
import config.Sim
import config.Time
import extension.*
import items.PowerCube
import items.QgressItem
import items.XmpBurster
import items.deployable.HeatSink
import items.deployable.Mod
import items.deployable.Resonator
import items.deployable.Shield
import items.deployable.Virus
import items.level.PortalLevel
import items.level.PowerCubeLevel
import items.level.ResonatorLevel
import items.level.XmpLevel
import items.types.HeatSinkType
import items.types.ShieldType
import items.types.VirusType
import system.Com
import system.display.Scene3D
import system.display.VectorFieldOverlay
import util.*
import util.data.Line
import util.data.Pos
import kotlin.math.*

data class Portal(
    val name: String,
    val location: Pos,
    val heatMap: GridMap,
    var vectors: VectorField, // filled asynchronously after creation (PathUtil.computeFieldAsync)
    val slots: Slots,
    val links: MutableSet<Link>,
    val fields: MutableSet<Field>,
    var owner: Agent?,
    val mods: MutableMap<ModSlot, Mod> = mutableMapOf(), // up to 4 mod slots (shields / heat sinks / link amps)
) {
    private val lastHacks: MutableMap<String, MutableList<Int>> = mutableMapOf()
    val id: String = "P-" + location.x + ":" + location.y + "-" + name
    fun isDeprecated() = slots.isEmpty()

    fun isUncaptured() = owner == null
    fun isEnemyOf(agent: Agent) = owner != null && owner?.faction != agent.faction
    fun isFriendlyTo(agent: Agent) = owner != null && owner?.faction == agent.faction

    private fun isCoveredByField() = World.allFields().any { it.isCoveringPortal(this) }
    private fun isLinkable(linker: Agent): Boolean = this.owner?.faction == linker.faction && isFullyDeployed()
    private fun isInside(): Boolean = findConnectedPortals().none { connected ->
        connected.fields.filter { it.isConnectedTo(this) }.count() > 1
    }

    fun canHack(hacker: Agent): Boolean = handleCooldown(hacker, true) == Cooldown.NONE
    fun canLinkOut(linker: Agent) = isLinkable(linker) &&
        (links.isEmpty() || links.count() < 8) &&
        !isCoveredByField() &&
        isInside()

    // Level comes from the deployed resonators only: a neutral portal (no resonators) has NO level (0)
    // and can't be attacked/shattered; a captured portal is at least level 1.
    private fun calculateLevel() = if (numberOfResosLeft() == 0) {
        0
    } else {
        clipLevel(slots.values.sumOf { it.resonator?.level?.level ?: 0 } / 8)
    }

    fun getLevel() = if (World.isReady) {
        PortalLevel.findByValue(calculateLevel())
    } else {
        PortalLevel.ZERO
    }

    fun x() = location.x
    fun y() = location.y

    private fun getAllResos() = this.slots.map { it.value.resonator }.filterNotNull()
    private fun numberOfResosLeft() = this.slots.count { it.value.resonator != null }
    private fun isFullyDeployed() = numberOfResosLeft() == 8
    private fun averageResoLevel(): Double {
        val resos = getAllResos()
        return resos.map { it.level.level }.sum() / resos.count().toDouble()
    }

    fun filledSlots() = slots.map { it.value }.filterNot { it.resonator == null }
    fun resoMap() = slots.mapNotNull { (octant, slot) -> slot.resonator?.let { octant to it } }.toMap()

    private fun linkMitigation(): Int {
        val incoming = findIncomingFrom()
        val totalLinkCount = incoming.count() + links.count()
        return round(400.0 / 9.0 * atan(totalLinkCount / E)).toInt()
    }

    private fun modMitigation(): Int = mods.values.filterIsInstance<Shield>().sumOf { it.type.mitigation }

    /** Total incoming-damage reduction (links + deployed shields), capped at 95% (Ingress rule). */
    fun totalMitigation(): Int = min(MAX_MITIGATION, linkMitigation() + modMitigation())

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
        if (mod is Shield && HtmlUtil.isRunningInBrowser()) SoundUtil.playShieldRemoveSound(location, mod.getLevel())
        Com.addMessage("$destroyer knocked a ${mod.abbr} off $this.")
        return mod
    }

    /** Slot a mod (shield / heat sink) into the first free mod slot (charges XM, awards AP, consumes). */
    fun deployMod(deployer: Agent, mod: Mod) {
        val free = ModSlot.values().firstOrNull { !mods.containsKey(it) } ?: return
        mods[free] = mod
        deployer.removeXm(modCost(mod))
        deployer.addAp(MOD_DEPLOY_AP)
        deployer.inventory.items.remove(mod)
        Com.addMessage("$deployer deployed a ${mod.abbr} on $this.")
    }

    private fun modCost(mod: Mod): Int = when (mod) {
        is Shield -> mod.type.deployCostXm
        is HeatSink -> mod.type.deployCostXm
        else -> 0
    }

    /** Virus flip (ADA / JARVIS): take the portal for [agent]'s faction — reassign resonators, drop mods. */
    fun refactor(agent: Agent) {
        val strippedShield = mods.values.filterIsInstance<Shield>().firstOrNull()
        owner = agent
        slots.values.filter { it.resonator != null }.forEach { it.owner = agent }
        mods.clear()
        agent.addAp(VIRUS_AP)
        if (strippedShield != null && HtmlUtil.isRunningInBrowser()) SoundUtil.playShieldRemoveSound(location, strippedShield.getLevel())
        Com.addMessage("$agent refactored $this to ${agent.faction}.")
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
    fun calcHealth(): Int {
        val resos = getAllResos()
        if (resos.isEmpty()) return 0 // neutral portal — no resonators
        val health = resos.map { it.calcHealthPercent() }.sum() / resos.count()
        return Util.clip(health, 0, 100)
    }

    private fun calcTotalXm(): Int = getAllResos().map { it.energy }.sum()
    fun calculateLinkingRangeInMeters() = {
        val x = averageResoLevel() // kotlin.math.pow?
        if (isFullyDeployed()) 160 * x * x * x * x else 0.0
    }

    fun findRandomPointNearPortal(distance: Int): Pos {
        val angle = Util.random() * PI
        val xOffset: Int = (distance * cos(angle)).toInt()
        val yOffset: Int = (distance * sin(angle)).toInt()
        val point = location.copy(x = location.x + xOffset, y = location.y + yOffset)
        return if (World.grid[point.toShadow()]?.isPassable ?: false) {
            point
        } else {
            findRandomPointNearPortal(distance)
        }
    }

    private fun findConnectedPortals(): List<Portal> = findOutgoingTo() + findIncomingFrom()

    fun findLinkableForKeys(linker: Agent): List<Portal> {
        val keyset = linker.inventory.findUniqueKeys() ?: return emptyList()
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

    fun createLink(linker: Agent, target: Portal) {
        val newLink: Link? = Link.create(this, target, linker)
        if (newLink != null) {
            // create link
            links.add(newLink)
            linker.inventory.consumeKeyToPortal(target)
            Com.addMessage("$linker created a link from $this to $target")
            SoundUtil.playLinkingSound(newLink)
            linker.addAp(187)
            linker.removeXm(250)

            // create fields
            val connectedToTarget = target.findConnectedPortals()
            val connectedToHere = this.findConnectedPortals()
            val anchors = connectedToTarget.filter { connectedToHere.contains(it) }
            anchors.forEach { anchor ->
                if (Field.isPossible(this, target, anchor)) {
                    val newField = Field.create(this, target, anchor, linker)
                    if (newField != null) {
                        Com.addMessage("$linker created a field at $this. +$newField")
                        SoundUtil.playFieldingSound(newField)
                        fields.add(newField)
                        linker.addAp(1250)
                    }
                }
            }
        }
    }

    fun tryHack(hacker: Agent): HackResult {
        val cooldown = handleCooldown(hacker, false)
        if (cooldown == Cooldown.NONE) {
            val stuff = hack(hacker)
            return HackResult(stuff, null)
        }
        return HackResult(null, cooldown)
    }

    fun tryGlyph(glypher: Agent): HackResult {
        val normal = tryHack(glypher)
        if (normal.cooldown == null) {
            val glyphItems = mutableListOf<QgressItem>()
            glyphItems.addAll(normal.items ?: emptyList())
            glyphItems.addAll(hack(glypher))
            if (Util.random() < glypher.skills.glyphSkill) {
                glyphItems.addAll(hack(glypher))
            }
            return HackResult(glyphItems.toList(), null)
        }
        return HackResult(null, normal.cooldown)
    }

    private fun hack(hacker: Agent): MutableList<QgressItem> {
        val level = min(calculateLevel(), hacker.getLevel())

        val newStuff = mutableListOf<QgressItem?>()
        newStuff.addAll(obtainResos(hacker, level))
        newStuff.addAll(obtainXmps(hacker, level))
        newStuff.addAll(obtainShields(hacker))
        newStuff.addAll(obtainHeatSinks(hacker))
        newStuff.addAll(obtainVirus(hacker))
        newStuff.addAll(obtainPowerCubes(level, hacker))
        newStuff.add(PortalKey.tryHack(this, hacker))

        val isEnemyPortal = owner != null && hacker.faction != owner?.faction
        if (isEnemyPortal) {
            hacker.addAp(100)
            hacker.removeXm(300 * this.calculateLevel())
        } else {
            hacker.removeXm(50 * this.calculateLevel())
        }

        return newStuff.filterNotNull().toMutableList()
    }

    private fun obtainResos(hacker: Agent, level: Int): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        Quality.values().map { quality ->
            val selectedLevel = ResonatorLevel.find(level, quality).level
            while (Util.random() < quality.chance) {
                stuff.add(Resonator.create(hacker, selectedLevel) as QgressItem)
            }
        }
        return stuff
    }

    private fun obtainXmps(hacker: Agent, level: Int): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        Quality.values().map { quality ->
            val selectedLevel = XmpLevel.find(level, quality).level
            while (Util.random() < quality.chance) {
                stuff.add(XmpBurster.create(hacker, selectedLevel) as QgressItem)
            }
        }
        return stuff
    }

    private fun obtainShields(hacker: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        ShieldType.values().forEach {
            if (Util.random() < DropRates.shieldChance.getValue(it)) {
                stuff.add(Shield(it, hacker))
            }
        }
        return stuff
    }

    private fun obtainHeatSinks(hacker: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        HeatSinkType.values().forEach {
            if (Util.random() < DropRates.heatSinkChance.getValue(it)) {
                stuff.add(HeatSink(it, hacker))
            }
        }
        return stuff
    }

    private fun obtainPowerCubes(level: Int, hacker: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        Quality.values().map { quality ->
            val selectedLevel = PowerCubeLevel.find(level, quality).level
            while (Util.random() < quality.chance * 0.3) {
                stuff.add(PowerCube.create(hacker, selectedLevel) as QgressItem)
            }
        }
        return stuff
    }

    private fun obtainVirus(hacker: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        VirusType.values().forEach {
            if (Util.random() < DropRates.virusChance.getValue(it)) {
                stuff.add(Virus(it, hacker))
            }
        }
        return stuff
    }

    private fun handleCooldown(hacker: Agent, readOnly: Boolean): Cooldown {
        // a result of NONE should add a the ticknumber to the list of the last hacks
        val key = hacker.key()
        fun cool(agentsLastHacks: MutableList<Int>, tickNr: Int): Cooldown {
            agentsLastHacks.sort()
            val lastHack = agentsLastHacks.last()
            val ticksSinceLastHack: Int = tickNr - lastHack
            val baseCooldownS = (Cooldown.FIVE.seconds * cooldownFactor()).toInt() // heat sinks shorten it
            val timeDiff = Time.secondsToTicks(baseCooldownS) - ticksSinceLastHack
            val cooldown = Cooldown.valueOf(Time.ticksToSeconds(timeDiff))
            if (cooldown == Cooldown.NONE && !readOnly) {
                agentsLastHacks.add(tickNr)
                lastHacks[key] = mutableListOf(tickNr)
            }
            return cooldown
        }

        fun burn(agentsLastHacks: MutableList<Int>, tickNr: Int): Cooldown {
            val maxBurnoutTicks = Time.secondsToTicks(Cooldown.BURNOUT.seconds)
            val maxTickDifference = tickNr - maxBurnoutTicks
            val isBurnout = agentsLastHacks.toList().filter { it < maxTickDifference }.count() <= 0
            if (isBurnout) {
                return Cooldown.BURNOUT
            } else {
                if (!readOnly) {
                    agentsLastHacks.add(tickNr)
                    lastHacks[key] = mutableListOf(tickNr)
                }
                return Cooldown.NONE // reset
            }
        }

        val isFirstHack = !lastHacks.containsKey(key)
        return if (isFirstHack) {
            if (!readOnly) {
                lastHacks[key] = mutableListOf(World.tick)
            }
            Cooldown.NONE
        } else {
            val agentsLastHacks: MutableList<Int> = lastHacks.getValue(key)
            if (agentsLastHacks.count() < MAX_HACKS) {
                cool(agentsLastHacks, World.tick)
            } else {
                burn(agentsLastHacks, World.tick)
            }
        }
    }

    fun isOwnedByEnemy(agent: Agent) = owner?.faction != null && owner?.faction != agent.faction
    fun deploy(deployer: Agent, resos: Map<Octant, Resonator>, distance: Int) {
        check(!isOwnedByEnemy(deployer))

        val isCapture = owner == null
        if (isCapture) {
            owner = deployer
            Com.addMessage("$deployer captured $this.")
        }

        val initialResoCount = slots.count { it.value.resonator != null }
        val firstResoCount = max(resos.size, (8 - initialResoCount))
        resos.asIterable().forEachIndexed { index, (octant, resonator) ->
            val level = resonator.level
            val oldReso = slots[octant]
            val oldLevel = oldReso?.resonator?.level?.level
            check(oldLevel ?: 0 < level.level)
            val sameLevelCount = slots.count { it.value.resonator?.level == level }

            val isUnableToDeployMoreOfTheSame = sameLevelCount >= level.deployablePerPlayer
            if (isUnableToDeployMoreOfTheSame) { // should only happen rarely
                return
            }

            deployer.addAp(
                when {
                    isCapture && index == 0 -> 500
                    index < firstResoCount -> 125
                    index == firstResoCount && firstResoCount + initialResoCount == 8 -> 250
                    (oldReso?.isOwnedBy(deployer) ?: false) -> 65
                    else -> 0
                },
            )
            deployer.removeXm(level.level * 20)
            val oldDistance = oldReso?.distance
            val newDistance = (if (oldDistance == 0) distance else oldDistance) ?: distance
            // console.trace("$agent deploys ${resonator} to $octant at $this. $distance $oldDistance")
            slots[octant]?.deployReso(deployer, resonator, newDistance)
            val xx = location.x + octant.calcXOffset(newDistance)
            val yy = location.y + octant.calcYOffset(newDistance)
            resonator.deploy(this, octant, Pos(xx, yy))
        }
        deployer.inventory.consumeResos(resos.map { it.value })
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
        agent.removeXm(ZAP_BASE_XM * level + ZAP_SHIELD_XM * totalMitigation())
        if (HtmlUtil.isRunningInBrowser()) {
            Scene3D.fireBolt(location, level, agent.pos, defender.faction.color)
            SoundUtil.playThunderSound((agent.pos.x / Sim.width * 2.0 - 1.0).coerceIn(-1.0, 1.0))
        }
    }

    fun destroy(destroyer: Agent? = null) {
        val droppedMods = mods.values.toList()
        val lvl = getLevel().value
        owner = null
        slots.forEach {
            it.value.clear()
        }
        mods.clear()
        if (droppedMods.isNotEmpty() && HtmlUtil.isRunningInBrowser()) {
            Scene3D.dropMods(location, lvl, droppedMods) // mods tumble out when the portal goes down
            droppedMods.filterIsInstance<Shield>().firstOrNull()?.let { SoundUtil.playShieldRemoveSound(location, it.getLevel()) }
        }
        destroyAllLinksAndFields(destroyer)
        World.allAgents.forEach { agent ->
            if (agent.actionPortal == this) {
                agent.actionPortal = World.randomPortal()
                agent.action.start(ActionItem.WAIT)
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
        Scene3D.shatterPortal(location, shardColor, level, resoLevels) // glass shards + resonators fall
        SoundUtil.playGlassShatterSound(location, heaviness, 0.8)
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
        if (resoLevel != null && HtmlUtil.isRunningInBrowser()) {
            Scene3D.dropResonator(location, lvl, octant.ordinal, resoLevel) // the destroyed rod falls out
        }
        val leftResos = numberOfResosLeft()
        when {
            leftResos <= 0 -> destroy(destroyer)
            leftResos <= 2 -> destroyAllLinksAndFields(destroyer)
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
        val fluct = Util.randomInt(300)
        val offset = if (Util.randomBool()) fluct else -fluct
        return location to if (getLevel().toInt() <= 4.5) {
            (calculateLevel() * 1000) + offset
        } else {
            (calculateLevel() * 750) + offset
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

    override fun toString() = name
    override fun equals(other: Any?) = other is Portal && id == other.id
    override fun hashCode() = id.hashCode() * 31

    companion object {
        fun findChargeableForKeys(agent: Agent, keys: List<PortalKey>): List<Portal>? {
            val chargeable = World.factionPortals(agent.faction).filter { it.calcHealth() <= 90 }.toSet()
            return chargeable.filter { keys.map { a -> a.portal }.contains(it) }
        }

        const val MAX_HACKS = 4 // TODO implement multihacks
        private const val MAX_MITIGATION = 95 // damage-reduction cap (Ingress rule)
        private const val ZAP_BASE_XM = 15 // retaliation XM damage per portal level
        private const val ZAP_SHIELD_XM = 1 // extra retaliation XM per point of mitigation (shields zap harder)
        private const val MOD_DEPLOY_AP = 125
        private const val MOD_DESTROY_AP = 75 // AP for knocking a mod off (cf. resonator destroy)
        private const val MIN_COOLDOWN_FACTOR = 0.05 // heat sinks can't reduce cooldown below 5%
        private const val VIRUS_AP = 1000 // AP for flipping a portal with a virus
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))

        // Non-blocking: the portal is built with an empty flow field, then PathUtil.computeFieldAsync
        // fills portal.vectors off-thread (heatMap stays empty — it's never read externally). Agents
        // fall back to a straight-line heading while vectors is empty (Agent.moveCloserToDestinationPortal).
        fun create(location: Pos): Portal {
            val slots: Slots = Octant.values().map { it to ResonatorSlot.create() }.toMap().toMutableMap()
            val portal = Portal(
                PortalNames.nameFor(location) ?: Util.generatePortalName(),
                location,
                emptyMap(),
                emptyMap(),
                slots,
                mutableSetOf(),
                mutableSetOf(),
                null,
            )
            if (HtmlUtil.isRunningInBrowser()) {
                SoundUtil.playPortalCreationSound(location)
                PathUtil.computeFieldAsync(location) { field ->
                    portal.vectors = field
                    VectorFieldOverlay.flash("portal:${portal.id}")
                }
            }
            return portal
        }

        private const val SPREAD_CANDIDATES = 8

        // Best-candidate (Mitchell's) sampling: of several valid candidates, take the one farthest from
        // existing portals, so portals spread to cover the sim space optimally (edges fill before the
        // centre clusters). Each candidate already respects the min-distance gate (createRandomForPortal).
        // Used for the initial spawns AND the Explore action.
        fun createRandom(): Portal {
            if (!HtmlUtil.isRunningInBrowser()) return create(Pos.createRandomForPortal())
            val candidates = Pos.portalCandidates() // ONE grid scan; sample from it (cheap)
            if (candidates.isEmpty()) return create(Pos.createRandomForPortal())
            fun pick() = candidates[(Util.random() * candidates.size).toInt()]
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
