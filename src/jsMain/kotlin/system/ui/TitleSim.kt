package system.ui

import World
import agent.Agent
import agent.Faction
import config.Config
import config.Locations
import config.Sim
import config.Time
import extension.Grid
import items.PowerCube
import items.XmpBurster
import items.deployable.Resonator
import items.deployable.Shield
import items.level.XmpLevel
import items.types.ShieldType
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import portal.Portal
import portal.PortalKey
import portal.XmMap
import system.audio.Sound
import system.display.PortalNameTicker
import system.display.Scene3D
import system.display.SunController
import system.display.VectorFieldOverlay
import system.grid.GridConnectivity
import system.map.GeoLocator
import system.map.MapController
import util.data.Pos
import kotlin.js.Json

/**
 * The faction-screen backdrop is a **real, small game sim** — a handful of portals, a 3-v-3 agent
 * roster, and a few dozen NPCs, all driven by the actual tick loop / AI on a real grid. So the title
 * is literally the game running behind the menu (same rendering, pathing, captures, links, fields,
 * XMPs — no parallel code). Built small + at a fixed location so it spins up fast; wiped by the reload
 * when a faction is picked (the game has no in-place teardown; see Bootstrap's reload handoff).
 */
object TitleSim {
    private const val TITLE_PORTALS = 8
    private const val TITLE_COMBAT_DYNAMISM = 0.9 // quite dynamic (near max) so the demo combat reads lively
    private val TITLE_LEVELS = intArrayOf(3, 5, 8) // one agent per faction at each of these levels
    private const val TITLE_NPCS = 30
    private const val TITLE_XMPS = 60 // ≥ Attacker's threshold so the agents actually fire
    private const val TITLE_RESOS = 24
    private const val TITLE_CUBES = 10
    private const val MAX_TITLE_RETRIES = 4 // try a few iconic locations before forcing the known-good default
    private const val TITLE_BUILD_MESH_MS = 3500 // after the title city has risen, swap to our own meshes (→ shadows)
    private const val TITLE_SUN_SLOW_MS = 8000 // let the intro sun sweep fast, then ease it to a slow drift

    // Title mini-game: click the scene to blast. LMB = a full L8 XMP; RMB = an "ultra-strike" — the same
    // burst squished to a tight, brighter, higher-pitched hit (no dedicated ultra animation yet).
    private const val TITLE_BLAST_LEVEL = 8
    private const val ULTRA_SQUISH = 0.32 // tighter footprint than a normal XMP so it reads clearly different
    private const val ULTRA_BRIGHT = 1.5

    private var interval = 0
    private var started = false

    /** Fires when the 3D title letters are in the scene (so the UI can time the faction menu to them). */
    var onTitleReady: (() -> Unit)? = null

    fun start() {
        if (started) return
        started = true
        World.userFaction = Faction.ENL
        Scene3D.showBorder = false // no boundary wall/mask on the title
        PortalNameTicker.setEnabled(false) // the title never shows portal names (re-enabled by default in-game)
        Scene3D.titleWordmarkOnReady = {
            // fires once the 3D letters are in the scene
            hideDomWordmark()
            onTitleReady?.invoke()
        }
        hideDomWordmark() // the 3D letters replace it — hide the flat DOM text immediately (no flash)
        window.addEventListener("pointerdown", { Sound.enableAudio() }) // autoplay: unlock on first gesture
        Sim.setSize(Sim.presetWidth(Sim.SMALL_SCALE), Sim.presetHeight(Sim.SMALL_SCALE)) // small → fast to build
        Sim.roundField = true // a centered round arena → portals cluster around the centre (border stays hidden)
        VectorFieldOverlay.flashEnabled = false // no flow-field flashes on the title
        Config.startPortals = TITLE_PORTALS
        Config.combatDynamism = TITLE_COMBAT_DYNAMISM // the title demo plays flashier than the default game
        // Open on the player's home if location is *already* shared (no prompt — GeoLocator); otherwise
        // a random iconic location. Both build live (no precomputed paths), so any location works.
        GeoLocator.homeIfPermitted(
            onLocated = { lng, lat -> loadTitleWorld(JSON.parse("[$lng,$lat]")) },
            onNone = { loadTitleWorld(Locations.randomTitle().toJSON()) },
        )
    }

    private fun loadTitleWorld(center: Json, attempt: Int = 0) {
        MapController.loadMaps(center, demo = false, callback = fun(grid: Grid) {
            // Not every location is playable at the small round title size — a home over open water (a
            // player on a ship) or an unexpectedly sparse spot has no room for paths/portals. Fall back
            // to an iconic location, forcing the known-good default on the final try. Same gate as the
            // live game (GridConnectivity.MIN_WALKABILITY), checked here because it needs the live readback.
            if (World.walkability < GridConnectivity.MIN_WALKABILITY && attempt < MAX_TITLE_RETRIES) {
                val next = if (attempt + 1 >= MAX_TITLE_RETRIES) Locations.DEFAULT else Locations.randomTitle()
                console.warn(
                    "Title location unplayable (walkability ${(World.walkability * 100).toInt()}%) — retrying at ${next.displayName}",
                )
                loadTitleWorld(next.toJSON(), attempt + 1)
                return
            }
            World.grid = grid
            World.isReady = true
            MapController.enable3D()
            MapController.startTitleCinematic() // 3D terrain + zoom to frame the arena + slow orbit
            buildWorld()
            bindBlasts()
            // Our own building meshes (→ real shadows) once the title city has risen; then ease the
            // intro sun from its fast sweep down to a slow drift.
            window.setTimeout({ MapController.buildBuildingColliders() }, TITLE_BUILD_MESH_MS)
            window.setTimeout({ SunController.setSpeed(false) }, TITLE_SUN_SLOW_MS)
        })
    }

    // Let the player blast the title scene: LMB = L8 XMP, RMB = a squished/brighter/higher ultra-strike.
    // Both actually damage the scene (shatter portals, drop shields) like an agent's XMP — not just FX.
    private fun bindBlasts() {
        MapController.bindTitleBlasts(
            { e -> MapController.eventToSimPos(e)?.let { blastAt(it, ultra = false) } },
            { e -> MapController.eventToSimPos(e)?.let { blastAt(it, ultra = true) } },
        )
    }

    private fun blastAt(pos: Pos, ultra: Boolean) {
        val attacker = World.allAgents.firstOrNull() ?: return // any agent credits the (title) damage
        // Detonate FIRST (records the blast origin) so destroyed resos/mods fly away from it, then damage.
        if (ultra) {
            Scene3D.playXmpBurst(pos, TITLE_BLAST_LEVEL, squishXY = ULTRA_SQUISH, bright = ULTRA_BRIGHT, ultra = true)
        } else {
            Scene3D.playXmpBurst(pos, TITLE_BLAST_LEVEL)
        }
        XmpBurster.blastAt(pos, XmpLevel.valueOf(TITLE_BLAST_LEVEL), attacker, ultra = ultra)
    }

    // The 3D letters replace the flat DOM wordmark — hide it once they're in the scene.
    private fun hideDomWordmark() {
        (document.getElementsByClassName("titleBrand").item(0) as? HTMLElement)?.style?.display = "none"
    }

    /** Stop the tick + camera drift (the scene itself is torn down by the onboarding reload). */
    fun stop() {
        if (interval != 0) window.clearInterval(interval)
        interval = 0
        MapController.stopTitleOrbit()
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
