package agent

import Canvas
import Ctx
import World
import config.Colors
import config.Config
import config.Dim
import config.Time
import portal.Portal
import util.*
import util.data.Cell
import util.data.Circle
import util.data.Complex
import util.data.Coords

data class NonFaction(var pos: Coords, val speed: Float, val size: AgentSize,
                      var destination: Coords, var vectorField: Map<Coords, Complex>,
                      var busyUntil: Int) {
    private val swarmTendency = 0.1
    private val swarmChance = swarmTendency - (swarmTendency * 0.5 * size.offset)

    private val isDrunk = Util.random() <= 0.02 //TODO

    private var velocity = Complex.ZERO
    private fun distanceToDestination(): Double = pos.distanceTo(destination)
    private fun distanceToPortal(portal: Portal): Double = pos.distanceTo(portal.location)
    private fun isAtDestination(): Boolean = distanceToDestination() < Dim.maxDeploymentRange // Constants.phi
    private fun isBusy(tick: Int): Boolean = tick <= busyUntil
    fun act() {
        if (isBusy(World.tick)) {
            if (Util.random() < 0.001) { //stop waiting and go somewhere random
                this.busyUntil = World.tick
                moveElsewhere()
            }
            return
        }

        if (Util.random() < 0.007) {
            wait()
        }

        if (Util.random() < 0.015) {
            moveElsewhere()
        }

        if (isAtDestination()) {
            wait()
        } else {
            val force = if (Config.isNpcSwarming && Util.random() < swarmChance) {
                val nearestNpc: NonFaction = World.allNonFaction.filterNot { it == this }.minBy {
                    it.pos.distanceTo(this.pos)
                } ?: this
                val nearPos = nearestNpc.pos
                val re = -(this.pos.xx() - nearPos.xx()).toFloat()
                val im = -(this.pos.yy() - nearPos.yy()).toFloat()
                val acceleration = 5.0F
                Complex(re * acceleration, im * acceleration)
            } else {
                vectorField[PathUtil.posToShadowPos(pos)]
            }
            velocity = MovementUtil.move(velocity, force, speed)
            this.pos = Coords((pos.x + velocity.re).toInt(), (pos.y + velocity.im).toInt())
        }
    }

    private fun isOffScreen() = pos.x < 0 || pos.y < 0 || pos.x >= World.w() || pos.y >= World.h()

    private fun wait() {
        this.velocity = Complex.ZERO
        this.busyUntil = World.tick + createWaitTime()
    }

    private fun moveElsewhere() {
        return if (!isOffScreen() && Util.random() < 0.96) {
            moveToRandomOffscreenDestination()
        } else if (Util.random() < 0.7) {
            moveToFarPortal()
        } else {
            moveToRandomPortal()
        }
    }

    private fun moveToRandomOffscreenDestination() {
        val destination = Util.shuffle(DESTINATIONS).first()
        this.vectorField = getOrCreateVectorField(destination)
        this.destination = destination
    }

    private fun moveToFarPortal() {
        val portal = findFarPortal(pos)
        this.vectorField = portal.vectorField
        this.destination = portal.location
    }

    private fun moveToRandomPortal() {
        val randomTarget: Portal = World.allPortals[(Util.random() * (World.allPortals.size - 1)).toInt()]
        this.vectorField = randomTarget.vectorField
        this.destination = randomTarget.location
    }

    fun draw(ctx: Ctx) = ctx.drawImage(NonFaction.image(size), pos.xx(), pos.yy())

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

        private val fields = mutableMapOf<Coords, Map<Coords, Complex>>()
        fun getOrCreateVectorField(destination: Coords): Map<Coords, Complex> {
            val maybeField = fields[destination]
            return if (maybeField != null && maybeField.isNotEmpty()) {
                maybeField
            } else {
                val newField = PathUtil.calculateVectorField(PathUtil.generateHeatMap(destination))
                fields[destination] = newField
                newField
            }
        }

        private fun findFarPortal(pos: Coords) = World.allPortals.sortedByDescending { pos.distanceTo(it.location) }.first()

        private val images = mapOf(-1 to drawTemplate(-1), 0 to drawTemplate(0), 1 to drawTemplate(1))
        private val MIN_WAIT = Time.secondsToTicks(5)
        private val MAX_WAIT = Time.secondsToTicks(45)
        private fun createWaitTime() = Util.randomInt(MIN_WAIT, MAX_WAIT)
        private fun image(size: AgentSize): Canvas = images.get(size.offset) ?: drawTemplate(0)
        private fun drawTemplate(sizeOffset: Int): Canvas {
            val lineWidth = 2
            val r = Dim.agentRadius.toInt() + sizeOffset
            val w = r * 2 + (2 * lineWidth)
            val h = w
            return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
                val fillStyle = "#ffffff"
                val strokeStyle = Colors.black
                val circle = Circle(Coords(r + lineWidth, r + lineWidth), r.toDouble())
                DrawUtil.drawCircle(ctx, circle, strokeStyle, lineWidth.toDouble(), fillStyle)
            })
        }

        private const val maxSpeed = 2.5F
        private const val minSpeed = 1.5F

        fun create(grid: Map<Coords, Cell>): NonFaction {
            val position = Coords.createRandomPassable(grid)
            val size = AgentSize.createRandom()
            val min = minSpeed - size.offset
            val max = maxSpeed - size.offset
            val speed = min + (Util.random().toFloat() * (max - min))
            val newNonFaction = if (Util.random() < 0.1) { //move to offscreen destination
                val destination = Util.shuffle(DESTINATIONS).first()
                val vectorField = getOrCreateVectorField(destination)
                NonFaction(position, speed, size, destination, vectorField, World.tick)
            } else { //move to random portal
                val portal = World.allPortals[(Util.random() * (World.allPortals.size - 1)).toInt()]
                NonFaction(position, speed, size, portal.location, portal.vectorField, World.tick)
            }
            SoundUtil.playNpcCreationSound(newNonFaction)
            DrawUtil.drawNonFaction(newNonFaction)
            return newNonFaction
        }
    }
}
