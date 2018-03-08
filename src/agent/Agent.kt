package agent

import Canvas
import Ctx
import World
import agent.action.Action
import agent.action.ActionItem
import config.*
import items.QgressItem
import items.deployable.Resonator
import items.level.ResonatorLevel
import items.level.XmpLevel
import org.w3c.dom.HTMLInputElement
import portal.Link
import portal.Portal
import system.Queues
import util.*
import util.data.*
import kotlin.browser.window
import kotlin.math.max
import kotlin.math.min

data class Agent(val faction: Faction, val name: String, val pos: Coords, val skills: Skills,
                 val inventory: Inventory, val action: Action, var actionPortal: Portal, var destination: Coords,
                 var ap: Int = 0, var xm: Int = 0, var velocity: Complex = Complex.ZERO) {
    fun key() = toString()
    fun distanceToDestination(): Double = pos.distanceTo(destination)
    fun distanceToPortal(portal: Portal): Double = pos.distanceTo(portal.location)
    fun isAtActionPortal(): Boolean = distanceToPortal(actionPortal) < Dimensions.maxDeploymentRange
    fun isInAttackRange(range: Int): Boolean {
        val strongest = actionPortal.findStrongestResoPos()
        return strongest != null && pos.distanceTo(strongest) < range
    }

    fun isBusy(tick: Int): Boolean = tick <= action.untilTick
    fun lineToPortal(portal: Portal) = Line(pos, portal.location)
    fun lineToDestination() = Line(pos, destination)

    fun getLevel(): Int = getLevel(this.ap)
    fun getXmCapacity(): Int = getXmCapacity(getLevel())

    private fun calcAbsXmBar() = min(getXmCapacity(), max(0, xm))
    fun xmBarPercent() = calcAbsXmBar() * 100 / getXmCapacity()
    fun isXmBarEmpty() = xmBarPercent() == 0
    fun isXmFilled() = xmBarPercent() >= 80
    fun removeXm(v: Int) {
        if (xm - v <= 0) {
            this.xm = 0
        } else {
            this.xm -= v
        }
    }

    fun addXm(v: Int) {
        if (xm + v >= getXmCapacity()) {
            this.xm = getXmCapacity()
        } else {
            this.xm += v
        }
    }

    fun addAp(v: Int) {
        this.ap += v
    }

    fun act(): Agent {
        //println("DEBUG: ${World.tick} $action")
        val useLocationFix = false
        if (isBusy(World.tick)) {
            if (useLocationFix && Util.random() < 0.005) {
                return doSomethingElse()
            }
            return this
        }
        return when (action.item) {
            ActionItem.RECHARGE -> rechargePortal()
            ActionItem.RECYCLE -> recycleItems()
            ActionItem.MOVE -> moveCloserToDestinationPortal()
            ActionItem.ATTACK -> attackPortal()
            ActionItem.DEPLOY -> deployPortal()
            else -> doSomething()
        }
    }

    fun doSomething(): Agent {
        if (!isAtActionPortal()) {
            return doSomethingElse()
        }
        return when (actionPortal.owner?.faction) {
            null -> doNeutralPortalAction()
            this.faction -> doFriendlyPortalAction()
            else -> doEnemyPortalAction()
        }
    }

    private fun isHackPossible() = actionPortal.canHack(this)
    private fun isDeploymentPossible() = !actionPortal.isEnemyOf(this)
            && actionPortal.findAllowedResoLevels(this).map { it.value }.sum() > 0

    private fun q(value: QValue): Double {
        val id = value.name + "Slider" + faction.nickName
        val slider = window.document.getElementById(id) as HTMLInputElement
        return slider.valueAsNumber * value.weight
    }

    private fun basicQvalues(): List<Pair<Double, () -> Agent>> {
        val captureQ = if (MovementUtil.hasUncapturedPortals()) q(QValue.CAPTURE) else -1.0
        val hackQ = if (isHackPossible()) q(QValue.HACK) else -1.0
        val recycleQ = if (isDeploymentPossible()) q(QValue.RECYCLE) else -1.0
        val rechargeQ = if (isXmFilled()) q(QValue.RECHARGE) else -1.0
        val attackSomewhereQ = if (isAttackPossible()) q(QValue.ATTACK_SOMEHERE) else -1.0
        val moveElsewhereQ = q(QValue.MOVE_ELSEWHERE)
        return listOf(
                captureQ to { MovementUtil.moveToUncapturedPortal(this) },
                hackQ to { hackActionPortal() },
                recycleQ to { recycleItems() },
                rechargeQ to { rechargePortal() },
                attackSomewhereQ to { attackSomewhere() },
                moveElsewhereQ to { moveElsewhere() }
        )
    }

    private fun neutralQvalues(): List<Pair<Double, () -> Agent>> {
        val basicValues = basicQvalues()
        val deployQ = if (isDeploymentPossible()) q(QValue.DEPLOY) else -1.0
        return basicValues + listOf(deployQ to { deployPortal() })
    }

    private fun friendlyQvalues(): List<Pair<Double, () -> Agent>> {
        val basicValues = basicQvalues()
        val deployQ = if (isDeploymentPossible()) q(QValue.DEPLOY) else -1.0
        val linkQ = if (isLinkPossible()) q(QValue.LINK) else -1.0
        return basicValues + listOf(
                deployQ to { deployPortal() },
                linkQ to { createLink() }
        )
    }

    private fun enemyQvalues(): List<Pair<Double, () -> Agent>> {
        val basicValues = basicQvalues()
        val attackQ = if (isLinkPossible()) q(QValue.ATTACK) else -1.0
        return basicValues + listOf(attackQ to { attackPortal() })
    }

    val defaultAction = { moveElsewhere() }
    fun doSomethingElse(): Agent = Util.select(basicQvalues(), defaultAction).invoke()
    fun doNeutralPortalAction(): Agent = Util.select(neutralQvalues(), defaultAction).invoke()
    fun doFriendlyPortalAction(): Agent = Util.select(friendlyQvalues(), defaultAction).invoke()
    fun doEnemyPortalAction(): Agent = Util.select(enemyQvalues(), defaultAction).invoke()

    private fun moveElsewhere(): Agent {
        val moveToNearQ = q(QValue.MOVE_TO_NEAR)
        val moveToFriendlyQ = if (MovementUtil.hasFriendlyPortals(this)) q(QValue.MOVE_TO_FRIENDLY) else -1.0
        val moveToRandomQ = q(QValue.MOVE_TO_RANDOM)
        val qValues = listOf(
            moveToNearQ to { MovementUtil.moveToNearestPortal(this) },
            moveToFriendlyQ to { MovementUtil.moveToFriendlyHighLevelPortal(this) },
            moveToRandomQ to { MovementUtil.moveToRandomPortal(this) }
        )
        val newAgent = Util.select(qValues, { MovementUtil.moveToNearestPortal(this) }).invoke()
        newAgent.action.start(ActionItem.MOVE)
        return newAgent
    }

    private fun attackSomewhere(): Agent {
        val attackClosestQ = if (MovementUtil.hasEnemyPortals(this) && isAttackPossible()) q(QValue.ATTACK_CLOSE) else -1.0
        val attackMostLinkedQ = if (MovementUtil.hasEnemyPortals(this) && isAttackPossible()) q(QValue.ATTACK_LINKS) else -1.0
        val attackMostVulnerableQ = if (MovementUtil.hasEnemyPortals(this) && isAttackPossible()) q(QValue.ATTACK_WEAK) else -1.0
        val actions = listOf(
                attackClosestQ to { MovementUtil.attackClosePortal(this) },
                attackMostLinkedQ to { MovementUtil.attackMostLinkedPortal(this) },
                attackMostVulnerableQ to { MovementUtil.attackMostVulnerablePortal(this) }
        )
        val newAgent = Util.select(actions, { MovementUtil.attackClosePortal(this) }).invoke()
        newAgent.action.start(ActionItem.ATTACK)
        return newAgent
    }

    private fun moveCloserToDestinationPortal(): Agent {
        if (!World.isReady) {
            println("WARN: moveCloserToDestination: World is not ready.")
            return doNothing()
        }
        if (isAtActionPortal()) {
            return if (action.item.equals(ActionItem.ATTACK)) this else doNothing()
        }
        addXm(2) //FIXME

        val force = actionPortal.vectorField.get(PathUtil.posToShadowPos(pos))
        velocity = MovementUtil.move(velocity, force, skills.speed)
        return this.copy(pos = Coords((pos.x + velocity.re).toInt(), (pos.y + velocity.im).toInt()))
    }

    private fun isAttackPossible() = inventory.findXmps().isNotEmpty()

    private fun isArrived() = distanceToDestination() <= skills.inRangeSpeed()
    fun moveCloserInRange(): Agent {
        val part = skills.inRangeSpeed() / distanceToDestination()
        val rawDiffX = (pos.xDiff(destination) * part).toInt()
        val rawDiffY = (pos.yDiff(destination) * part).toInt()
        val rawNextX = pos.x - rawDiffX
        val rawNextY = pos.y - rawDiffY
        return this.copy(pos = Coords(rawNextX, rawNextY))
    }

    fun rechargePortal(): Agent {
        if (!hasKeys()) {
            return this
        }
        val chargable = Portal.findChargeableForKeys(this)
        if (chargable!!.isEmpty()) {
            return this
        }
        val lowest: Portal? = chargable.sortedBy { it.calcHealth() }.first()
        if (lowest != null) {
            val resos = lowest.resoSlots.mapNotNull { it.value.resonator }
            val resoCount = resos.count()
            resos.forEach { it.recharge(this, 1000 / resoCount) }
        }
        return this
    }

    fun recycleItems(): Agent {
        return this //TODO implement..
    }

    fun attackPortal(): Agent {
        fun findExactDestination(): Coords {
            if (actionPortal.calcHealth() > 0.5) {
                return actionPortal.location
            }
            val maybeDestination = actionPortal.findStrongestResoPos()
            val isPassable = maybeDestination != null && maybeDestination.isPassable()
            return if (isPassable) { maybeDestination!! } else { actionPortal.location }
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

        if (action.item != ActionItem.ATTACK) {
            action.start(ActionItem.ATTACK)
            destination = findExactDestination()
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
            val distance = skills.deployPrecision * Dimensions.maxDeploymentRange
            return actionPortal.findRandomPointNearPortal(distance.toInt())
        }

        fun doDeploy(): Agent {
            if (actionPortal.isEnemyOf(this)) {
                return doSomethingElse()
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
            val linkOptions: List<Portal>? = actionPortal.findLinkableForKeys(this)?.filter {
                it != actionPortal && it.owner != null && !it.isDeprecated()
            }?.distinct()
            if (linkOptions != null && linkOptions.isNotEmpty()) {
                val linkLinks: List<Link> = linkOptions.map { Link.create(actionPortal, it, this) }.filterNotNull()
                val nonCrossing = linkLinks.filter { link ->
                    World.allLines().filter {
                        it.doesIntersect(link.getLine())
                    }.isEmpty()
                }
                val hasLinkOptions = nonCrossing.isNotEmpty()
                return hasLinkOptions
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
                    World.allLines().filter {
                        it.doesIntersect(link.getLine())
                    }.isEmpty()
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
        val attackDistance = (level.rangeM * 0.5) + Dimensions.portalRadius
        val enemyPortals = World.allPortals.filter { it.owner?.faction != this.faction }
        return enemyPortals.filter { it.location.distanceTo(this.pos) <= attackDistance }.sortedBy { it.location.distanceTo(this.pos) }
    }

    fun findResosInAttackRange(level: XmpLevel): List<Resonator> {
        val attackDistance = level.rangeM * 0.5
        val portals = findPortalsInAttackRange(level)
        val slots = portals.flatMap { it.resoSlots.map { it.value } }
        val resosInRange = slots.filter { it.resonator != null && it.resonator.coords?.distanceTo(this.pos)!! <= attackDistance }
        return resosInRange.map { it.resonator }.filterNotNull()
    }

    private fun shadowPos() = PathUtil.posToShadowPos(pos)

    fun draw(ctx: Ctx) {
        val image = getAgentImage(faction, action.item)
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
            val deployCircle = Circle(pos, Dimensions.maxDeploymentRange)
            DrawUtil.drawCircle(ctx, deployCircle, Colors.agentDeployCircle, Dimensions.agentDeployCircleLineWidth)
        }
    }

    override fun toString() = "L" + getLevel() + " " + faction.abbr + "-" + name
    override fun equals(other: Any?) = other is Agent && this.key() == other.key()
    override fun hashCode() = this.key().hashCode() * 31

    companion object {
        private fun getXmCapacity(level: Int): Int = when (level) {
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

        private val enlImages = ActionItem.values().map { it to drawAgentTemplate(Faction.ENL, it) }.toMap()
        private val resImages = ActionItem.values().map { it to drawAgentTemplate(Faction.RES, it) }.toMap()
        private fun xmKey(faction: Faction, percent: Int) = faction.abbr + ":" + percent
        private val xmBarImages = Faction.values().flatMap { fac ->
            (0..100).map {
                val lw = Dimensions.agentLineWidth
                val r = Dimensions.agentRadius.toInt()
                val w = (r * 2) + (2 * lw)
                xmKey(fac, it) to DrawUtil.renderBarImage(fac.color, it, 3, w, lw)
            }
        }.toMap()

        private fun getAgentImage(faction: Faction, actionItem: ActionItem): Canvas {
            return when (faction) {
                Faction.ENL -> enlImages.getValue(actionItem)
                Faction.RES -> resImages.getValue(actionItem)
                else -> throw IllegalStateException("Illegal faction: $faction")
            }
        }

        private fun getXmBarImage(faction: Faction, percent: Int): Canvas {
            check(percent >= 0 && percent <= 100)
            return xmBarImages.getValue(xmKey(faction, percent))
        }

        private fun drawAgentTemplate(faction: Faction, actionItem: ActionItem): Canvas {
            val lw = Dimensions.agentLineWidth
            val r = Dimensions.agentRadius.toInt()
            val w = (r * 2) + (2 * lw)
            val h = w
            return HtmlUtil.prerender(w, h, fun(ctx: Ctx) {
                val pos = Coords(r + lw, r + lw)
                val strokeStyle = Colors.black
                val circle = Circle(pos, r.toDouble())
                DrawUtil.drawCircle(ctx, circle, strokeStyle, lw * 2.0, faction.color)
                DrawUtil.drawText(ctx, pos.copy(x = pos.x + 1), actionItem.letter, strokeStyle, 13, DrawUtil.CODA)
            })
        }

        fun createFrog(grid: Map<Coords, Cell>) = create(grid, Faction.ENL)
        fun createSmurf(grid: Map<Coords, Cell>) = create(grid, Faction.RES)
        private fun create(grid: Map<Coords, Cell>, faction: Faction): Agent {
            val initialAp = Util.randomInt(1000000)
            val initialXm = getXmCapacity(getLevel(initialAp))
            val coords = Coords.createRandomPassable(grid)
            val actionPortal = Util.findNearestPortal(coords) ?: World.allPortals.get(0) //FIXME
            return Agent(faction, Util.generateAgentName(), coords, Skills.createRandom(),
                    Inventory(), Action.create(), actionPortal, actionPortal.location, initialAp, initialXm)
        }
    }
}
