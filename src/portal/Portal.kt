package portal

import Canvas
import Ctx
import World
import agent.Agent
import agent.Faction
import agent.action.Action
import agent.action.ActionItem
import config.Colors
import config.Dimensions
import config.Styles
import items.PowerCube
import items.QgressItem
import items.XmpBurster
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

data class Portal constructor(val name: String, val location: Coords,
                              val heatMap: Map<Coords, Int>, val vectorField: Map<Coords, Complex>,
                              val resoSlots: MutableMap<Octant, ResonatorSlot>,
                              val links: MutableSet<Link>, val fields: MutableSet<Field>,
                              var owner: Agent?) {
    val lastHacks: MutableMap<String, MutableList<Int>> = mutableMapOf()
    val id: String = "P-" + location.x + ":" + location.y + "-" + name
    fun isDeprecated() = resoSlots.isEmpty()

    fun isUncaptured() = owner == null

    fun isCoveredByField() = World.allFields().filter { it.isCoveringPortal(this) }.isNotEmpty()
    fun isLinkable(agent: Agent): Boolean = this.owner?.faction == agent.faction && isFullyDeployed()
    fun isInside(): Boolean = findConnectedPortals().filter { connected ->
        connected.fields.filter { field -> field.idSet.contains(this) }.count() > 1
    }.isEmpty()

    fun canHack(agent: Agent): Boolean = handleCooldown(agent, true) == Cooldown.NONE
    fun canLinkOut(agent: Agent) = isLinkable(agent) && (links.isEmpty() || links.count() < 8) &&
            !isCoveredByField() && isInside()

    fun calculateLevel() = if (owner == null) 0 else clipLevel(resoSlots.values.map { (it.resonator?.level?.level ?: 0) }.sum() / 8)
    fun getLevel() = PortalLevel.findByValue(calculateLevel())
    fun x() = location.x.toDouble()
    fun y() = location.y.toDouble()
    private fun getAllResos() = resoSlots.map { it.value.resonator }.filterNotNull()
    private fun isFullyDeployed() = resoSlots.map { it.value.resonator }.filterNotNull().count() == 8
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

    fun findResos(): List<Resonator> = resoSlots.map { it.value.resonator }.filterNotNull()
    private fun findStrongestReso() = findResos().sortedBy { it.health * it.level.level }.first()
    fun findStrongestResoPos(): Coords? = findStrongestReso().coords
    fun calcHealth(): Int = findResos().map { it.health }.sum() / findResos().count()
    fun calcTotalXm(): Int = getAllResos().map { it.health * it.level.xmToPortal }.sum()
    fun calculateLinkingRangeInMeters() = {
        val x = averageResoLevel() //kotlin.math.pow?
        if (isFullyDeployed()) 160 * x * x * x * x else 0.0
    }

    fun findOutgoingTo(): List<Portal> = links.map { it.destination }
    fun findIncomingFrom(): List<Portal> = World.allLinks().filter { it.destination == this }.map { it.origin }

    fun findRandomPointNearPortal(distance: Int): Coords {
        val angle = Util.random() * PI
        val xOffset: Int = (distance * cos(angle)).toInt()
        val yOffset: Int = (distance * sin(angle)).toInt()
        val point = location.copy(x = location.x + xOffset, y = location.y + yOffset)
        if (World.grid.get(PathUtil.posToShadowPos(point))?.isPassable ?: false) {
            return point
        } else {
            return findRandomPointNearPortal(distance)
        }
    }

    fun findConnectedPortals(): List<Portal> = findOutgoingTo() + findIncomingFrom()

    fun findLinkableForKeys(keyset: List<PortalKey>, agent: Agent): List<Portal>? {
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
            agent.ap = agent.ap + 187
            agent.xm = agent.xm - 250

            // create fields
            val connectedToTarget = target.findConnectedPortals()
            val connectedToHere = this.findConnectedPortals()
            val anchors = connectedToTarget.filter { connectedToHere.contains(it) }
            anchors.forEach { anchor ->
                if (Field.isPossible(this, target, anchor)) {
                    val newField = Field.create(this, target, anchor, agent)
                    if (newField != null) {
                        Com.addMessage("$agent created a field at $this between $target and $anchor. +$newField")
                        SoundUtil.playFieldingSound(newField)
                        fields.add(newField)
                        agent.ap = agent.ap + 1250
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

        agent.xm = agent.xm - (50 * this.calculateLevel())
        if (owner != null && agent.faction != owner?.faction) {
            agent.ap = agent.ap + 100
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
            while (Util.random() < (1 / it.roll)) {
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
            val timeDiff = Util.secondsToTicks(Cooldown.FIVE.seconds).toInt() - ticksSinceLastHack
            val cooldown = Cooldown.valueOf(Util.ticksToSeconds(timeDiff).toInt())
            if (cooldown == Cooldown.NONE && !readOnly) {
                agentsLastHacks.add(tickNr)
                lastHacks.put(key, mutableListOf(tickNr))
            }
            return cooldown
        }

        fun burn(agentsLastHacks: MutableList<Int>, tickNr: Int): Cooldown {
            val maxBurnoutTicks = Util.secondsToTicks(Cooldown.BURNOUT.seconds)
            val maxTickDifference = tickNr - maxBurnoutTicks
            val isBurnout = agentsLastHacks.toList().filter { it < maxTickDifference }.count() <= 0
            if (isBurnout) {
                return Cooldown.BURNOUT
            } else {
                if (!readOnly) {
                    agentsLastHacks.add(tickNr)
                    lastHacks.put(key, mutableListOf(tickNr))
                }
                return Cooldown.NONE //reset
            }
        }

        val isFirstHack = !lastHacks.containsKey(key)
        if (isFirstHack) {
            if (!readOnly) {
                lastHacks.put(key, mutableListOf(World.tick))
            }
            return Cooldown.NONE
        } else {
            val agentsLastHacks: MutableList<Int> = lastHacks.get(key)!!
            if (agentsLastHacks.count() < MAX_HACKS) {
                return cool(agentsLastHacks, World.tick)
            } else {
                return burn(agentsLastHacks, World.tick)
            }
        }
    }

    fun deploy(agent: Agent, resos: Map<Octant, Resonator>, distance: Int) {
        if (owner == null) {
            owner = agent
            Com.addMessage("$agent captured $this.")
            agent.ap = agent.ap + 500
        }
        resos.forEach { (octant, resonator) ->
            val oldDistance = resoSlots.get(octant)?.distance
            val newDistance = (if (oldDistance == 0) distance else oldDistance) ?: distance
            //println("DEBUG: $agent deploys ${resonator} to $octant at $this. $distance $oldDistance")
            val slot = ResonatorSlot(agent.key(), resonator, newDistance)
            resoSlots.put(octant, slot)
            val xx = location.x + octant.calcXOffset(slot.distance)
            val yy = location.y + octant.calcYOffset(slot.distance)
            resonator.deploy(this, octant, Coords(xx, yy))
        }
        agent.inventory.consumeResos(resos.map { it.value })
    }

    fun destroy(tick: Int) {
        resoSlots.clear()
        links.clear()
        fields.clear()
        owner = null
        findIncomingFrom().forEach { connctedPortal ->
            connctedPortal.links.forEach { link ->
                if (link.destination == this || link.origin == this) {
                    connctedPortal.links.remove(link)
                }
            }
            connctedPortal.fields.forEach { field ->
                if (field.idSet.contains(this)) {
                    connctedPortal.fields.remove(field)
                }
            }
        }
        World.allAgents.forEach { agent ->
            val portalKeys: List<PortalKey>? = agent.inventory.findKeys()?.filter { key -> key.portal.equals(this) }?.toList()
            if (portalKeys != null) {
                agent.inventory.items.removeAll(portalKeys)
            }
            if (agent.actionPortal == this) {
                agent.actionPortal = World.allPortals.first()
                agent.action = Action.start(ActionItem.WAIT, tick)
            }
        }
        World.allPortals.remove(this)
    }

    fun removeReso(octant: Octant) {
        resoSlots.put(octant, ResonatorSlot(null, null, 0))
        val numberOfResosLeft = resoSlots.filter { it.value.resonator != null }.count()
        if (numberOfResosLeft < 2) {
            findConnectedPortals().forEach { connctedPortal ->
                connctedPortal.links.forEach { link ->
                    if (link.destination == this) {
                        connctedPortal.links.remove(link)
                    }
                }
                connctedPortal.fields.forEach { field ->
                    if (field.primaryAnchor == this || field.secondaryAnchor == this) {
                        connctedPortal.fields.remove(field)
                    }
                }
            }
            links.clear()
            fields.clear()
        }
        if (numberOfResosLeft <= 0) {
            owner = null
        }
    }

    fun findAllowedResoLevels(agent: Agent): Map<ResonatorLevel, Int> {
        if (owner == null || owner?.faction == agent.faction) {
            return ResonatorLevel.values().map { level ->
                level to level.deployablePerPlayer - resoSlots.filter { slot ->
                    slot.value.isOwnedBy(agent) && slot.value.resonator?.level?.level == level.level
                }.count()
            }.toMap()
        } else {
            return mapOf()
        }
    }

    fun leakXm(): Int = (calcTotalXm() * XM_LEAK).toInt()
    fun decayResonators(): Unit = getAllResos().forEach { it.decay() }

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
        }.map { it.key to it.value }.toList()
        octantSlots.map { octantSlot ->
            val octant: Octant = octantSlot.first
            val slot = octantSlot.second
            val reso = slot.resonator!!
            val resoLevel = reso.level
            val x = location.x + octant.calcXOffset(slot.distance)
            val y = location.y + octant.calcYOffset(slot.distance)

            val lineToPortal = Line(Coords(x, y), location)
            val alpha = reso.health / 100.0
            drawResoLine(lineToPortal, resoLevel.color, owner?.faction?.color ?: Faction.NONE.color, 1.0, alpha)

            val resoCircle = Circle(Coords(x, y), Dimensions.resoRadius)
            DrawUtil.drawCircle(ctx, resoCircle, Colors.black, 2.0, resoLevel.color, alpha)
            if (Styles.isDrawResoLevels) {
                DrawUtil.drawText(ctx, Coords(x, y), reso.level.level.toString(), Colors.black, 8, DrawUtil.CODA)
            }
        }
    }

    fun drawCenter(ctx: Ctx) {
        val image = getCenterImage(owner?.faction ?: Faction.NONE, getLevel())
        ctx.drawImage(image, location.xx() - (image.width / 2), location.yy() - (image.height / 2))
    }

    fun drawName(ctx: Ctx) {
        val xOffset = 34
        val yOffset = 18
        ctx.drawImage(nameImage, location.xx() - xOffset, location.yy() + yOffset)
    }

    override fun equals(other: Any?) = other is Portal && id.equals(other.id)
    override fun hashCode() = id.hashCode() * 31
    override fun toString() = name

    val nameImage = createNameImage()
    private fun createNameImage(): Canvas {
        val fontSize = Dimensions.portalNameFontSize
        val lineWidth = 2.0
        val w = 100
        val h = fontSize + (2 * lineWidth)
        val x = lineWidth + (fontSize / 2)
        val y = lineWidth + (fontSize * 2 / 3)
        return HtmlUtil.prerender(w, h.toInt(), fun(ctx: Ctx) {
            val coords = Coords(x.toInt(), y.toInt())
            DrawUtil.strokeText(ctx, coords, name, Colors.white, Dimensions.portalNameFontSize, DrawUtil.CODA, lineWidth, Colors.black)
        })
    }

    companion object {
        private val centerImages: Map<Pair<Faction, PortalLevel>, Canvas> = PortalLevel.values().flatMap { level ->
            Faction.values().map { faction ->
                (faction to level) to renderPortalCenter(faction, level)
            }
        }.toMap()

        private fun getCenterImage(faction: Faction, level: PortalLevel) = centerImages.get(faction to level)!!

        fun renderPortalCenter(faction: Faction, level: PortalLevel): Canvas {
            return renderPortalCenter(faction.color, level)
        }

        fun renderPortalCenter(color: String, level: PortalLevel): Canvas {
            val lineWidth = 2
            val r = Dimensions.portalRadius.toInt()
            val w = r * 2 + (2 * lineWidth)
            val h = w
            return HtmlUtil.prerender(w, h, fun(ctx: Ctx) {
                val portalCircle = Circle(Coords(r + lineWidth, r + lineWidth), r.toDouble())
                DrawUtil.drawCircle(ctx, portalCircle, Colors.black, 2.0, color)
                val pos = Coords(r + lineWidth + if (level.value > 1) 0 else 1, r + lineWidth)
                DrawUtil.drawText(ctx, pos, level.toString(), Colors.black, 13, DrawUtil.CODA)
            })
        }

        val XM_LEAK = 0.2
        val XM_LEAK_FREQ_MIN = 20
        val XM_LEAK_RADIUS_M = 40
        val DECAY_FREQ_H = 24

        val MAX_HACKS = 4 //TODO implement multihacks
        private fun clipLevel(level: Int): Int = max(1, min(level, 8))
        fun create(location: Coords): Portal {
            val emptySlot = ResonatorSlot(null, null, 0)
            val slots: MutableMap<Octant, ResonatorSlot> = Octant.values().map { it to emptySlot }.toMap().toMutableMap()
            val heatMap = PathUtil.generateHeatMap(location)
            val vectorField = PathUtil.calculateVectorField(heatMap)
            return Portal(Util.generatePortalName(), location, heatMap, vectorField,
                    slots, mutableSetOf(), mutableSetOf(), null)
        }
        fun createRandom(): Portal {
            val location = Coords.createRandomForPortal()
            return create(location)
        }
    }
}
