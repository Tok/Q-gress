package system.ui

import World
import agent.Agent
import agent.Faction
import agent.StuckTracker
import config.Config
import config.Locations
import config.Sim
import extension.Grid
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.w3c.dom.HTMLDivElement
import portal.Portal
import system.audio.Tts
import system.display.Scene3D
import system.display.SunController
import system.grid.GridConnectivity
import system.map.Attribution
import system.map.MapCamera
import system.map.MapController
import system.map.Navigation
import system.ui.panel.TuningPanel
import util.GameUrl
import util.NameGen
import util.Profiler
import util.Rng
import util.freshSeed
import kotlin.js.Json

/**
 * The world-generation pipeline, split out of [Bootstrap] (the app hub): seed + size from the URL,
 * load the maps → build the grid, gate unplayable (mostly-water) areas, spawn portals + agents + NPCs
 * behind the loading overlay, then start the game loop. Calls back into [Bootstrap] for the bits the app
 * shell still owns (faction pick, the per-frame `tick`, the map click/hover handlers, location state, and
 * the shared `createButton` / `doNewGame` helpers).
 */
object WorldBuilder {
    private var coloredMap = true // terrain colour on (eases in after world-gen); Menu toggle flips it

    fun initWorld(center: Json) {
        // Size + seed from a shared link (if present) → reproduce the exact world; else a fresh seed
        // (captured for sharing). Set before generation, since Rng.random is the sole RNG source.
        GameUrl.size()?.let { Sim.setSize(it.first, it.second) }
        // Apply the rest of the onboarding settings if present in the URL (deep link / reload handoff).
        GameUrl.portals()?.let { Config.startPortals = it }
        GameUrl.npcMultiplier()?.let { Config.npcMultiplier = it }
        GameUrl.startStage()?.let { Config.startStage = it }
        GameUrl.round()?.let { Sim.roundField = it }
        Rng.seed(GameUrl.seed() ?: freshSeed())
        Profiler.beginWorldGen()
        Onboarding.close() // dismiss the onboarding screen (it loads without a reload)
        // Staged loading overlay, up before the first tile request (the world build runs ~2 min on Big).
        LoadingOverlay.show()
        MapController.loadMaps(center, callback = onMapload())
    }

    private fun createQSliders(fact: Faction) {
        // One merged tuning list in the dock's TUNE tab (Actions, a divider, then Destinations),
        // instead of the two separate floating slider panes.
        TuningPanel.build(fact, GameUrl.isReadOnly())
        GameUrl.tune()?.let { TuningPanel.importTuning(it) } // restore shared-link tuning
    }

    private fun createPortals(callback: () -> Unit) {
        val total = Config.startPortals.coerceAtLeast(Config.minPortals) // never gen below the floor (always ≥5)
        fun createPortal(callback: () -> Unit, count: Int) {
            document.defaultView?.setTimeout(fun() {
                if (count > 0) {
                    val newPortal = Portal.createRandom()
                    LoadingOverlay.building(
                        LoadingOverlay.PCT_WORLD,
                        LoadingOverlay.PCT_PEOPLE,
                        total - count + 1,
                        total,
                        "Creating portal ${newPortal.name}",
                    )
                    World.allPortals.add(newPortal)
                    // Render the spawning world behind the (now translucent) loading screen: the new
                    // portal grows in. Its flow field is computed async and, once ready, joins the
                    // paced VectorFieldOverlay sweep (driven by Scene3D.render, the continuous loop).
                    Scene3D.sync()
                    createPortal(callback, count - 1)
                } else {
                    callback()
                }
            }, 0)
        }
        LoadingOverlay.detail("Creating portals…")
        World.allPortals.clear()
        createPortal(callback, total)
    }

    private fun createAgents(callback: () -> Unit) {
        World.allAgents.clear()
        StuckTracker.reset() // fresh world → drop stale stuck-history (?debug)
        NameGen.reset() // fresh roster → fresh handle dedupe
        LoadingOverlay.detail("Deploying ${Config.startFrogs()} ENL + ${Config.startSmurfs()} RES agents…")
        repeat(Config.startFrogs()) {
            World.allAgents.add(Agent.createFrog(World.grid))
        }
        repeat(Config.startSmurfs()) {
            World.allAgents.add(Agent.createSmurf(World.grid))
        }
        World.allNonFaction.clear()
        // Auto-size the population for this map + location (walkability is known now the grid is built);
        // held constant afterwards by 1-for-1 replacement on recruit, so we never run out of recruits.
        // Curated showpiece (title-eligible) locations count as tourist hotspots → a crowd bonus.
        val tourist = Locations.byCoords(Bootstrap.currentLng, Bootstrap.currentLat)?.title ?: false
        Config.maxNonFaction = Config.npcPopulation(Sim.width, Sim.height, World.walkability, tourist)
        World.createNonFaction(callback, Config.maxFor())
    }

