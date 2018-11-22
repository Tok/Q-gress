package agent

import Canvas
import Ctx
import World
import agent.action.Action
import agent.action.ActionItem
import agent.action.ActionSelector
import agent.qvalue.QDestinations
import config.Colors
import config.Config
import config.Dim
import config.Styles
import items.PowerCube
import items.QgressItem
import items.deployable.Resonator
import items.level.ResonatorLevel
import items.level.XmpLevel
import portal.Link
import portal.Portal
import portal.XmMap
import system.Com
import system.Queues
import util.DrawUtil
import util.PathUtil
import util.SoundUtil
import util.Util
import util.data.*
import kotlin.math.max
import kotlin.math.min

data class Agent(val faction: Faction, val name: String, val pos: Coords, val skills: Skills,
                 val inventory: Inventory, val action: Action, var actionPortal: Portal, var destination: Coords,
                 var ap: Int = 0, var xm: Int = 0, var velocity: Complex = Complex.ZERO) {
    fun key() = toString()
    private fun distanceToDestination(): Double = pos.distanceTo(destination)
    fun distanceToPortal(portal: Portal): Double = pos.distanceTo(portal.location)
    fun isAtActionPortal(): Boolean = distanceToPortal(actionPortal) < Dim.maxDeploymentRange
    private fun isInAttackRange(range: Int): Boolean {
        val strongest = actionPortal.findStrongestResoPos()
        return strongest != null && pos.distanceTo(strongest) < range
    }

    private fun isBusy(): Boolean = World.tick <= action.untilTick
    private fun lineToPortal(portal: Portal) = Line(pos, portal.location)
    private fun lineToDestination() = Line(pos, destination)

    fun getLevel(): Int = getLevel(this.ap)
    fun xmCapacity(): Int = xmCapacity(getLevel())

    private fun calcAbsXmBar() = min(xmCapacity(), max(0, xm))
    private fun xmBarPercent() = calcAbsXmBar() * 100 / xmCapacity()
    private fun isXmBarEmpty() = xmBarPercent() == 0
    fun isXmFilled() = xmBarPercent() >= 80

    fun removeXm(v: Int) {
        this.xm = Util.clip(xm - v, 0, xmCapacity())
    }

    fun addXm(v: Int) {
        this.xm = Util.clip(xm + v, 0, xmCapacity())
    }

    fun addAp(v: Int) {
        this.ap += v * Config.apMultiplier
    }

    private fun isFastAction(): Boolean = action.item == ActionItem.MOVE || (action.item == ActionItem.ATTACK && !isAtActionPortal())
    private fun isMoveInRange(): Boolean = action.item == ActionItem.ATTACK && !isArrived()

    fun act(): Agent {
        val next = when {
            isBusy() -> this
            isFastAction() -> moveCloserToDestinationPortal()
            isMoveInRange() -> moveCloserInRange()
            else -> ActionSelector.doSomething(this)
        }
        next.collectXm()
        return next
    }

    fun isHackPossible() = actionPortal.canHack(this)
    fun isDeploymentPossible() = !actionPortal.isEnemyOf(this)
            && actionPortal.findAllowedResoLevels(this).map { it.value }.sum() > 0

    fun moveElsewhere(): Agent {
        val agent = this
        val hasEnemyPortals = MovementUtil.hasEnemyPortals(agent)
        with(QDestinations) {
            val randomQ = ActionSelector.q(faction, MOVE_TO_RANDOM)
            val nearQ = ActionSelector.q(faction, MOVE_TO_NEAR)
            val uncapturedQ = if (MovementUtil.hasUncapturedPortals()) ActionSelector.q(faction, MOVE_TO_UNCAPTURED) else -1.0
            val friendlyQ = if (MovementUtil.hasFriendlyPortals(agent)) ActionSelector.q(faction, MOVE_TO_MOST_FRIENDLY) else -1.0
            val nearEnemyQ = if (hasXmps() && hasEnemyPortals) ActionSelector.q(faction, MOVE_TO_NEAR_ENEMY) else -1.0
            val weakEnemyQ = if (hasXmps() && hasEnemyPortals) ActionSelector.q(faction, MOVE_TO_WEAK_ENEMY) else -1.0
            val strongEnemyQ = if (hasXmps() && hasEnemyPortals) ActionSelector.q(faction, MOVE_TO_STRONG_ENEMY) else -1.0
            val qValues = listOf(
                    randomQ to { MovementUtil.moveToRandomPortal(agent) },
                    nearQ to { MovementUtil.moveToNearestPortal(agent) },
                    uncapturedQ to { MovementUtil.moveToUncapturedPortal(agent) },
                    friendlyQ to { MovementUtil.moveToFriendlyHighLevelPortal(agent) },
                    nearEnemyQ to { MovementUtil.attackClosePortal(agent) },
                    weakEnemyQ to { MovementUtil.attackMostVulnerablePortal(agent) },
                    strongEnemyQ to { MovementUtil.attackMostLinkedPortal(agent) }
            )
            val newAgent = Util.select(qValues, { MovementUtil.moveToNearestPortal(agent) }).invoke()
            newAgent.action.start(ActionItem.MOVE)
            return newAgent
        }
    }

    private fun moveCloserToDestinationPortal(): Agent {
        if (!World.isReady) {
            println("WARN: moveCloserToDestination: World is not ready.")
            return doNothing()
        }
        if (isAtActionPortal()) {
            return if (action.item == ActionItem.ATTACK) this else doNothing()
        }

        val force = actionPortal.vectorField[PathUtil.posToShadowPos(pos)]
        velocity = MovementUtil.move(velocity, force, skills.speed)
        return this.copy(pos = Coords((pos.x + velocity.re).toInt(), (pos.y + velocity.im).toInt()))
    }

    private fun hasXmps() = inventory.findXmps().isNotEmpty()

    private fun isArrived() = distanceToDestination() <= skills.inRangeSpeed()
    private fun moveCloserInRange(): Agent = moveCloserTo(destination)
    private fun moveCloserTo(dest: Coords): Agent {
        val part = skills.inRangeSpeed() / pos.distanceTo(dest)
        val rawDiffX = (pos.xDiff(dest) * part).toInt()
        val rawDiffY = (pos.yDiff(dest) * part).toInt()
        val rawNextX = pos.x - rawDiffX
        val rawNextY = pos.y - rawDiffY
        return this.copy(pos = Coords(rawNextX, rawNextY))
    }

    private fun collectXm() {
        val heaps = XmMap.findXmInRange(pos)
        heaps.forEach { heap ->
            if (xm < xmCapacity()) {
                addXm(heap.value.xm)
                heap.value.collect()
            }
        }
    }

    fun rechargePortal(): Agent {
        if (!hasKeys()) {
            return this
        }
        val chargeable = Portal.findChargeableForKeys(this)
        if (chargeable!!.isEmpty()) {
            return this
        }
        val lowest: Portal? = chargeable.sortedBy { it.calcHealth() }.first()
        if (lowest != null) {
            val resos = lowest.resoSlots.mapNotNull { it.value.resonator }
            resos.forEach { it.recharge(this, 1000 / resos.count()) }
        }
        return this
    }

    fun recruitNewAgents(): Agent {
        if (action.item != ActionItem.RECRUIT) {
            this.action.start(ActionItem.RECRUIT)
            if (Util.random() < NonFaction.changeToBeRecruited) {
                val npc = NonFaction.findNearestTo(pos)
                World.allNonFaction.remove(npc)
                val newAgent = when (faction) {
                    Faction.ENL -> Agent.createFrog(World.grid)
                    Faction.RES -> Agent.createSmurf(World.grid)
                    else -> throw IllegalStateException("$this is $faction NPC.")
                }
                Com.addMessage("$newAgent has completed the tutorial.")
                World.allAgents.add(newAgent)
            }
        }
        return this
    }

    fun recycleItems(): Agent {
        //TODO improve
        val cubes: List<PowerCube> = inventory.findPowerCubes()
        if (cubes.isNotEmpty()) {
            val cube: PowerCube = cubes.first()
            addXm(cube.level.calculateRecycleXm())
            inventory.consumeCubes(listOf(cube))
        }
        return this
    }

    fun attackPortal(): Agent {
        fun findExactDestination(): Coords {
            if (actionPortal.calcHealth() > 0.5) {
                return actionPortal.location
            }
            val maybeDestination = actionPortal.findStrongestResoPos()
            val isPassable = maybeDestination != null && maybeDestination.isPassable()
            return if (isPassable) {
                maybeDestination!!
            } else {
                actionPortal.location
            }
        }

        fun doAttack(): Agent {
            val maxXmps = 12
            val allXmps = inventory.findXmps()
            val selectedXmps = allXmps.sortedBy { it.level }.take(min(maxXmps, allXmps.size))
            if (selectedXmps.isEmpty()) {
                action.end()
                return this
            }
            selectedXmps.forEach { xmpBurster ->
                when (xmpBurster.level.level) {
                    1 -> removeXm(10)
                    2 -> removeXm(20)
                    3 -> removeXm(70)
                    4 -> removeXm(140)
                    5 -> removeXm(250)
                    6 -> removeXm(360)
                    7 -> removeXm(490)
                    else -> removeXm(640)
                }
            }
            Queues.registerAttack(this, selectedXmps)
            inventory.consumeXmps(selectedXmps)
            action.end()
            return this
        }

        return when {
            !isAtActionPortal() -> moveCloserToDestinationPortal()
            !isArrived() -> moveCloserInRange()
            else -> doAttack()
        }
    }

    fun deployPortal(): Agent {
        fun findExactDestination(): Coords {
            val distance = skills.deployPrecision * Dim.maxDeploymentRange
            return actionPortal.findRandomPointNearPortal(distance.toInt())
        }

        fun doDeploy(): Agent {
            if (actionPortal.isEnemyOf(this)) {
                return doNothing()
            }
            val allowedResoLevels: Map<ResonatorLevel, Int> = actionPortal.findAllowedResoLevels(this)
            val areMoreResosAllowed = allowedResoLevels.map { it.value }.sum() > 0
            if (areMoreResosAllowed) {
                val ownedInPortal = actionPortal.resoSlots.filter { it.value.isOwnedBy(this) }.toList()
                val inventoryResos = inventory.items.filter { it is Resonator }.map { it as Resonator }.sortedBy { it.level }
                val deployLowFirstSet = inventoryResos.toSet()

                //in one move, deployActionPortal as many resonators as possible of one selected level
                deployLowFirstSet.forEach { reso ->
                    val owned = ownedInPortal.filter { slot -> slot.second.resonator?.level?.level ?: 0 >= reso.level.level }.count()
                    val maxDeployable: Int = max(reso.level.deployablePerPlayer - owned, 0)
                    val levelResos = inventoryResos.filter { it.level.level == reso.level.level && it.level.level <= this.getLevel() }
                    if (levelResos.isNotEmpty()) {
                        val resos = levelResos.take(min(maxDeployable, levelResos.size - 1))
                        if (resos.isNotEmpty()) {
                            val deployable = actionPortal.resoSlots.filter { it.value.resonator?.level?.level ?: 0 < reso.level.level }.toList()
                            if (!deployable.isEmpty()) {
                                val deployMap = Util.shuffle(deployable).zip(resos).map { it.first.first to it.second }.toMap()
                                val distance = distanceToPortal(actionPortal)
                                actionPortal.deploy(this, deployMap, distance.toInt())
                                SoundUtil.playDeploySound(actionPortal.location, distance.toInt())
                                //return this.copy(action = Action.start(ActionItem.DEPLOY, World.tick))

                            }
                        }
                    }
                }
            }
            action.end()
            return this
        }

        if (!isArrived()) {
            return moveCloserInRange()
        }
        return doDeploy()
    }

    fun doNothing(): Agent {
        action.start(ActionItem.WAIT)
        return this
    }

    fun keySet() = inventory.findUniqueKeys()
    fun hasKeys() = keySet() != null && keySet()!!.isNotEmpty()

    fun isLinkPossible(): Boolean {
        if (!actionPortal.canLinkOut(this)) {
            return false
        }
        if (hasKeys()) {
            val targetOptions: List<Portal>? = actionPortal.findLinkableForKeys(this)?.filter {
                it != actionPortal && it.owner != null && !it.isDeprecated()
            }?.distinct()
            if (targetOptions?.isNotEmpty() == true) {
                val linkOptions: List<Link> = targetOptions.mapNotNull {
                    Link.create(actionPortal, it, this)
                }
                return linkOptions.any { link ->
                    World.allLines().none { it.doesIntersect(link.getLine()) }
                }
            }
        }
        return false
    }

    fun createLink(): Agent {
        if (!actionPortal.canLinkOut(this)) {
            return doNothing()
        }
        if (hasKeys()) {
            val linkOptions: List<Portal>? = actionPortal.findLinkableForKeys(this)?.filter {
                it != actionPortal && it.owner != null && !it.isDeprecated()
            }?.distinct()
            if (linkOptions != null && linkOptions.isNotEmpty()) {
                val linkLinks: List<Link> = linkOptions.map { Link.create(actionPortal, it, this) }.filterNotNull()
                val nonCrossing = linkLinks.filter { link ->
                    World.allLines().none {
                        it.doesIntersect(link.getLine())
                    }
                }
                val hasLinkOptions = nonCrossing.isNotEmpty()
                if (hasLinkOptions) {
                    val randomTarget: Link = Util.shuffle(nonCrossing).first()
                    actionPortal.createLink(this, randomTarget.destination)
                }
            }
        }
        action.start(ActionItem.LINK)
        return this
    }

    fun hackActionPortal(): Agent {
        if (isAtActionPortal() && actionPortal.canHack(this)) {
            val hackResult = actionPortal.tryHack(this)
            SoundUtil.playHackingSound(actionPortal.location)
            val isSuccess = hackResult.items != null
            if (isSuccess) {
                val newStuff: List<QgressItem> = hackResult.items!!
                inventory.items.addAll(newStuff)
            }
        }
        action.start(ActionItem.HACK)
        return this
    }

    private fun findPortalsInAttackRange(level: XmpLevel): List<Portal> {
        val attackDistance = (level.rangeM * 0.5) + Dim.portalRadius
        val enemyPortals = World.allPortals.filter { it.owner?.faction != this.faction }
        return enemyPortals.filter { it.location.distanceTo(this.pos) <= attackDistance }.sortedBy { it.location.distanceTo(this.pos) }
    }

    fun findResosInAttackRange(level: XmpLevel): List<Resonator> {
        val attackDistance = level.rangeM * 0.5
        val portals = findPortalsInAttackRange(level)
        val slots = portals.flatMap { it.resoSlots.map { s -> s.value } }
        val resosInRange = slots.filter { it.resonator != null && it.resonator.coords?.distanceTo(this.pos)!! <= attackDistance }
        return resosInRange.map { it.resonator }.filterNotNull()
    }

    private fun shadowPos() = PathUtil.posToShadowPos(pos)

    fun draw(ctx: Ctx) {
        val image = ActionItem.getIcon(action.item, faction)
        ctx.drawImage(image, pos.xx(), pos.yy())
        val xmBar = getXmBarImage(faction, xmBarPercent())
        ctx.drawImage(xmBar, pos.xx(), pos.yy() - 3)
    }

    fun drawRadius(ctx: Ctx) {
        if (Styles.isDrawDestination) {
            DrawUtil.drawLine(ctx, lineToPortal(actionPortal), Colors.nextPortal, 1.0)
            DrawUtil.drawLine(ctx, lineToDestination(), Colors.destination, 1.0)
        }
        if (Styles.isDrawAgentRange) {
            val deployCircle = Circle(pos, Dim.maxDeploymentRange)
            DrawUtil.drawCircle(ctx, deployCircle, Colors.agentDeployCircle, Dim.agentDeployCircleLineWidth)
        }
    }

    override fun toString() = "L" + getLevel() + " " + faction.abbr + "-" + name
    override fun equals(other: Any?) = other is Agent && this.key() == other.key()
    override fun hashCode() = this.key().hashCode() * 31

    companion object {
        private fun xmCapacity(level: Int): Int = when (level) {
            1 -> 3000
            2 -> 4000
            3 -> 5000
            4 -> 6000
            5 -> 7000
            6 -> 8000
            7 -> 9000
            8 -> 10000
            9 -> 10900
            10 -> 11700
            11 -> 12400
            12 -> 13000
            13 -> 13500
            14 -> 13900
            15 -> 14200
            else -> 14400
        }

        private fun getLevel(actionPoints: Int): Int = when (actionPoints) {
            in 0..10000 -> 1
            in 10000..30000 -> 2
            in 30000..70000 -> 3
            in 70000..150000 -> 4
            in 150000..300000 -> 5
            in 300000..600000 -> 6
            in 600000..1200000 -> 7
            in 1200000..2400000 -> 8
            in 2400000..4000000 -> 9 //+ 1 gold 4 silver
            in 4000000..6000000 -> 10 //+ 2 gold 5 silver
            in 6000000..8400000 -> 11 //+ 4 gold 6 silver
            in 8400000..12000000 -> 12 //+ 6 gold 7 silver
            in 12000000..17000000 -> 13 //+ 1 Platinum 7 Gold
            in 17000000..24000000 -> 14 //+ 2 Platinum 7 Gold
            in 24000000..40000000 -> 15 //+ 3 Platinum 7 Gold
            else -> 16 //TODO + 2 Black 4 Platinum 7 Gold
        }

        private fun getLinkingRange(level: Int): Int = when (level) {
            9 -> 2250
            10 -> 2500
            11 -> 2750
            12 -> 3000
            13 -> 3250
            14 -> 3500
            15 -> 3750
            16 -> 4000
            else -> 2000
        }

        private fun xmKey(faction: Faction, percent: Int) = faction.abbr + ":" + percent
        private val xmBarImages = Faction.values().flatMap { fac ->
            (0..100).map {
                val lw = Dim.agentLineWidth
                val r = Dim.agentRadius
                val w = (r + lw) * 2
                xmKey(fac, it) to DrawUtil.renderBarImage(fac.color, it, 3, w, lw)
            }
        }.toMap()

        private fun getXmBarImage(faction: Faction, percent: Int): Canvas {
            check(percent in 0..100)
            return xmBarImages.getValue(xmKey(faction, percent))
        }

        fun createFrog(grid: Map<Coords, Cell>) = create(grid, Faction.ENL)
        fun createSmurf(grid: Map<Coords, Cell>) = create(grid, Faction.RES)
        private fun create(grid: Map<Coords, Cell>, faction: Faction): Agent {
            val initialAp = 0
            val initialXm = xmCapacity(getLevel(initialAp))
            val coords = Coords.createRandomPassable(grid)
            val actionPortal = Util.findNearestPortal(coords) ?: World.allPortals[0] //FIXME
            return Agent(faction, Util.generateAgentName(), coords, Skills.createRandom(),
                    Inventory(), Action.create(), actionPortal, actionPortal.location, initialAp, initialXm)
        }
    }
}
