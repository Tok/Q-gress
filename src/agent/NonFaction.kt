package agent

import Canvas
import Ctx
import World
import config.Colors
import config.Constants
import config.Dimensions
import config.Time
import portal.Portal
import util.*
import util.data.Cell
import util.data.Circle
import util.data.Complex
import util.data.Coords

data class NonFaction(var pos: Coords, val speed: Float,
                      var destination: Coords, var vectorField: Map<Coords, Complex>,
                      var busyUntil: Int) {
    val isDrunk = Util.random() <= 0.02 //TODO
    var velocity = Complex.ZERO
    fun distanceToDestination(): Double = pos.distanceTo(destination)
    fun distanceToPortal(portal: Portal): Double = pos.distanceTo(portal.location)
    fun isAtDestination(): Boolean = distanceToDestination() < Dimensions.maxDeploymentRange // Constants.phi
    fun isBusy(tick: Int): Boolean = tick <= busyUntil

    fun act() {
        if (isBusy(World.tick)) {
            if (Util.random() < 0.001) { //stop waiting and go somewhere random
                this.busyUntil = World.tick
                moveElsewhere()
            }
            return
        }

        if (Util.random() < 0.005) {
            wait()
        }

        if (Util.random() < 0.02) {
            moveElsewhere()
        }

        if (isAtDestination()) {
            wait()
        } else {
            val shadowPos = PathUtil.posToShadowPos(pos)
            val force = vectorField.get(shadowPos) ?: velocity
            val mag = speed * Time.globalSpeedFactor * World.speed / 100
            val relativeForce = Complex.fromMagnitudeAndPhase(mag, force.phase)
            val oldWeight = Constants.historyFactor * 100 / World.speed
            val oldVector = Complex.valueOf(this.velocity.magnitude * oldWeight, this.velocity.phase)
            val newVector = Complex.valueOf(relativeForce.magnitude * (1F - oldWeight), relativeForce.phase)
            val velo = oldVector + newVector
            this.velocity = velo
            this.pos = Coords((pos.x + this.velocity.re).toInt(), (pos.y + this.velocity.im).toInt())
        }
    }

    fun isOffScreen() = pos.x < 0 || pos.y < 0 || pos.x >= World.w() || pos.y >= World.h()

    fun wait() {
        this.velocity = Complex.ZERO
        this.busyUntil = World.tick + creatWaitTime()
    }

    fun moveElsewhere() {
        if (!isOffScreen() && Util.random() < 0.8) {
            return moveToRandomOffscreenDestination()
        }
        if (Util.random() < 0.5) {
            return moveToFarPortal()
        }
        return moveToRandomPortal()
    }

    fun moveToRandomOffscreenDestination() {
        val destination = Util.shuffle(DESTINATIONS).first()
        this.vectorField = getOrCreateVectorField(destination)
        this.destination = destination
    }

    fun moveToFarPortal() {
        val portal = findFarPortal(pos)
        this.vectorField = portal.vectorField
        this.destination = portal.location
    }

    fun moveToRandomPortal() {
        val randomTarget: Portal = World.allPortals[(Util.random() * (World.allPortals.size - 1)).toInt()]
        this.vectorField = randomTarget.vectorField
        this.destination = randomTarget.location
    }

    fun draw(ctx: Ctx) = ctx.drawImage(NonFaction.image, pos.xx(), pos.yy())

    companion object {
        private val OFFSCREEN_DISTANCE = PathUtil.RESOLUTION * (MapUtil.OFFSCREEN_CELL_ROWS / 2)
        val useOffscreenEdgeDestinations = false //TODO reactivate?
        val OFFSCREEN_EDGES = listOf(
                Coords(-OFFSCREEN_DISTANCE, -OFFSCREEN_DISTANCE),
                Coords(World.w() + OFFSCREEN_DISTANCE, -OFFSCREEN_DISTANCE),
                Coords(-OFFSCREEN_DISTANCE, World.h() + OFFSCREEN_DISTANCE),
                Coords(World.w() + OFFSCREEN_DISTANCE, World.h() + OFFSCREEN_DISTANCE)
        )
        val DESTINATIONS = listOf(
                //NORTH
                Coords(World.w() / 3, -OFFSCREEN_DISTANCE),
                Coords(World.w() * 2 / 3, -OFFSCREEN_DISTANCE),
                //WEST
                Coords(-OFFSCREEN_DISTANCE, World.h() / 3),
                Coords(-OFFSCREEN_DISTANCE, World.h() * 2 / 3),
                //EAST
                Coords(World.w() + OFFSCREEN_DISTANCE, World.h() / 3),
                Coords(World.w() + OFFSCREEN_DISTANCE, World.h() * 2 / 3),
                //SOUTH
                Coords(World.w() / 3, World.h() + OFFSCREEN_DISTANCE),
                Coords(World.w() * 2 / 3, World.h() + OFFSCREEN_DISTANCE)
        )

        val fields = mutableMapOf<Coords, Map<Coords, Complex>>()
        fun getOrCreateVectorField(destination: Coords): Map<Coords, Complex> {
            val maybeField = fields.get(destination)
            if (maybeField != null && maybeField.isNotEmpty()) {
                return maybeField
            } else {
                val newField = PathUtil.calculateVectorField(PathUtil.generateHeatMap(destination))
                fields.put(destination, newField)
                return newField
            }
        }

        private fun findFarPortal(pos: Coords): Portal {
            val randomFarPortals = World.allPortals.sortedByDescending { pos.distanceTo(it.location) }
            randomFarPortals.forEach { portal ->
                if (Util.random() < 0.4) {
                    return portal
                }
            }
            return randomFarPortals.elementAt(0)
        }

        private val MIN_WAIT = Util.secondsToTicks(5)
        private val MAX_WAIT = Util.secondsToTicks(45)
        private fun creatWaitTime() = Util.randomInt(MIN_WAIT, MAX_WAIT)
        private val image: Canvas = drawTemplate()
        private fun drawTemplate(): Canvas {
            val lineWidth = 2
            val r = Dimensions.agentRadius.toInt()
            val w = r * 2 + (2 * lineWidth)
            val h = w
            return HtmlUtil.prerender(w, h, fun(ctx: Ctx) {
                val fillStyle = "#ffffff"
                val strokeStyle = Colors.black
                val circle = Circle(Coords(r + lineWidth, r + lineWidth), r.toDouble())
                DrawUtil.drawCircle(ctx, circle, strokeStyle, lineWidth.toDouble(), fillStyle)
            })
        }

        val maxSpeed = 5F
        val minSpeed = 3F
        fun create(grid: Map<Coords, Cell>): NonFaction {
            val position = Coords.createRandomPassable(grid)
            val speed = minSpeed + (Util.random().toFloat() * (maxSpeed - minSpeed))
            val newNonFaction = if (Util.random() < 0.1) { //move to offscreen destination
                val destination = Util.shuffle(DESTINATIONS).first()
                val vectorField = getOrCreateVectorField(destination)
                NonFaction(position, speed, destination, vectorField, World.tick)
            } else { //move to random portal
                val portal = World.allPortals[(Util.random() * (World.allPortals.size - 1)).toInt()]
                NonFaction(position, speed, portal.location, portal.vectorField, World.tick)
            }
            DrawUtil.drawNonFaction(newNonFaction)
            return newNonFaction
        }
    }
}
