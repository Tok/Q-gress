package portal

import Canvas
import Ctx
import World
import agent.Agent
import agent.Faction
import agent.action.ActionItem
import config.Colors
import config.Dim
import config.Styles
import config.Time
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
import util.data.Complex
import util.data.Coords
import util.data.Line
import kotlin.math.*

data class Portal(val name: String, val location: Coords,
                  val heatMap: Map<Coords, Int>, val vectorField: Map<Coords, Complex>,
                  val resoSlots: MutableMap<Octant, ResonatorSlot>,
                  val links: MutableSet<Link>, val fields: MutableSet<Field>,
                  var owner: Agent?) {
    private val lastHacks: MutableMap<String, MutableList<Int>> = mutableMapOf()
    val id: String = "P-" + location.x + ":" + location.y + "-" + name
    fun isDeprecated() = resoSlots.isEmpty()

    fun isUncaptured() = owner == null
    fun isEnemyOf(agent: Agent) = owner != null && owner?.faction != agent.faction
    fun isFriendlyTo(agent: Agent) = owner != null && owner?.faction == agent.faction

    private fun isCoveredByField() = World.allFields().any { it.isCoveringPortal(this) }
    private fun isLinkable(agent: Agent): Boolean = this.owner?.faction == agent.faction && isFullyDeployed()
    private fun isInside(): Boolean = findConnectedPortals().none { connected ->
        connected.fields.filter { field -> field.idSet.contains(this) }.count() > 1
    }

    fun canHack(agent: Agent): Boolean = handleCooldown(agent, true) == Cooldown.NONE
    fun canLinkOut(agent: Agent) = isLinkable(agent) && (links.isEmpty() || links.count() < 8) &&
            !isCoveredByField() && isInside()

    private fun calculateLevel() = if (owner == null) 1 else clipLevel(resoSlots.values.map {
        (it.resonator?.level?.level ?: 0)
    }.sum() / 8)

    fun getLevel() =
            if (World.isReady) PortalLevel.findByValue(calculateLevel())
            else PortalLevel.ZERO

    fun x() = location.x.toDouble()
    fun y() = location.y.toDouble()

    private fun getAllResos() = resoSlots.map { it.value.resonator }.filterNotNull()
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

    fun findStrongestResoPos(): Coords? = findStrongestReso()?.coords
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

    private fun findOutgoingTo(): List<Portal> = links.map { it.destination }
    private fun findIncomingFrom(): List<Portal> = World.allLinks().filter { it.destination == this }.map { it.origin }

    fun findRandomPointNearPortal(distance: Int): Coords {
        val angle = Util.random() * PI
        val xOffset: Int = (distance * cos(angle)).toInt()
        val yOffset: Int = (distance * sin(angle)).toInt()
        val point = location.copy(x = location.x + xOffset, y = location.y + yOffset)
        return if (World.grid[PathUtil.posToShadowPos(point)]?.isPassable == true) {
            point
        } else {
            findRandomPointNearPortal(distance)
        }
    }

    private fun findConnectedPortals(): List<Portal> = findOutgoingTo() + findIncomingFrom()

    fun findLinkableForKeys(agent: Agent): List<Portal>? {
        val keyset = agent.inventory.findUniqueKeys()!!
        val allLinks = World.allPortals.flatMap { it.links }.filter { Link.isPossible(it) }.toSet()
        val nonIntersecting: List<Portal> = keyset.map { it.portal }.filter { destination ->
            val line = Line(location, destination.location)
            allLinks.filter { it.getLine().doesIntersect(line) }.isEmpty()
        }
        return nonIntersecting.filter { it.isLinkable(agent) }
    }

    fun createLink(agent: Agent, target: Portal) {
        val newLink: Link? = Link.create(this, target, agent)
        if (newLink != null) {
            // create link
            links.add(newLink)
            agent.inventory.consumeKeyToPortal(target)
            Com.addMessage("$agent created a link from $this to $target")
            SoundUtil.playLinkingSound(newLink)
            agent.addAp(187)
            agent.removeXm(250)

            // create fields
            val connectedToTarget = target.findConnectedPortals()
            val connectedToHere = this.findConnectedPortals()
            val anchors = connectedToTarget.filter { connectedToHere.contains(it) }
            anchors.forEach { anchor ->
                if (Field.isPossible(this, target, anchor)) {
                    val newField = Field.create(this, target, anchor, agent)
                    if (newField != null) {
                        Com.addMessage("$agent created a field at $this. +$newField")
                        SoundUtil.playFieldingSound(newField)
                        fields.add(newField)
                        agent.addAp(1250)
                    }
                }
            }
        }
    }

    fun tryHack(agent: Agent): HackResult {
        val cooldown = handleCooldown(agent, false)
        if (cooldown == Cooldown.NONE) {
            val stuff = hack(agent)
            return HackResult(stuff, null)
        }
        return HackResult(null, cooldown)
    }

    private fun hack(agent: Agent): MutableList<QgressItem> {
        val level = min(calculateLevel(), agent.getLevel())

        val newStuff = mutableListOf<QgressItem?>()
        newStuff.addAll(obtainResos(level, agent))
        newStuff.addAll(obtainXmps(level, agent))
        newStuff.addAll(obtainShields(agent))
        newStuff.addAll(obtainVirus(agent))
        newStuff.addAll(obtainPowerCubes(level, agent))
        newStuff.add(PortalKey.tryHack(this, agent))

        val isEnemyPortal = owner != null && agent.faction != owner?.faction
        if (isEnemyPortal) {
            agent.addAp(100)
            agent.removeXm(300 * this.calculateLevel())
        } else {
            agent.removeXm(50 * this.calculateLevel())
        }

        return newStuff.filterNotNull().toMutableList()
    }

    private fun obtainResos(level: Int, agent: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        Quality.values().map { quality ->
            val selectedLevel = ResonatorLevel.find(level, quality).level
            while (Util.random() < quality.chance) {
                stuff.add(Resonator.create(selectedLevel, agent) as QgressItem)
            }
        }
        return stuff
    }

    private fun obtainXmps(level: Int, agent: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        Quality.values().map { quality ->
            val selectedLevel = XmpLevel.find(level, quality).level
            while (Util.random() < quality.chance) {
                stuff.add(XmpBurster.create(selectedLevel, agent) as QgressItem)
            }
        }
        return stuff
    }

    private fun obtainShields(agent: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        ShieldType.values().forEach {
            if (Util.random() < it.chance) {
                stuff.add(Shield(it, agent))
            }
        }
        return stuff
    }

    private fun obtainPowerCubes(level: Int, agent: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        Quality.values().map { quality ->
            val selectedLevel = PowerCubeLevel.find(level, quality).level
            while (Util.random() < quality.chance * 0.3) {
                stuff.add(PowerCube.create(selectedLevel, agent) as QgressItem)
            }
        }
        return stuff
    }

    private fun obtainVirus(agent: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        VirusType.values().forEach {
            while (Util.random() < (1 / it.roll)) {
                stuff.add(Virus(it, agent))
            }
        }
        return stuff
    }

    private fun handleCooldown(agent: Agent, readOnly: Boolean): Cooldown {
        //a result of NONE should add a the ticknumber to the list of the last hacks
        val key = agent.key()
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

    fun deployMods(agent: Agent, @Suppress("UNUSED_PARAMETER") mods: Map<Octant, DeployableItem>) {
        val isCommon = true //TODO implement
        val isRare = false
        val isVeryRare = false
        if (isCommon) {
            agent.removeXm(400)
        }
        if (isRare) {
            agent.removeXm(800)
        }
        if (isVeryRare) {
            agent.removeXm(1000)
        }
    }

    fun deploy(agent: Agent, resos: Map<Octant, Resonator>, distance: Int) {
        val isCapture = owner == null
        if (isCapture) {
            owner = agent
            Com.addMessage("$agent captured $this.")
        }

        val initialResoCount = resoSlots.filterValues { !it.isEmpty() }.filterNot { it.value.resonator == null }.size
        val firstResoCount = max(resos.size, (8 - initialResoCount))
        resos.asIterable().forEachIndexed { index, (octant, resonator) ->
            val oldReso = resoSlots[octant]
            if (isCapture && index == 0) {
                agent.addAp(500)
            } else if (index < firstResoCount) {
                agent.addAp(125)
            } else if (index == firstResoCount && firstResoCount + initialResoCount == 8) {
                agent.addAp(250)
            } else if (oldReso?.isOwnedBy(agent) != true) {
                agent.addAp(65)
            }
            agent.removeXm(resonator.level.level * 20)
            val oldDistance = oldReso?.distance
            val newDistance = (if (oldDistance == 0) distance else oldDistance) ?: distance
            //println("DEBUG: $agent deploys ${resonator} to $octant at $this. $distance $oldDistance")
            val slot = ResonatorSlot(agent.key(), resonator, newDistance)
            resoSlots[octant] = slot
            val xx = location.x + octant.calcXOffset(slot.distance)
            val yy = location.y + octant.calcYOffset(slot.distance)
            resonator.deploy(this, octant, Coords(xx, yy))
        }
        agent.inventory.consumeResos(resos.map { it.value })
    }

    fun destroy(tick: Int, isRemovePortal: Boolean) {
        if (isRemovePortal) {
            resoSlots.clear()
        }
        links.clear()
        fields.clear()
        owner = null
        findIncomingFrom().forEach { connectedPortal ->
            connectedPortal.links.forEach { link ->
                if (link.destination == this || link.origin == this) {
                    connectedPortal.links.remove(link)
                }
            }
            connectedPortal.fields.forEach { field ->
                if (field.idSet.contains(this)) {
                    connectedPortal.fields.remove(field)
                }
            }
        }
        World.allAgents.forEach { agent ->
            val portalKeys: List<PortalKey>? = agent.inventory.findKeys().filter { key -> key.portal == this }.toList()
            if (portalKeys != null) {
                agent.inventory.items.removeAll(portalKeys)
            }
            if (agent.actionPortal == this) {
                agent.actionPortal = World.allPortals.first()
                agent.action.item = ActionItem.WAIT
                agent.action.untilTick = tick + 1
            }
        }
        if (isRemovePortal) {
            World.allPortals.remove(this)
        }
    }

    fun removeReso(octant: Octant, agent: Agent?) {
        resoSlots[octant] = ResonatorSlot(null, null, 0)
        val numberOfResosLeft = resoSlots.filter { it.value.resonator != null }.count()
        if (numberOfResosLeft < 2) {
            findConnectedPortals().forEach { connctedPortal ->
                connctedPortal.links.forEach { link ->
                    if (link.destination == this) {
                        agent?.addAp(187)
                        connctedPortal.links.remove(link)
                    }
                }
                connctedPortal.fields.forEach { field ->
                    if (field.primaryAnchor == this || field.secondaryAnchor == this) {
                        agent?.addAp(750)
                        connctedPortal.fields.remove(field)
                    }
                }
            }
            links.clear()
            fields.clear()
        }
        if (numberOfResosLeft <= 0) {
            destroy(World.tick, false)
        }
    }

    fun findAllowedResoLevels(agent: Agent): Map<ResonatorLevel, Int> {
        return if (owner == null || owner?.faction == agent.faction) {
            ResonatorLevel.values().map { level ->
                level to level.deployablePerPlayer - resoSlots.filter { slot ->
                    slot.value.isOwnedBy(agent) && slot.value.resonator?.level?.level == level.level
                }.count()
            }.toMap()
        } else {
            mapOf()
        }
    }

    fun leakXm(): Pair<Coords, Int> {
        val fluct = Util.randomInt(300)
        val offset = if (Util.randomBool()) fluct else -fluct
        return location to if (getLevel().toInt() <= 4.5) {
            (calculateLevel() * 1000) + offset
        } else {
            (calculateLevel() * 750) + offset
        }
    }

    fun decay() {
        getAllResos().forEach { it.decay() }
        if (getAllResos().isEmpty()) {
            destroy(World.tick, false)
        }
    }

    fun drawResonators(ctx: Ctx) {
        fun drawResoLine(line: Line, levelColor: String, factionColor: String, lineWidth: Double, alpha: Double = 1.0) {
            ctx.globalAlpha = alpha
            ctx.strokeStyle = Colors.black
            ctx.lineWidth = lineWidth + 1.5
            ctx.beginPath()
            ctx.moveTo(line.from.xx(), line.from.yy())
            ctx.lineTo(line.to.xx(), line.to.yy())
            ctx.closePath()
            ctx.stroke()
            ctx.lineWidth = lineWidth
            if (Styles.isDrawResoLineGradient) { //CPU intensive
                val gradient = ctx.createLinearGradient(line.from.xx(), line.from.yy(), line.to.xx(), line.to.yy())
                gradient.addColorStop(0.2, levelColor)
                gradient.addColorStop(0.7, factionColor)
                ctx.strokeStyle = gradient
            } else {
                ctx.strokeStyle = levelColor
            }
            ctx.beginPath()
            ctx.moveTo(line.from.xx(), line.from.yy())
            ctx.lineTo(line.to.xx(), line.to.yy())
            ctx.closePath()
            ctx.stroke()
            ctx.globalAlpha = 1.0
        }

        val octantSlots: List<Pair<Octant, ResonatorSlot>> = resoSlots.filter {
            it.value.owner != null && it.value.resonator != null
        }.toList()
        octantSlots.map { octantSlot ->
            val octant: Octant = octantSlot.first
            val slot = octantSlot.second
            val reso = slot.resonator!!
            val resoLevel = reso.level
            val x = location.x + octant.calcXOffset(slot.distance)
            val y = location.y + octant.calcYOffset(slot.distance)

            val lineToPortal = Line(Coords(x, y), location)
            val alpha = reso.calcHealthPercent().toDouble()
            drawResoLine(lineToPortal, resoLevel.getColor(), owner?.faction?.color ?: Faction.NONE.color, 1.0, alpha)

            val resoCircle = Circle(Coords(x, y), Dim.resoRadius)
            DrawUtil.drawCircle(ctx, resoCircle, Colors.black, 2.0, resoLevel.getColor(), alpha)
            if (Styles.isDrawResoLevels) {
                DrawUtil.drawText(ctx, Coords(x, y), reso.level.level.toString(), Colors.black, 8, DrawUtil.CODA)
            }
        }
    }

    fun drawCenter(ctx: Ctx, isDrawHealthBar: Boolean = true) {
        val image = getCenterImage(owner?.faction ?: Faction.NONE, getLevel())
        val x = location.xx() - (image.width / 2)
        val y = location.yy() - (image.height / 2)
        ctx.drawImage(image, x, y)
        if (isDrawHealthBar) {
            val healthBarImage = getHealthBarImage(owner?.faction ?: Faction.NONE, calcHealth())
            ctx.drawImage(healthBarImage, x, y + image.height + 1)
        }
    }

    fun drawName(ctx: Ctx) {
        val xOffset = 34
        val yOffset = 18
        ctx.drawImage(nameImage, location.xx() - xOffset, location.yy() + yOffset)
    }

    override fun toString() = name
    override fun equals(other: Any?) = other is Portal && id == other.id
    override fun hashCode() = id.hashCode() * 31

    private val nameImage = createNameImage()
    private fun createNameImage(): Canvas {
        val fontSize = Dim.portalNameFontSize
        val lineWidth = 2.0
        val w = 100
        val h = fontSize + (2 * lineWidth)
        val x = lineWidth + (fontSize / 2)
        val y = lineWidth + (fontSize * 2 / 3)
        return HtmlUtil.preRender(w, h.toInt(), fun(ctx: Ctx) {
            val coords = Coords(x.toInt(), y.toInt())
            DrawUtil.strokeText(ctx, coords, name, Colors.white, Dim.portalNameFontSize, DrawUtil.CODA, lineWidth, Colors.black)
        })
    }

    companion object {
        fun findChargeableForKeys(agent: Agent): List<Portal>? {
            if (!agent.hasKeys()) {
                return listOf()
            }
            val chargeable = World.factionPortals(agent.faction).filter { it.calcHealth() <= 90 }.toSet()
            return chargeable.filter { agent.keySet()!!.map { a -> a.portal }.contains(it) }
        }

        private val centerImages: Map<Pair<Faction, PortalLevel>, Canvas> = PortalLevel.values().flatMap { level ->
            Faction.values().map { (it to level) to renderPortalCenter(it.color, level) }
        }.toMap()
        private val healthBarImages: Map<Pair<Faction, Int>, Canvas> = (0..100).flatMap { health ->
            val lw = Dim.portalLineWidth
            val r = Dim.portalRadius.toInt()
            val w = (r * 2) + (2 * lw)
            Faction.values().map { (it to health) to DrawUtil.renderBarImage(it.color, health, 5, w, lw) }
        }.toMap()

        private fun getCenterImage(faction: Faction, level: PortalLevel) = centerImages[faction to level]!!
        private fun getHealthBarImage(faction: Faction, health: Int) = healthBarImages[faction to health]!!

        fun renderPortalCenter(color: String, level: PortalLevel): Canvas {
            val lw = Dim.portalLineWidth
            val r = Dim.portalRadius.toInt()
            val w = (r * 2) + (2 * lw)
            val h = w
            return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
                val portalCircle = Circle(Coords(r + lw, r + lw), r.toDouble())
                DrawUtil.drawCircle(ctx, portalCircle, Colors.black, 2.0, color)
                val pos = Coords(r + lw + if (level.value > 1) 0 else 1, r + lw)
                DrawUtil.drawText(ctx, pos, level.display, Colors.black, 13, DrawUtil.CODA)
            })
        }

        val emptySlot = ResonatorSlot(null, null, 0)
        const val MAX_HACKS = 4 //TODO implement multihacks
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))
        fun create(location: Coords): Portal {
            val slots: MutableMap<Octant, ResonatorSlot> = Octant.values().map { it to emptySlot }.toMap().toMutableMap()
            val heatMap = PathUtil.generateHeatMap(location)
            val vectorField = PathUtil.calculateVectorField(heatMap)
            SoundUtil.playPortalCreationSound(location)
            return Portal(Util.generatePortalName(), location, heatMap, vectorField,
                    slots, mutableSetOf(), mutableSetOf(), null)
        }

        fun createRandom(): Portal {
            val location = Coords.createRandomForPortal()
            return create(location)
        }
    }
}
