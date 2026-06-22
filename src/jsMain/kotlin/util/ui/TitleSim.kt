package util.ui

import World
import agent.Agent
import agent.Faction
import config.Config
import config.Location
import config.Sim
import config.Time
import extension.Grid
import kotlinx.browser.window
import portal.Portal
import portal.XmMap
import system.display.Scene3D
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
    private const val TITLE_FROGS = 3
    private const val TITLE_SMURFS = 3
    private const val TITLE_NPCS = 40

    private var interval = 0
    private var started = false

    fun start() {
        if (started) return
        started = true
        World.userFaction = Faction.ENL
        Scene3D.showBorder = false // no boundary wall/mask on the title
        window.addEventListener("pointerdown", { SoundUtil.enableAudio() }) // autoplay: unlock on first gesture
        Sim.setSize(Sim.presetWidth(Sim.SMALL_SCALE), Sim.presetHeight(Sim.SMALL_SCALE)) // small → fast to build
        Sim.roundField = false // border is hidden anyway; skip the circle mask
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
        repeat(TITLE_FROGS) { World.allAgents.add(Agent.createFrog(World.grid)) }
        repeat(TITLE_SMURFS) { World.allAgents.add(Agent.createSmurf(World.grid)) }
        World.allNonFaction.clear()
        World.createNonFaction({}, TITLE_NPCS) // paced serial drop-in (renders each as it lands)
        Scene3D.sync()
        interval = window.setInterval({ tick() }, Time.minTickInterval)
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
