package portal

import World
import agent.Agent
import agent.Faction
import agent.action.ActionItem
import config.Colors
import config.Dim
import config.Styles
import config.Time
import extension.*
import items.PowerCube
import items.QgressItem
import items.XmpBurster
import items.deployable.DeployableItem
import items.deployable.Resonator
import items.deployable.Shield
import items.deployable.Virus
import items.level.PortalLevel
import items.level.PowerCubeLevel
import items.level.ResonatorLevel
import items.level.XmpLevel
import items.types.ShieldType
import items.types.VirusType
import system.Com
import util.*
import util.data.Circle
import util.data.Line
import util.data.Pos
import kotlin.math.*

data class Portal(
    val name: String, val location: Pos, val heatMap: GridMap, val vectors: VectorField,
    val slots: Slots, val links: MutableSet<Link>, val fields: MutableSet<Field>, var owner: Agent?
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
    fun canLinkOut(linker: Agent) = isLinkable(linker) && (links.isEmpty() || links.count() < 8) &&
            !isCoveredByField() && isInside()

    private fun calculateLevel() = if (owner == null) 1 else clipLevel(slots.values.map {
        (it.resonator?.level?.level ?: 0)
    }.sum() / 8)

    fun getLevel() =
        if (World.isReady) PortalLevel.findByValue(calculateLevel())
        else PortalLevel.ZERO

    fun x() = location.x
    fun y() = location.y

    private fun getAllResos() = slots.map { it.value.resonator }.filterNotNull()
    private fun isFullyDeployed() = getAllResos().count() == 8
    private fun averageResoLevel(): Double {
        val resos = getAllResos()
        return resos.map { it.level.level }.sum() / resos.count().toDouble()
    }

    private fun calculateLinkMitigation(): Int {
        val maxMitigation = 95
        //TODO shields...
        val incoming = findIncomingFrom()
        val totalLinkCount = incoming.count() + links.count()
        return min(maxMitigation, round(400.0 / 9.0 * atan(totalLinkCount / E)).toInt())
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
        val health = resos.map { it.calcHealthPercent() }.sum() / resos.count()
        return Util.clip(health, 0, 100)
    }

    private fun calcTotalXm(): Int = getAllResos().map { it.energy }.sum()
    fun calculateLinkingRangeInMeters() = {
        val x = averageResoLevel() //kotlin.math.pow?
        if (isFullyDeployed()) 160 * x * x * x * x else 0.0
    }

    fun findRandomPointNearPortal(distance: Int): Pos {
        val angle = Util.random() * PI
        val xOffset: Int = (distance * cos(angle)).toInt()
        val yOffset: Int = (distance * sin(angle)).toInt()
        val point = location.copy(x = location.x + xOffset, y = location.y + yOffset)
        return if (World.grid[point.toShadow()]?.isPassable == true) {
            point
        } else {
            findRandomPointNearPortal(distance)
        }
    }

    private fun findConnectedPortals(): List<Portal> = findOutgoingTo() + findIncomingFrom()

    fun findLinkableForKeys(linker: Agent): List<Portal> {
        val keyset = linker.inventory.findUniqueKeys()!!
        val allLinks = World.allPortals.flatMap { it.links }.filter { Link.isNotExisting(it) }.toSet()
        val nonIntersecting: List<Portal> = keyset.map { it.portal }.filter { destination ->
            val line = Line(location, destination.location)
            allLinks.filter { it.getLine().doesIntersect(line) }.isEmpty()
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
            if (Util.random() < it.chance) {
                stuff.add(Shield(it, hacker))
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
            while (Util.random() < (1 / it.roll)) {
                stuff.add(Virus(it, hacker))
            }
        }
        return stuff
    }

    private fun handleCooldown(hacker: Agent, readOnly: Boolean): Cooldown {
        //a result of NONE should add a the ticknumber to the list of the last hacks
        val key = hacker.key()
        fun cool(agentsLastHacks: MutableList<Int>, tickNr: Int): Cooldown {
            agentsLastHacks.sort()
            val lastHack = agentsLastHacks.last()
            val ticksSinceLastHack: Int = tickNr - lastHack
            val timeDiff = Time.secondsToTicks(Cooldown.FIVE.seconds) - ticksSinceLastHack
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
                return Cooldown.NONE //reset
            }
        }

        val isFirstHack = !lastHacks.containsKey(key)
        return if (isFirstHack) {
            if (!readOnly) {
                lastHacks[key] = mutableListOf(World.tick)
            }
            Cooldown.NONE
        } else {
            val agentsLastHacks: MutableList<Int> = lastHacks.get(key)!!
            if (agentsLastHacks.count() < MAX_HACKS) {
                cool(agentsLastHacks, World.tick)
            } else {
                burn(agentsLastHacks, World.tick)
            }
        }
    }

    fun deployMods(deployer: Agent, @Suppress("UNUSED_PARAMETER") mods: Map<Octant, DeployableItem>) {
        val isCommon = true //TODO implement
        val isRare = false
        val isVeryRare = false
        if (isCommon) {
            deployer.removeXm(400)
        }
        if (isRare) {
            deployer.removeXm(800)
        }
        if (isVeryRare) {
            deployer.removeXm(1000)
        }
    }

    fun deploy(deployer: Agent, resos: Map<Octant, Resonator>, distance: Int) {
        val isCapture = owner == null
        if (isCapture) {
            owner = deployer
            Com.addMessage("$deployer captured $this.")
        }

        val initialResoCount = slots.filterValues { !it.isEmpty() }.filterNot { it.value.resonator == null }.size
        val firstResoCount = max(resos.size, (8 - initialResoCount))
        resos.asIterable().forEachIndexed { index, (octant, resonator) ->
            val oldReso = slots[octant]
            if (isCapture && index == 0) {
                deployer.addAp(500)
            } else if (index < firstResoCount) {
                deployer.addAp(125)
            } else if (index == firstResoCount && firstResoCount + initialResoCount == 8) {
                deployer.addAp(250)
            } else if (oldReso?.isOwnedBy(deployer) != true) {
                deployer.addAp(65)
            }
            deployer.removeXm(resonator.level.level * 20)
            val oldDistance = oldReso?.distance
            val newDistance = (if (oldDistance == 0) distance else oldDistance) ?: distance
            //console.trace("$agent deploys ${resonator} to $octant at $this. $distance $oldDistance")
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

    fun destroy(destroyer: Agent? = null) {
        owner = null
        slots.forEach {
            it.value.clear()
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
        destroy()
        SoundUtil.playPortalRemovalSound(location)
        World.allAgents.forEach { agent ->
            val portalKeys: List<PortalKey>? = agent.inventory.findKeys().filter { key -> key.portal == this }.toList()
            if (portalKeys != null) {
                agent.inventory.items.removeAll(portalKeys)
            }
        }
        World.allPortals.remove(this)
    }

    fun removeReso(octant: Octant, destroyer: Agent?) {
        this.slots[octant]?.clear()
        val numberOfResosLeft = slots.filter { it.value.resonator != null }.count()
        if (numberOfResosLeft <= 0) {
            destroy(destroyer)
        } else if (numberOfResosLeft <= 2) {
            destroyAllLinksAndFields(destroyer)
        }
    }

    fun findAllowedResoLevels(deployer: Agent): Map<ResonatorLevel, Int> {
        return if (owner == null || owner?.faction == deployer.faction) {
            ResonatorLevel.values().map { level ->
                level to level.deployablePerPlayer - slots.filter { slot ->
                    slot.value.isOwnedBy(deployer) && slot.value.resonator?.level?.level == level.level
                }.count()
            }.toMap()
        } else {
            mapOf()
        }
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

    fun drawResonators(ctx: Ctx) {
        if (HtmlUtil.isNotRunningInBrowser()) return
        fun drawResoLine(line: Line, levelColor: String, factionColor: String, lineWidth: Double, alpha: Double = 1.0) {
            ctx.globalAlpha = alpha
            ctx.strokeStyle = Colors.black
            ctx.lineWidth = lineWidth + 1.5
            ctx.beginPath()
            ctx.moveTo(line.from.x, line.from.y)
            ctx.lineTo(line.to.x, line.to.y)
            ctx.closePath()
            ctx.stroke()
            ctx.lineWidth = lineWidth
            if (Styles.isDrawResoLineGradient) { //CPU intensive
                val gradient = ctx.createLinearGradient(line.from.x, line.from.y, line.to.x, line.to.y)
                gradient.addColorStop(0.2, levelColor)
                gradient.addColorStop(0.7, factionColor)
                ctx.strokeStyle = gradient
            } else {
                ctx.strokeStyle = levelColor
            }
            ctx.beginPath()
            ctx.moveTo(line.from.x, line.from.y)
            ctx.lineTo(line.to.x, line.to.y)
            ctx.closePath()
            ctx.stroke()
            ctx.globalAlpha = 1.0
        }

        val octantSlots: List<Pair<Octant, ResonatorSlot>> = slots.filter {
            it.value.owner != null && it.value.resonator != null
        }.toList()
        octantSlots.map { octantSlot ->
            val octant: Octant = octantSlot.first
            val slot = octantSlot.second
            val reso = slot.resonator!!
            val resoLevel = reso.level
            val x = location.x + octant.calcXOffset(slot.distance)
            val y = location.y + octant.calcYOffset(slot.distance)

            val lineToPortal = Line(Pos(x, y), location)
            val alpha = reso.calcHealthPercent().toDouble()
            drawResoLine(lineToPortal, resoLevel.getColor(), owner?.faction?.color ?: Faction.NONE.color, 1.0, alpha)

            val resoCircle = Circle(Pos(x, y), Dim.resoRadius)
            DrawUtil.drawCircle(ctx, resoCircle, Colors.black, 2.0, resoLevel.getColor(), alpha)
            if (Styles.isDrawResoLevels) {
                DrawUtil.drawText(ctx, Pos(x, y), reso.level.level.toString(), Colors.black, 8, DrawUtil.CODA)
            }
        }
    }

    fun drawCenter(ctx: Ctx, isDrawHealthBar: Boolean = true) {
        if (HtmlUtil.isNotRunningInBrowser()) return
        val image = getCenterImage(owner?.faction ?: Faction.NONE, getLevel())
        val x = location.x - (image.width / 2)
        val y = location.y - (image.height / 2)
        ctx.drawImage(image, x, y)
        if (isDrawHealthBar) {
            val healthBarImage = getHealthBarImage(owner?.faction ?: Faction.NONE, calcHealth())
            ctx.drawImage(healthBarImage, x, y + image.height + 1)
        }
    }

    fun drawName(ctx: Ctx) {
        if (HtmlUtil.isNotRunningInBrowser()) return
        val xOffset = 34
        val yOffset = 18
        ctx.drawImage(nameImage, location.x - xOffset, location.y + yOffset)
    }

    override fun toString() = name
    override fun equals(other: Any?) = other is Portal && id == other.id
    override fun hashCode() = id.hashCode() * 31

    private val nameImage = if (HtmlUtil.isRunningInBrowser()) createNameImage() else null
    private fun createNameImage(): Canvas? {
        val fontSize = Dim.portalNameFontSize
        val lineWidth = 2.0
        val w = 100
        val h = fontSize + (2 * lineWidth)
        val x = lineWidth + (fontSize / 2)
        val y = lineWidth + (fontSize * 2 / 3)
        return HtmlUtil.preRender(w, h.toInt(), fun(ctx: Ctx) {
            val coords = Pos(x.toInt(), y.toInt())
            DrawUtil.strokeText(
                ctx,
                coords,
                name,
                Colors.white,
                Dim.portalNameFontSize,
                DrawUtil.CODA,
                lineWidth,
                Colors.black
            )
        })
    }

    companion object {
        fun findChargeableForKeys(agent: Agent, keys: List<PortalKey>): List<Portal>? {
            val chargeable = World.factionPortals(agent.faction).filter { it.calcHealth() <= 90 }.toSet()
            return chargeable.filter { keys.map { a -> a.portal }.contains(it) }
        }

        private val centerImages: Map<Pair<Faction, PortalLevel>, Canvas> = if (HtmlUtil.isRunningInBrowser()) {
            PortalLevel.values().flatMap { level ->
                Faction.values().map { (it to level) to renderPortalCenter(it.color, level) }
            }.toMap()
        } else {
            emptyMap()
        }
        private val healthBarImages: Map<Pair<Faction, Int>, Canvas> = if (HtmlUtil.isRunningInBrowser()) {
            (0..100).flatMap { health ->
                val lw = Dim.portalLineWidth
                val r = Dim.portalRadius.toInt()
                val w = (r * 2.0) + (2.0 * lw)
                Faction.values().map { (it to health) to DrawUtil.renderBarImage(it.color, health, 5.0, w, lw) }
            }.toMap()
        } else {
            emptyMap()
        }

        private fun getCenterImage(faction: Faction, level: PortalLevel) = centerImages[faction to level]!!
        private fun getHealthBarImage(faction: Faction, health: Int) = healthBarImages[faction to health]!!

        fun renderPortalCenter(color: String, level: PortalLevel): Canvas {
            val lw = Dim.portalLineWidth
            val r = Dim.portalRadius.toInt()
            val w = (r * 2) + (2 * lw)
            val h = w
            return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
                val portalCircle = Circle(Pos(r + lw, r + lw), r.toDouble())
                DrawUtil.drawCircle(ctx, portalCircle, Colors.black, 2.0, color)
                val pos = Pos(r + lw + if (level.value > 1) 0 else 1, r + lw)
                DrawUtil.drawText(ctx, pos, level.display, Colors.black, 13, DrawUtil.CODA)
            })
        }

        const val MAX_HACKS = 4 //TODO implement multihacks
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))
        fun create(location: Pos): Portal {
            val slots: Slots = Octant.values().map { it to ResonatorSlot.create() }.toMap().toMutableMap()
            val (heatMap, vectorField) = if (HtmlUtil.isRunningInBrowser()) {
                val heatMap = PathUtil.generateHeatMap(location)
                SoundUtil.playPortalCreationSound(location)
                heatMap to PathUtil.calculateVectorField(heatMap, location)
            } else {
                mutableMapOf<Pos, Int>() to mutableMapOf()
            }
            return Portal(
                Util.generatePortalName(), location, heatMap, vectorField,
                slots, mutableSetOf(), mutableSetOf(), null
            )
        }

        fun createRandom(): Portal {
            val location = Pos.createRandomForPortal()
            return create(location)
        }
    }
}
