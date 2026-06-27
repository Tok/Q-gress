package agent
import World
import config.Config
import config.Dim
import config.Sim
import config.Time
import extension.Grid
import extension.VectorField
import portal.Portal
import system.audio.Sound
import system.grid.Pathfinding
import system.map.MapController
import system.ui.LoadingOverlay
import util.*
import util.data.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class NonFaction(
    var pos: Pos,
    val speed: Double,
    val size: AgentSize,
    var destination: Pos,
    var vectors: VectorField,
    var busyUntil: Int,
    val id: Int = nextId(), // stable per-NPC id for the 3D spawn (marble drop) animation
) {
    // Stable identity from the immutable [id]. A data class would otherwise hash over the mutable [pos]
    // etc., so an NPC's hashCode would change as it moves — corrupting any Set it's in (World.allNonFaction)
    // and throwing "object hashCodes changed". (Same fix as Agent/Portal.)
    override fun equals(other: Any?) = other is NonFaction && id == other.id
    override fun hashCode() = id

    private val swarmTendency = 0.02
    private val swarmChance = swarmTendency - (swarmTendency * 0.5 * size.offset)

    fun isOnScreen() = pos.isOffGrid()
    private val isDrunk = Util.random() <= 0.02 // TODO

    private var velocity = Complex.ZERO
    private var beelineTicks = 0 // >0 = un-stick override: head straight to the destination, ignoring the looping field
    private var triedBeeline = false // a bee-line was already spent this stuck episode → escalate to a new destination
    private fun distanceToDestination(): Double = pos.distanceTo(destination)
    private fun distanceToPortal(portal: Portal): Double = pos.distanceTo(portal.location)
    private fun isAtDestination(): Boolean = distanceToDestination() < Dim.maxDeploymentRange // Constants.phi
    private fun isBusy(tick: Int): Boolean = tick <= busyUntil

    // ?debug stuck detection feeds only entities actively trying to travel (see StuckTracker).
    fun isStuckCandidate(tick: Int) = !isBusy(tick) && !isAtDestination()

    /** Rest in place until at least [untilTick] — used while a recruiter walks up to + meets this NPC. */
    fun holdInPlace(untilTick: Int) {
        busyUntil = maxOf(busyUntil, untilTick)
    }

    fun act() {
        if (isBusy(World.tick)) return // resting at a spot — stay put until the rest timer expires

        // NPCs change destination ONLY on arrival, never mid-journey: re-deciding while crossing (it used
        // to ~1.5%/tick) sent them to the new "far side", which flips at the centre — so they kept drifting
        // back toward the middle. Now they commit to a destination, walk clear across the map, rest, then
        // strike out for the next one. (Stuck recovery is the one exception, so a wedged NPC isn't frozen.)
        if (isAtDestination()) {
            moveElsewhere() // pick the next destination…
            wait() // …then rest here a beat before setting off
            return
        }

        maybeRecoverFromStuck()
        val force: Complex = if (beelineTicks > 0) {
            beelineTicks--
            Movement.headingTo(pos, destination) // un-stick override: straight line through the spiral
        } else if (Config.isNpcSwarming && Util.random() < swarmChance) {
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
            vectors[pos.toShadow()] ?: Movement.headingTo(pos, destination)
        }
        velocity = Movement.move(velocity, force, speed)
        this.pos = Pos(pos.x + velocity.re, pos.y + velocity.im)
    }

    /**
     * Un-stick a wandering NPC flagged by [StuckTracker]. Escalates like the agents: first spend a
     * bee-line straight at the destination (ignoring the looping field); if that doesn't free it,
     * re-roll a new destination via [moveElsewhere]. A no-op while a bee-line is already running or
     * when not flagged.
     */
    private fun maybeRecoverFromStuck() {
        if (beelineTicks > 0) return
        if (!StuckTracker.isStuck("npc:$id")) {
            triedBeeline = false
            return
        }
        if (triedBeeline) {
            moveElsewhere()
            triedBeeline = false
        } else {
            beelineTicks = StuckTracker.RECOVERY_BEELINE_TICKS
            triedBeeline = true
        }
    }

    private fun findNearest() = World.allNonFaction.filterNot { it == this }.minByOrNull {
        it.pos.distanceTo(this.pos)
    } ?: throw IllegalStateException("Unable to find nearest to $pos")

    private fun wait() {
        this.velocity = Complex.ZERO
        this.busyUntil = World.tick + createWaitTime()
        this.beelineTicks = 0 // drop any in-flight un-stick override; we've stopped to wait
        this.triedBeeline = false
    }

    private fun moveElsewhere() = if (Util.random() < OFFSCREEN_DEST_CHANCE) {
        // Mostly head for the FAR side of the map (even when already off-screen) so NPCs walk clear across
        // it, edge to edge, instead of clumping around the central portals.
        moveToOpposingOffscreenDestination()
    } else if (Util.random() < 0.7) {
        moveToFarPortal() // ...but still send some to portals so there's life around them too
    } else {
        moveToRandomPortal()
    }

    private fun moveToOpposingOffscreenDestination() {
        val destination = opposingOffscreenDestination(pos)
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
        private val OFFSCREEN_DISTANCE = Pos.res * (MapController.OFFSCREEN_CELL_ROWS / 2)

        // Roughly the gap between adjacent off-map destinations along the border (sim units ≈ half a screen).
        // The count then scales with the field perimeter, bounded so we don't compute too many full-map flow
        // fields (each destination needs one).
        private val OFFSCREEN_SPACING = minOf(Dim.width, Dim.height) * 0.5
        private const val MIN_OFFSCREEN = 8
        private const val MAX_OFFSCREEN = 14
        private const val OFFSCREEN_DEST_CHANCE = 0.85 // mostly cross the map edge-to-edge; the rest head to portals

        /**
         * Hidden destinations placed JUST OUTSIDE the play field, spaced evenly around its border, that NPCs
         * walk toward so they stream across the whole map instead of clumping at the central portals.
         * Computed from the CURRENT play-area size + shape on every call — NOT captured once at class-load,
         * when [Sim] still held its default size (that staleness put targets *inside* a larger map → the
         * centre-clustering bug). A point beyond the bounding box is outside any inscribed shape, so this
         * works for the round field (a ring of points by angle) and the rectangle (points along each border)
         * alike; irregular shapes can extend this later.
         */
        fun offscreenDestinations(): List<Pos> {
            val w = World.simW()
            val h = World.simH()
            return if (Sim.roundField) {
                val cx = w / 2.0
                val cy = h / 2.0
                val r = Sim.fieldRadius() + OFFSCREEN_DISTANCE
                val n = (2.0 * PI * r / OFFSCREEN_SPACING).toInt().coerceIn(MIN_OFFSCREEN, MAX_OFFSCREEN)
                (0 until n).map { i ->
                    val a = 2.0 * PI * i / n
                    Pos((cx + r * cos(a)).toInt(), (cy + r * sin(a)).toInt())
                }
            } else {
                rectBorderDestinations(w, h)
            }
        }

        // Points spaced evenly along the rectangle border, one [OFFSCREEN_DISTANCE] past each edge.
        private fun rectBorderDestinations(w: Int, h: Int): List<Pos> {
            val perEdgeX = (w / OFFSCREEN_SPACING).toInt().coerceIn(2, MAX_OFFSCREEN / 2)
            val perEdgeY = (h / OFFSCREEN_SPACING).toInt().coerceIn(2, MAX_OFFSCREEN / 2)
            val dest = mutableListOf<Pos>()
            for (i in 1..perEdgeX) {
                val x = w * i / (perEdgeX + 1)
                dest.add(Pos(x, -OFFSCREEN_DISTANCE)) // top
                dest.add(Pos(x, h + OFFSCREEN_DISTANCE)) // bottom
            }
            for (j in 1..perEdgeY) {
                val y = h * j / (perEdgeY + 1)
                dest.add(Pos(-OFFSCREEN_DISTANCE, y)) // left
                dest.add(Pos(w + OFFSCREEN_DISTANCE, y)) // right
            }
            return dest
        }

        fun prepareOffscreenLocations() = offscreenDestinations().forEach { getOrCreateVectorField(it) }

        // An off-map destination on the FAR side of the field from [from], so the NPC walks clear across the
        // map rather than to the nearest edge. We pick by DIRECTION (the half whose bearing from centre most
        // opposes the NPC's), not raw distance: on a round field every edge point is equidistant from the
        // centre, so a distance sort ties and tie-breaks to one compass direction (NPCs piling up north).
        // Near the centre there's no meaningful "opposite", so pick uniformly. Random within the chosen set.
        fun opposingOffscreenDestination(from: Pos): Pos {
            val cx = World.simW() / 2.0
            val cy = World.simH() / 2.0
            val nearCentre = minOf(World.simW(), World.simH()) * NEAR_CENTRE_FRAC
            return Util.shuffle(NonFactionMath.opposingHalf(offscreenDestinations(), from, cx, cy, nearCentre)).first()
        }

        private const val NEAR_CENTRE_FRAC = 0.15 // within this fraction of the field, "opposite" is meaningless

        private val fields = mutableMapOf<Pos, VectorField>()
        private val pending = mutableSetOf<Pos>() // destinations whose field is computing async
        fun offscreenCount(): Int = fields.count()
        fun offscreenTotal(): Int = offscreenDestinations().count()

        // Returns the cached field, or empty-now + an async fill on a miss. Callers snapshot the
        // empty map and fall back to a straight-line heading (see act()) until the field lands; they
        // re-fetch the ready field when they next change destination. [pending] dedupes the launch.
        fun getOrCreateVectorField(destination: Pos): VectorField {
            val maybeField = fields[destination]
            if (maybeField != null && maybeField.isNotEmpty()) {
                return maybeField
            }
            if (Config.headlessFieldCompute && HtmlUtil.isNotRunningInBrowser()) {
                return Pathfinding.computeFieldSync(destination).also { fields[destination] = it } // headless match
            }
            if (pending.add(destination)) {
                Pathfinding.computeFieldAsync(destination) { field ->
                    fields[destination] = field
                    pending.remove(destination)
                    LoadingOverlay.detail("Preparing routes…")
                    Sound.playOffScreenLocationCreationSound()
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

        /** Drop the cached offscreen flow fields, pending launches and id counter — for the headless
         *  match harness resetting state between matches (ai.SimRunner). */
        fun reset() {
            fields.clear()
            pending.clear()
            seq = 0
        }

        fun findNearestTo(pos: Pos) = World.allNonFaction.minByOrNull {
            it.pos.distanceTo(pos)
        } ?: throw IllegalStateException("Unable to find nearest to $pos")

        /** A random NPC to recruit (the recruiter walks up to whoever — recruiting isn't tied to a portal). */
        fun findRandom(): NonFaction? {
            val all = World.allNonFaction
            if (all.isEmpty()) return null
            return all.elementAt((Util.random() * all.size).toInt().coerceIn(0, all.size - 1))
        }

        fun create(grid: Grid): NonFaction {
            val position = Positions.createRandomPassable(grid)
            val size = AgentSize.createRandom()
            val speed = Skills.randomNpcSpeed()
            val newNonFaction = if (Util.random() < OFFSCREEN_DEST_CHANCE) { // mostly cross the map edge-to-edge
                val destination = opposingOffscreenDestination(position)
                val vectorField = getOrCreateVectorField(destination)
                NonFaction(position, speed, size, destination, vectorField, World.tick)
            } else { // some still head to a portal so there's life around them
                val portal = World.allPortals[(Util.random() * (World.allPortals.size - 1)).toInt()]
                NonFaction(position, speed, size, portal.location, portal.vectors, World.tick)
            }
            Sound.playNpcCreationSound(newNonFaction)
            return newNonFaction // rendered in 3D by Scene3D.sync(); no 2D draw (it'd just flash then clear)
        }
    }
}