    private fun createAgentsAndPortals(callback: () -> Unit) = createPortals(fun() {
        Profiler.mark("portals (${World.allPortals.size})")
        Profiler.flushFields() // most per-portal flow fields are queued by now
        createAgents(callback)
    })

    private fun onMapload() = fun(grid: Grid) {
        World.grid = grid
        Profiler.mark("maps + grid (${grid.size} cells)")
        if (World.grid.isEmpty()) {
            console.error("Grid is empty!")
        }
        // Gate mostly-water / unplayable locations before the expensive world build. Auto-start
        // (dev/headless) is exempt so tests never block.
        if (World.walkability < GridConnectivity.MIN_WALKABILITY && !GameUrl.isAutoStart()) {
            showUnplayableGate()
            return
        }
        // Anchor the 3D scene BEFORE spawning portals: Portal.create → PortalNames.nameFor projects
        // POI/street lng/lat through Scene3D.lngLatToSimPos, which throws until Scene3D is registered.
        // Registering first means portals actually adopt their real map names (else all fall back to
        // the random generator).
        MapController.enable3D()
        // Sample the DEM height grid BEFORE spawning portals (+ schedule retries as tiles stream in), so the
        // flow-field arrows that flash during world-gen sit on the terrain instead of at sea level. Without
        // this the first build often shows the vectors too low (heights weren't ready); a re-sample after the
        // Home view settles (below) keeps them accurate.
        Scene3D.onTerrainChanged()
        MapCamera.startBuildCinematic() // gentle orbit while portals + people spawn
        LoadingOverlay.stage(LoadingOverlay.PCT_WORLD, "Building world…")
        createAgentsAndPortals {
            LoadingOverlay.detail("Computing routes & starting simulation…")
            // Start the game with nothing selected (the vector field flashes itself on new portals).
            Scene3D.selected = null
            if (World.userFaction == null) {
                Bootstrap.chooseUserFaction(Faction.random())
            }
            createQSliders(World.userFactionOrThrow())
            Profiler.mark("agents+NPCs (${World.allAgents.size}+${World.allNonFaction.size})")
            GameLoop.start { Bootstrap.tick() }
            World.isReady = true
            console.log("[perf] WORLD READY") // headless profiler waits on this before the runtime window
            FpsMeter.start() // arms the FPS readout (menu-toggled display + ?debug console capture)
            document.getElementById("top-controls")?.removeClass("invisible") // reveal the toolbar now
            document.getElementById(Bootstrap.LOCATION_LABEL_ID)?.removeClass("invisible") // …and the location name (in #hudTop)
            Tts.announceLocation(Bootstrap.currentLocationName) // scanner reads the theatre of operations as play begins
            Attribution.collapse() // we've left the title → tuck the map credit into its (i)
            MapController.showSatellite() // terrain stays grayscale (set at map-load) until the fade below
            Navigation.setup()
            MapController.bindInteractions(Bootstrap::onMapClick, Bootstrap::onMapMove)
            LoadingOverlay.done()
            MapCamera.stopBuildCinematicAndHome() // settle to the Home view (top-down over the play area)
            if (coloredMap) MapController.fadeInColor() else MapController.setColored(false) // colour eases in post-build
            Scene3D.onTerrainChanged() // sample the DEM height grid (objects sit on the terrain)
            // Once the Home view has settled (buildings on screen), build our own building meshes +
            // colliders (so debris lands on roofs and the sun casts real building shadows).
            window.setTimeout({ MapController.buildBuildingColliders() }, 1600)
            SunController.setSpeed(false) // the intro's fast sun sweep eases to a slow drift in-game
        }
    }

    /** Block the build when the chosen area is mostly water/blocked; offer to pick another location. */
    private fun showUnplayableGate() {
        LoadingOverlay.done()
        val screen = document.createElement("div") as HTMLDivElement
        screen.id = "unplayableGate"
        screen.addClass("onboardScreen")
        val title = document.createElement("div") as HTMLDivElement
        title.addClass("onboardTitle")
        title.textContent = "Not enough ground to play"
        screen.append(title)
        val hint = document.createElement("div") as HTMLDivElement
        hint.addClass("onboardHint")
        val pct = (World.walkability * 100).toInt()
        hint.textContent = "“${Bootstrap.currentLocationName}” is only $pct% walkable — mostly water/blocked. Pick another."
        screen.append(hint)
        screen.append(
            Bootstrap.createButton("gateNewGame", "topButton displayFont onboardStart", "Choose another location") {
                Bootstrap.doNewGame()
            },
        )
        document.body?.append(screen)
    }
}
