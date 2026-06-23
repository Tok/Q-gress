package util.ui

import World
import agent.Agent
import agent.Faction
import config.Config
import config.Location
import config.Sim
import config.Time
import extension.Grid
import items.PowerCube
import items.XmpBurster
import items.deployable.Resonator
import items.deployable.Shield
import items.types.ShieldType
import kotlinx.browser.window
import portal.Portal
import portal.PortalKey
import portal.XmMap
import system.display.Scene3D
import system.display.VectorFieldOverlay
import util.MapUtil
import util.SoundUtil

/**
 * The faction-screen backdrop is a **real, small game sim** — a handful of portals, a 3-v-3 agent
 * roster, and a few dozen NPCs, all driven by the actual tick loop / AI on a real grid. So the title
 * is literally the game running behind the menu (same rendering, pathing, captures, links, fields,
 * XMPs — no parallel code). Built small + at a fixed location so it spins up fast; wiped by the reload
 * when a faction is picked (the game has no in-place teardown; see HtmlUtil's reload handoff).
 */
object TitleSim {
    private const val TITLE_PORTALS = 8
    private val TITLE_LEVELS = intArrayOf(3, 5, 8) // one agent per faction at each of these levels
    private const val TITLE_NPCS = 30
    private const val TITLE_XMPS = 60 // ≥ Attacker's threshold so the agents actually fire
    private const val TITLE_RESOS = 24
    private const val TITLE_CUBES = 10

    private var interval = 0
    private var started = false

    fun start() {
        if (started) return
        started = true
        World.userFaction = Faction.ENL
        Scene3D.showBorder = false // no boundary wall/mask on the title
        window.addEventListener("pointerdown", { SoundUtil.enableAudio() }) // autoplay: unlock on first gesture
        Sim.setSize(Sim.presetWidth(Sim.SMALL_SCALE), Sim.presetHeight(Sim.SMALL_SCALE)) // small → fast to build
        Sim.roundField = true // a centered round arena → portals cluster around the centre (border stays hidden)
        VectorFieldOverlay.flashEnabled = false // no flow-field flashes on the title
        Config.startPortals = TITLE_PORTALS
        MapUtil.loadMaps(Location.DEFAULT.toJSON(), demo = false, callback = fun(grid: Grid) {
            World.grid = grid
            World.isReady = true
            MapUtil.enable3D()
            MapUtil.startTitleCinematic() // 3D terrain + zoom to frame the arena + slow orbit
            buildWorld()
        })
    }

    /** Stop the tick + camera drift (the scene itself is torn down by the onboarding reload). */
    fun stop() {
        if (interval != 0) window.clearInterval(interval)
        interval = 0
        MapUtil.stopTitleOrbit()
    }

    private fun buildWorld() {
        World.allPortals.clear()
        repeat(TITLE_PORTALS) { World.allPortals.add(Portal.createRandom()) }
        World.allAgents.clear()
        // 3-v-3, one agent per faction at each title level (their AP sets the level).
        val frogs = TITLE_LEVELS.map { lvl -> Agent.createFrog(World.grid).also { it.ap = apForLevel(lvl) } }
        val smurfs = TITLE_LEVELS.map { lvl -> Agent.createSmurf(World.grid).also { it.ap = apForLevel(lvl) } }
        frogs.forEach { World.allAgents.add(it) }
        smurfs.forEach { World.allAgents.add(it) }
        equip(frogs)
        equip(smurfs)
        World.allNonFaction.clear()
        World.createNonFaction({}, TITLE_NPCS) // paced serial drop-in (renders each as it lands)
        Scene3D.sync()
        interval = window.setInterval({ tick() }, Time.minTickInterval)
    }

    // Lowest AP within each level band → that exact level (see Agent.getLevel thresholds).
    private fun apForLevel(level: Int) = when (level) {
        3 -> 50_000
        5 -> 200_000
        8 -> 1_500_000
        else -> 50_000
    }

    // Each title agent: XMPs (+ resos/cubes) matched to its OWN level, a key to every portal (so they
    // link + field), and a shield for the L8 of each faction. (Ultra Strikes aren't implemented yet.)
    private fun equip(agents: List<Agent>) {
        agents.forEach { a ->
            val lvl = a.getLevel().coerceIn(1, 8)
            val inv = a.inventory
            repeat(TITLE_XMPS) { inv.addItem(XmpBurster.create(a, lvl)) }
            repeat(TITLE_RESOS) { inv.addItem(Resonator.create(a, lvl)) }
            repeat(TITLE_CUBES) { inv.addItem(PowerCube.create(a, lvl)) }
            World.allPortals.forEach { p -> inv.addItem(PortalKey(p, a)) }
            if (lvl >= 8) inv.addItem(Shield(ShieldType.VERY_RARE, a))
        }
    }

    // The game's tick, minus the HUD: agents + NPCs act, then re-render the scene from world state.
    private fun tick() {
        if (!World.isReady) return
        val next = World.allAgents.toList().map { it.act() }.toSet()
        XmMap.updateStrayXm()
        World.allAgents.clear()
        World.allAgents.addAll(next)
        World.flushPendingAgents()
        World.allNonFaction.forEach { it.act() }
        window.requestAnimationFrame { Scene3D.sync() }
        World.tick++
    }
}
