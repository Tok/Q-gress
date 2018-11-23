package agent

import Canvas
import Ctx
import World
import config.Colors
import config.Config
import config.Dim
import config.Time
import portal.Portal
import system.display.VectorFields
import system.display.loading.Loading
import util.*
import util.data.Cell
import util.data.Circle
import util.data.Complex
import util.data.Coords

data class NonFaction(var pos: Coords, val speed: Float, val size: AgentSize,
                      var destination: Coords, var vectorField: Map<Coords, Complex>,
                      var busyUntil: Int) {
    private val swarmTendency = 0.02
    private val swarmChance = swarmTendency - (swarmTendency * 0.5 * size.offset)

    fun isOnScreen() = pos.isOffGrid()
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
                val nearPos = findNearest().pos
                if (nearPos.distanceTo(pos) < Dim.agentRadius) {
                    val re = -(this.pos.xx() - nearPos.xx()).toFloat()
                    val im = -(this.pos.yy() - nearPos.yy()).toFloat()
                    val acceleration = 1.2F
                    Complex(re * acceleration, im * acceleration)
                } else {
                    Complex(pos.x, pos.y)
                }
            } else {
                vectorField[PathUtil.posToShadowPos(pos)]
            }
            velocity = MovementUtil.move(velocity, force, speed)
            this.pos = Coords((pos.x + velocity.re).toInt(), (pos.y + velocity.im).toInt())
        }
    }

    private fun findNearest() =
            World.allNonFaction.filterNot { it == this }.minBy {
                it.pos.distanceTo(this.pos)
            } ?: throw IllegalStateException("Unable to find nearest to $pos")

    private fun wait() {
        this.velocity = Complex.ZERO
        this.busyUntil = World.tick + createWaitTime()
    }

    private fun moveElsewhere() {
        return if (!pos.isOffScreen() && Util.random() < 0.96) {
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
        val changeToBeRecruited = 0.05
        private val OFFSCREEN_DISTANCE = PathUtil.res * (MapUtil.OFFSCREEN_CELL_ROWS / 2)
        private val DESTINATIONS = listOf(
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
        private val OFFSCREEN_EDGES = listOf(
                Coords(-OFFSCREEN_DISTANCE, -OFFSCREEN_DISTANCE),
                Coords(World.w() + OFFSCREEN_DISTANCE, -OFFSCREEN_DISTANCE),
                Coords(-OFFSCREEN_DISTANCE, World.h() + OFFSCREEN_DISTANCE),
                Coords(World.w() + OFFSCREEN_DISTANCE, World.h() + OFFSCREEN_DISTANCE)
        )
        val OFFSCREEN = DESTINATIONS + (if (Config.useOffscreenEdgeDestinations) OFFSCREEN_EDGES else emptyList())
        fun prepareOffscreenLocations() = OFFSCREEN.forEach {
            NonFaction.getOrCreateVectorField(it)
        }

        private val fields = mutableMapOf<Coords, Map<Coords, Complex>>()
        fun offscreenCount(): Int = fields.count()
        fun offscreenTotal(): Int = OFFSCREEN.count()
        fun getOrCreateVectorField(destination: Coords): Map<Coords, Complex> {
            val maybeField = fields[destination]
            return if (maybeField != null && maybeField.isNotEmpty()) {
                maybeField
            } else {
                val newField = PathUtil.calculateVectorField(PathUtil.generateHeatMap(destination))
                Loading.draw()
                SoundUtil.playOffScreenLocationCreationSound()
                VectorFields.draw(newField)
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
                val fillStyle = Colors.npcColor
                val strokeStyle = Colors.black
                val circle = Circle(Coords(r + lineWidth, r + lineWidth), r.toDouble())
                DrawUtil.drawCircle(ctx, circle, strokeStyle, lineWidth.toDouble(), fillStyle)
            })
        }

        fun findNearestTo(pos: Coords) =
                World.allNonFaction.minBy {
                    it.pos.distanceTo(pos)
                } ?: throw IllegalStateException("Unable to find nearest to $pos")

        fun create(grid: Map<Coords, Cell>): NonFaction {
            val position = Coords.createRandomPassable(grid)
            val size = AgentSize.createRandom()
            val min = Skills.minSpeed
            val max = Skills.maxSpeed
            val v = min + (Util.random().toFloat() * (max - min)) - size.offset
            val speed = Util.clipFloat(v, min, max)
            val newNonFaction = if (Util.random() < 0.1) { //move to offscreen destination
                val destination = Util.shuffle(OFFSCREEN).first()
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
