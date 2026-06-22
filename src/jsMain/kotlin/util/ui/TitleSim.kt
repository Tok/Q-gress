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
import util.Util

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
        val frogs = List(TITLE_FROGS) { Agent.createFrog(World.grid) }
        val smurfs = List(TITLE_SMURFS) { Agent.createSmurf(World.grid) }
        frogs.forEach { World.allAgents.add(it) }
        smurfs.forEach { World.allAgents.add(it) }
        equip(frogs) // the first frog gets a shield
        equip(smurfs) // the first smurf gets a shield
        World.allNonFaction.clear()
        World.createNonFaction({}, TITLE_NPCS) // paced serial drop-in (renders each as it lands)
        Scene3D.sync()
        interval = window.setInterval({ tick() }, Time.minTickInterval)
    }

    // Give title agents a lively loadout: varied XMPs (so attacks differ), resos + cubes to build,
    // a key to every portal (so they link + field), and one shield per faction.
    private fun equip(agents: List<Agent>) {
        agents.forEachIndexed { i, a ->
            val inv = a.inventory
            repeat(TITLE_XMPS) { inv.addItem(XmpBurster.create(a, 3 + Util.randomInt(0, 5))) } // levels 3–8
            repeat(TITLE_RESOS) { inv.addItem(Resonator.create(a, 5 + Util.randomInt(0, 3))) }
            repeat(TITLE_CUBES) { inv.addItem(PowerCube.create(a, 6)) }
            World.allPortals.forEach { p -> inv.addItem(PortalKey(p, a)) }
            if (i == 0) inv.addItem(Shield(ShieldType.VERY_RARE, a))
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
