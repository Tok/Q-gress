package agent

import World
import config.Config
import config.Dim
import config.Sim
import config.Time
import extension.Grid
import extension.VectorField
import portal.Portal
import util.*
import util.data.Complex
import util.data.Pos
import util.ui.LoadingOverlay

data class NonFaction(
    var pos: Pos,
    val speed: Double,
    val size: AgentSize,
    var destination: Pos,
    var vectors: VectorField,
    var busyUntil: Int,
    val id: Int = nextId(), // stable per-NPC id for the 3D spawn (marble drop) animation
) {
    private val swarmTendency = 0.02
    private val swarmChance = swarmTendency - (swarmTendency * 0.5 * size.offset)

    fun isOnScreen() = pos.isOffGrid()
    private val isDrunk = Util.random() <= 0.02 // TODO

    private var velocity = Complex.ZERO
    private fun distanceToDestination(): Double = pos.distanceTo(destination)
    private fun distanceToPortal(portal: Portal): Double = pos.distanceTo(portal.location)
    private fun isAtDestination(): Boolean = distanceToDestination() < Dim.maxDeploymentRange // Constants.phi
    private fun isBusy(tick: Int): Boolean = tick <= busyUntil

    // ?debug stuck detection feeds only entities actively trying to travel (see StuckTracker).
    fun isStuckCandidate(tick: Int) = !isBusy(tick) && !isAtDestination()

    fun act() {
        if (isBusy(World.tick)) {
            if (Util.random() < 0.001) { // stop waiting and go somewhere random
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
            val force: Complex = if (Config.isNpcSwarming && Util.random() < swarmChance) {
                val nearPos = findNearest().pos
                if (nearPos.distanceTo(pos) < Dim.agentRadius) {
                    val re = -(this.pos.x - nearPos.x)
                    val im = -(this.pos.y - nearPos.y)
                    val acceleration = 1.2
                    Complex(re * acceleration, im * acceleration)
                } else {
                    Complex(pos.x, pos.y)
                }
            } else {
                vectors[pos.toShadow()] ?: MovementUtil.headingTo(pos, destination)
            }
            velocity = MovementUtil.move(velocity, force, speed)
            this.pos = Pos(pos.x + velocity.re, pos.y + velocity.im)
        }
    }

    private fun findNearest() = World.allNonFaction.filterNot { it == this }.minByOrNull {
        it.pos.distanceTo(this.pos)
    } ?: throw IllegalStateException("Unable to find nearest to $pos")

    private fun wait() {
        this.velocity = Complex.ZERO
        this.busyUntil = World.tick + createWaitTime()
    }

    private fun moveElsewhere() = if (!Sim.roundField && !pos.isOffScreen() && Util.random() < 0.96) {
        moveToRandomOffscreenDestination() // off-map roaming — only on rectangular maps (would leave the circle)
    } else if (Util.random() < 0.7) {
        moveToFarPortal()
    } else {
        moveToRandomPortal()
    }

    private fun moveToRandomOffscreenDestination() {
        val destination = Util.shuffle(DESTINATIONS).first()
        this.vectors = getOrCreateVectorField(destination)
        this.destination = destination
    }

    private fun moveToFarPortal() {
        val portal = findFarPortal(pos)
        this.vectors = portal.vectors
        this.destination = portal.location
    }

    private fun moveToRandomPortal() {
        val randomTarget: Portal = World.allPortals[(Util.random() * (World.allPortals.size - 1)).toInt()]
        this.vectors = randomTarget.vectors
        this.destination = randomTarget.location
    }

    companion object {
        private val OFFSCREEN_DISTANCE = Pos.res * (MapUtil.OFFSCREEN_CELL_ROWS / 2)
        private val DESTINATIONS = listOf(
            // NORTH
            Pos(World.simW() / 3, -OFFSCREEN_DISTANCE),
            Pos(World.simW() * 2 / 3, -OFFSCREEN_DISTANCE),
            // WEST
            Pos(-OFFSCREEN_DISTANCE, World.simH() / 3),
            Pos(-OFFSCREEN_DISTANCE, World.simH() * 2 / 3),
            // EAST
            Pos(World.simW() + OFFSCREEN_DISTANCE, World.simH() / 3),
            Pos(World.simW() + OFFSCREEN_DISTANCE, World.simH() * 2 / 3),
            // SOUTH
            Pos(World.simW() / 3, World.simH() + OFFSCREEN_DISTANCE),
            Pos(World.simW() * 2 / 3, World.simH() + OFFSCREEN_DISTANCE),
        )
        private val OFFSCREEN_EDGES = listOf(
            Pos(-OFFSCREEN_DISTANCE, -OFFSCREEN_DISTANCE),
            Pos(World.simW() + OFFSCREEN_DISTANCE, -OFFSCREEN_DISTANCE),
            Pos(-OFFSCREEN_DISTANCE, World.simH() + OFFSCREEN_DISTANCE),
            Pos(World.simW() + OFFSCREEN_DISTANCE, World.simH() + OFFSCREEN_DISTANCE),
        )
        val OFFSCREEN = DESTINATIONS + (if (Config.useOffscreenEdgeDestinations) OFFSCREEN_EDGES else emptyList())
        fun prepareOffscreenLocations() = OFFSCREEN.forEach {
            NonFaction.getOrCreateVectorField(it)
        }

        private val fields = mutableMapOf<Pos, VectorField>()
        private val pending = mutableSetOf<Pos>() // destinations whose field is computing async
        fun offscreenCount(): Int = fields.count()
        fun offscreenTotal(): Int = OFFSCREEN.count()

        // Returns the cached field, or empty-now + an async fill on a miss. Callers snapshot the
        // empty map and fall back to a straight-line heading (see act()) until the field lands; they
        // re-fetch the ready field when they next change destination. [pending] dedupes the launch.
        fun getOrCreateVectorField(destination: Pos): VectorField {
            val maybeField = fields[destination]
            if (maybeField != null && maybeField.isNotEmpty()) {
                return maybeField
            }
            if (pending.add(destination)) {
                PathUtil.computeFieldAsync(destination) { field ->
                    fields[destination] = field
                    pending.remove(destination)
                    LoadingOverlay.detail("Preparing routes…")
                    SoundUtil.playOffScreenLocationCreationSound()
                }
            }
            return emptyMap()
        }

        private fun findFarPortal(pos: Pos) = World.allPortals.sortedByDescending { pos.distanceTo(it.location) }.first()

        private val MIN_WAIT = Time.secondsToTicks(5)
        private val MAX_WAIT = Time.secondsToTicks(45)
        private fun createWaitTime() = Util.randomInt(MIN_WAIT, MAX_WAIT)

        private var seq = 0
        private fun nextId() = seq++

        fun findNearestTo(pos: Pos) = World.allNonFaction.minByOrNull {
            it.pos.distanceTo(pos)
        } ?: throw IllegalStateException("Unable to find nearest to $pos")

        fun create(grid: Grid): NonFaction {
            val position = Pos.createRandomPassable(grid)
            val size = AgentSize.createRandom()
            val speed = Skills.randomNpcSpeed()
            val newNonFaction = if (Util.random() < 0.1) { // move to offscreen destination
                val destination = Util.shuffle(OFFSCREEN).first()
                val vectorField = getOrCreateVectorField(destination)
                NonFaction(position, speed, size, destination, vectorField, World.tick)
            } else { // move to random portal
                val portal = World.allPortals[(Util.random() * (World.allPortals.size - 1)).toInt()]
                NonFaction(position, speed, size, portal.location, portal.vectors, World.tick)
            }
            SoundUtil.playNpcCreationSound(newNonFaction)
            return newNonFaction // rendered in 3D by Scene3D.sync(); no 2D draw (it'd just flash then clear)
        }
    }
}
