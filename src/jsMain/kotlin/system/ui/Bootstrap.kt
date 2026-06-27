package system.ui
import World
import agent.Agent
import agent.Faction
import agent.StuckTracker
import config.*
import config.Sim
import extension.CanvasFactory
import extension.Grid
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.w3c.dom.*
import org.w3c.dom.events.Event
import portal.Portal
import system.Cycle
import system.HeadlessRun
import system.Simulation
import system.audio.AmbientPrefs
import system.audio.AudioPrefs
import system.audio.InstrumentPrefs
import system.audio.MixerPrefs
import system.audio.Scale
import system.audio.Sound
import system.building.BuildingTransparency
import system.display.Scene3D
import system.display.Showcases
import system.display.SunController
import system.grid.GridCapture
import system.grid.GridConnectivity
import system.map.Attribution
import system.map.MapController
import system.map.Navigation
import system.ui.AudioDemo
import system.ui.Controls
import system.ui.Demo
import system.ui.Footer
import system.ui.Hud
import system.ui.Icons
import system.ui.Inspector
import system.ui.LoadingOverlay
import system.ui.MenuControls
import system.ui.Onboarding
import system.ui.VolumeControl
import system.ui.panel.DropRatesPanel
import system.ui.panel.TuningPanel
import util.Debug
import util.GameUrl
import util.GameplayPrefs
import util.NameGen
import util.Rng
import util.data.*
import util.data.toJson
import util.freshSeed
import kotlin.js.Json

@Suppress("UnusedParameter") // external JS global; param describes the contract
external fun encodeURIComponent(uri: String): String

object Bootstrap {
    private var coloredMap = true // terrain colour on (eases in after world-gen); Menu toggle flips it
    private const val LOCATION_LABEL_ID = "locationLabel"

    // The actually-loaded location (set by setLoadedLocation) — named in the top bar, and the target
    // a Reset reloads onto.
    private var currentLng = Locations.DEFAULT.lng
    private var currentLat = Locations.DEFAULT.lat
    private var currentLocationName = Locations.DEFAULT.displayName

    /** The loaded place name (for flavouring generated content, e.g. agent handles). */
    fun locationName() = currentLocationName

    fun isRunningInBrowser() = jsTypeOf(document) != "undefined"
    fun isNotRunningInBrowser() = !isRunningInBrowser()
    fun isLocal() = isRunningInBrowser() && document.location?.href?.contains("localhost") ?: false

    private fun tick() {
        if (!World.isReady || HeadlessRun.active) return // paused during an in-browser headless eval (trainer / leaderboard)
        Simulation.stepEntities() // shared functional-core step (agents + NPCs + stuck tracker)
        window.requestAnimationFrame {
            HudRenderer.redraw()
            val userFaction = World.userFactionOrThrow()
            val factions = userFaction to userFaction.enemy()
            val enlMu = World.calcTotalMu(Faction.ENL)
            val resMu = World.calcTotalMu(Faction.RES)
            Cycle.updateCheckpoints(World.tick, enlMu, resMu)
            val firstMu = if (factions.first == Faction.ENL) enlMu else resMu
            val secondMu = if (factions.first == Faction.RES) enlMu else resMu
            Scale.setLeading(firstMu >= secondMu) // major key while the player's faction leads, else minor
            HudRenderer.redrawUserInterface(firstMu, secondMu, factions)
            World.tick++
        }
    }

    fun load() {
        if (isNotRunningInBrowser()) return
        if (Controls.isUnsupported()) {
            Controls.showUnsupportedNotice()
            return
        }
        val rootDiv = document.getElementById("root") as HTMLDivElement
        rootDiv.addClass("container")
        // Master volume + mute live in their own fixed widget so they stay visible across every phase
        // (title, onboarding, world-gen, gameplay) — not bound to a screen/toolbar that comes and goes.
        Sound.restoreVolume() // re-read the saved volume/mute BEFORE the widget builds (survives reloads)
        AudioPrefs.load() // re-apply the saved master-FX tuning (AudioFx.build later picks it up via applyAll)
        MixerPrefs.load() // re-apply saved per-role mixer levels/mutes (the lazy gain buses pick them up)
        InstrumentPrefs.load() // re-apply saved per-instrument tuning (explosion kick)
        AmbientPrefs.load() // restore the ambient bed (incl. re-starting it if it was on)
        GameplayPrefs.load() // re-apply saved gameplay knobs BEFORE the menu sliders read Config to seed themselves
        document.body?.appendChild(createPersistentVolume())

        // Offscreen ImageData factory for the passability-grid readback (never displayed). No
        // on-screen 2D canvas layer remains — the world renders in the three.js custom layer and
        // the HUD is DOM.
        World.bgCan = CanvasFactory.createOffscreenCanvas(Dim.width, Dim.height)

        // /demo harness (hash-routed): a menu of effect demo scenes, separate from the game.
        // Reload on any hash change so switching game ⇄ demo (or between scenes) re-bootstraps.
        window.addEventListener("hashchange", { window.location.reload() })
        when (Demo.route(window.location.hash)) {
            Demo.Scene.MENU -> {
                Demo.showMenu()
                return
            }
            Demo.Scene.SANDBOX -> {
                loadDemoScene()
                return
            }
            Demo.Scene.AUDIO -> {
                AudioDemo.show()
                return
            }
            null -> Unit // not a demo route → load the game
        }

        // Map clicks/hover are wired to MapLibre after the map loads (see onMapload → bindInteractions).
        val controlDiv = createControlDiv()
        val buttonDiv = document.createElement("div") as HTMLDivElement
        buttonDiv.addClass("buttonDiv")

        // Left group, far left: Menu (also holds the Terrain/Vectors overlay toggles), Home,
        // Pause/Resume, and the loaded-location name.
        val leftGroup = document.createElement("div") as HTMLDivElement
        leftGroup.addClass("toolbarGroup")
        leftGroup.append(createMenuSpan()) // New Game / Reset + overlay toggles
        // Recenter top-down over the play area (find your way back after panning/rotating away).
        leftGroup.append(createButton("homeButton", "topButton displayFont", "HOME") { MapController.goHome() })
        leftGroup.append(createSpeedControls()) // Pause + ×1/×3/Max (Space toggles pause; -/+ keys nudge speed)
        leftGroup.append(createAutoCamToggle()) // Auto cam sits right after the speed controls (camera ≈ playback)
        bindKeyboardShortcuts() // Space=pause · Home=recenter · zoom/pan/speed/mute/Esc

        // Keep the toggle's highlight in sync — incl. when a manual move snaps the drift (and toggle) out.
        MapController.onAutoCamChanged = { on -> syncAutoCamToggle(on) }
        MapController.setAutoCam(true)
        // Install the chosen AI drivers up front (the visible "AI vs AI" pickers now live in the BRAINS tab).
        system.ui.DriverControls.installDefaults()

        // Header centre: the live ENL-vs-RES scoreboard (StatsPanel fills #toolbarCentre). The loaded-location
        // name moves to the slim strip just below the header (Hud.top(), centred) — see createLocationLabel().
        val centre = document.createElement("div") as HTMLDivElement
        centre.id = "toolbarCentre"
        centre.addClass("toolbarCentre")
        buttonDiv.append(leftGroup)
        buttonDiv.append(centre)
        controlDiv.append(buttonDiv)
        Hud.top().append(createLocationLabel())

        rootDiv.append(controlDiv)

        Controls.addLegend()

        loadLocations { startOnboardingOrWorld() } // fetch the editable location catalogue, then run onboarding
    }

    // Fetch the editable location catalogue (resources/locations.json) into [Locations], then [onReady];
    // a missing/malformed file keeps the DEFAULT-only catalogue rather than blocking startup.
    private fun loadLocations(onReady: () -> Unit) {
        window.asDynamic().fetch("locations.json")
            .then { r: dynamic -> r.text() }
            .then { txt: dynamic ->
                runCatching { Locations.setAll(Locations.parse(txt as String)) }
                onReady()
            }
            .catch { _: dynamic -> onReady() }
    }

    /**
     * Gate the world load behind the onboarding order: **faction → map-size → location → load**, run
     * as in-memory screens (no reloads). `?local=true` auto-starts; a faction+lng/lat deep link loads
     * directly (e.g. the in-game location dropdown's reload). Demo scenes returned earlier, before this.
     */
    private fun startOnboardingOrWorld() {
        if (Debug.mode == "capture") { // ?debug=capture: sweep presets → download fixtures, no world
            LoadingOverlay.show()
            GridCapture.sweep()
            return
        }
        val faction = GameUrl.faction()
        val urlCenter = GameUrl.lngLat()
        when {
            GameUrl.isAutoStart() -> {
                chooseUserFaction(faction ?: Faction.random())
                loadUrlLocation(urlCenter, Locations.DEFAULT.displayName)
                initWorld(centerOrDefault())
            }
            faction != null && urlCenter != null -> { // deep link → straight to load
                chooseUserFaction(faction)
                loadUrlLocation(urlCenter, "Custom location")
                initWorld(centerOrDefault())
            }
            else -> runOnboarding()
        }
    }

    // Back-able flow (faction → map size → location); Esc steps back (escClose). Block bodies (Unit) on
    // purpose — expression bodies make the mutual references recurse in type inference.
    private fun runOnboarding() = onboardFaction()

    private fun onboardFaction() {
        Onboarding.showFaction { f ->
            World.userFaction = null // clear the title sim's placeholder faction so the real pick takes
            chooseUserFaction(f)
            onboardDrivers()
        }
    }

    // Step 2 — pick each side's brain (default net vs net). Choices ride the start-URL (GameUrl) into the game.
    private fun onboardDrivers() {
        Onboarding.showDrivers(World.userFactionOrThrow(), onBack = { onboardFaction() }) { onboardMapSize() }
    }

    private fun onboardMapSize() {
        Onboarding.showMapSize(Config.startPortals, onBack = { onboardDrivers() }) { w, h, portals, stage ->
            Sim.setSize(w, h) // size first, so the location screen's play-area box is the real size
            Config.startPortals = portals
            Config.startStage = stage
            onboardLocation()
        }
    }

    // Reload into the game via the deep-link URL — cleanly wipes the title sim, reusing the reload load path.
    private fun onboardLocation() {
        Onboarding.showLocation(onBack = { onboardMapSize() }) { lng, lat, name -> navigateToLocation(lng, lat, name) }
    }

    private fun centerOrDefault(): Json = GameUrl.lngLat()?.toJson() ?: Locations.DEFAULT.toJSON()

    // Always-visible master-volume widget (fixed top-right), created once and parented to <body> so it
    // persists across the title, onboarding, world-gen and gameplay. Keeps the "soundLabel"/"volumeSlider"
    // ids the mute shortcut + audio-enable rely on.
    private fun createPersistentVolume(): HTMLDivElement {
        val wrap = document.createElement("div") as HTMLDivElement
        wrap.addClass("persistentVolume")
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label", "topLabel", "topIcon", "volumeIcon")
        label.id = "soundLabel"
        val slider = document.createElement("input") as HTMLInputElement
        slider.id = "volumeSlider"
        slider.type = "range"
        slider.min = "0.0"
        slider.max = "1.0"
        slider.step = "0.05"
        slider.value = Sound.displayVolume().toString() // reflect the restored/saved level (+ mute glyph)
        slider.addClass("slider", "volumeSlider")
        VolumeControl.build(label, slider) // speaker icon (click = mute) + slider
        wrap.append(label)
        wrap.append(slider)
        return wrap
    }

    // Four compact buttons replace the old pause button + speed slider: Pause (toggles, Space-bound) and
    // the ×1 / ×3 / Max presets (active one highlighted; -/+ keys still nudge).
    private fun createSpeedControls(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        span.addClass("toolbarGroup", "speedControls")
        span.append(createButton(GameLoop.PAUSE_BUTTON_ID, "topButton displayFont speedBtn", "Pause") { GameLoop.togglePause() })
        GameLoop.SPEED_PRESETS.forEach { (mult, label, id) ->
            span.append(createButton(id, "topButton displayFont speedBtn", label) { GameLoop.selectSpeed(mult) })
        }
        GameLoop.refreshSpeedButtons()
        return span
    }

    /** The Auto cam toggle button (icon-only, rightmost). Snaps out when a manual move cancels the drift. */
    private fun createAutoCamToggle(): HTMLButtonElement {
        val btn =
            createButton("autoCamToggle", "topButton displayFont autoCamBtn", "") { MapController.setAutoCam(!MapController.isAutoCamOn()) }
        btn.innerHTML = Icons.CAM
        btn.title = "Auto cam"
        return btn
    }

    private fun createControlDiv(): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
        div.id = "top-controls"
        div.addClass("controls", "invisible") // hidden through title/onboarding/loading; shown when ready
        return div
    }

    // Minimal bootstrap for the unified sandbox demo scene: just the 3D scene + the sandbox controls.
    private fun loadDemoScene() {
        World.userFaction = Faction.ENL
        val center = Pos(Sim.width / 2, Sim.height / 2)
        MapController.loadMaps(Locations.DEFAULT.toJSON(), demo = true, callback = fun(grid: Grid) {
            World.grid = grid
            World.isReady = true
            Navigation.setup()
            MapController.enable3D()
            // Sandbox map interaction: LMB places a portal on empty ground (a ground ring previews
            // place vs select) or selects the one under the cursor; RMB shatters the nearest. The
            // effects (Hack / Fire XMP) are panel buttons that act on the selected portal.
            MapController.bindPortalDemo(
                fun(event: dynamic) {
                    val pos = MapController.eventToSimPos(event) ?: return
                    Sound.enableAudio()
                    if (Demo.xmpOnClick()) {
                        Scene3D.playXmpBurst(pos, Demo.xmpLevel()) // detonate at the click point (plays the XMP sound)
                        return
                    }
                    if (Showcases.click(pos, Demo.portalLevel(), Demo.portalColorValue())) {
                        Sound.playPortalCreationSound(pos)
                    }
                },
                fun(event: dynamic) {
                    val pos = MapController.eventToSimPos(event) ?: return
                    Sound.enableAudio()
                    Showcases.removeNear(pos)
                    Sound.playGlassShatterSound(pos, 0.4, 0.9)
                },
                fun(event: dynamic) {
                    Showcases.moveCursor(MapController.eventToSimPos(event))
                },
            )
            GameLoop.start { demoTick() }
            Demo.showControls(center)
        })
    }

    private fun demoTick() {
        if (World.isReady) window.requestAnimationFrame { HudRenderer.redraw() }
    }

    private fun initWorld(center: Json) {
        // Size + seed from a shared link (if present) → reproduce the exact world; else a fresh seed
        // (captured for sharing). Set before generation, since Rng.random is the sole RNG source.
        GameUrl.size()?.let { Sim.setSize(it.first, it.second) }
        // Apply the rest of the onboarding settings if present in the URL (deep link / reload handoff).
        GameUrl.portals()?.let { Config.startPortals = it }
        GameUrl.npcMultiplier()?.let { Config.npcMultiplier = it }
        GameUrl.startStage()?.let { Config.startStage = it }
        GameUrl.round()?.let { Sim.roundField = it }
        Rng.seed(GameUrl.seed() ?: freshSeed())
        Onboarding.close() // dismiss the onboarding screen (it loads without a reload)
        // Staged loading overlay, up before the first tile request (the world build runs ~2 min on Big).
        LoadingOverlay.show()
        MapController.loadMaps(center, callback = onMapload())
    }

    private fun closePopup() {
        (document.getElementById("popup") as? HTMLDivElement)?.addClass("invisible")
    }

    private fun createQSliders(fact: Faction) {
        // One merged tuning list in the dock's TUNE tab (Actions, a divider, then Destinations),
        // instead of the two separate floating slider panes.
        TuningPanel.build(fact, GameUrl.isReadOnly())
        GameUrl.tune()?.let { TuningPanel.importTuning(it) } // restore shared-link tuning
    }

    private fun chooseUserFaction(fact: Faction) {
        Sound.enableAudio() // first user gesture → resume audio (autoplay policy)
        closePopup()
        if (World.userFaction != null) {
            console.warn("Faction ${World.userFaction} was already chosen.")
            return
        }
        World.userFaction = fact
        LoadingOverlay.setAccent(fact.color) // tint the loading screen with the chosen faction
    }

    private fun bindKeyboardShortcuts() = Shortcuts.bind(
        Shortcuts.Handlers(
            command = { onShortcutCommand(it) },
            zoom = { MapController.zoomBy(it) },
            pan = { dx, dy -> MapController.panBy(dx, dy) },
            buildingOpacity = { BuildingTransparency.nudge(it) },
            speedDelta = { GameLoop.nudgeSpeed(it) },
        ),
    )

    private fun syncAutoCamToggle(on: Boolean) =
        (document.getElementById("autoCamToggle") as? HTMLElement)?.let { if (on) it.addClass("active") else it.removeClass("active") }

    private fun onShortcutCommand(c: Shortcuts.Command) = when (c) {
        Shortcuts.Command.PAUSE -> GameLoop.togglePause()
        Shortcuts.Command.HOME -> MapController.goHome()
        Shortcuts.Command.CYCLE_TAB -> Footer.cycleTab()
        Shortcuts.Command.MUTE -> toggleMuteUi()
        Shortcuts.Command.AUTO_CAM -> MapController.setAutoCam(!MapController.isAutoCamOn()) // C — highlight syncs via onAutoCamChanged
        Shortcuts.Command.CLOSE -> escClose()
    }

    private fun toggleMuteUi() {
        // Reuse the speaker icon's own click handler (toggles mute + syncs slider + swaps the glyph) so the
        // M-key and the icon stay in lock-step; fall back to a bare toggle if the toolbar isn't built yet.
        val icon = document.getElementById("soundLabel") as? HTMLElement
        if (icon != null) icon.click() else Sound.toggleMute()
    }

    private fun escClose() {
        if (Onboarding.isShowing()) { // mid-onboarding, Esc steps back a screen instead of closing panels
            Onboarding.back()
            return
        }
        Controls.hideLegend()
        DropRatesPanel.close()
        document.querySelector(".gameMenu")?.classList?.add("invisible") // close the Menu dropdown if open
        closePopup()
    }

    private fun onMapClick(event: dynamic) {
        Sound.enableAudio() // first user gesture → resume audio (autoplay policy)
        // Ground point under the cursor; MapLibre fires "click" only for a click, not after a drag.
        val pos = MapController.eventToSimPos(event) ?: return
        // Portals are *discovered*, not placed by the player: a click only selects the portal under
        // the cursor (or deselects). Manual placement/removal lives only in the /#demo sandbox.
        if (pos.hasClosePortalForClick()) {
            Inspector.select("portal:" + pos.findClosestPortal().id)
        } else {
            Inspector.select(null)
        }
    }

    private fun onMapMove(event: dynamic) {
        // Hover affordance only — highlight the portal under the cursor (nothing is buildable now).
        val pos = MapController.eventToSimPos(event)
        if (pos != null && pos.hasClosePortalForClick()) {
            Scene3D.setBuildMarker(pos, "portal")
        } else {
            Scene3D.setBuildMarker(null, "")
        }
    }

    private fun maybeWidth(id: String) = document.getElementById(id)?.clientWidth
    private fun maybeHeight(id: String) = document.getElementById(id)?.clientHeight
    fun topActionOffset(): Int = maybeHeight("top-controls") ?: 100
    fun leftSliderWidth() = maybeWidth("left-sliders") ?: 241
    fun leftSliderHeight() = maybeHeight("left-sliders") ?: 217
    fun rightSliderWidth() = maybeWidth("right-sliders") ?: 213
    fun rightSliderHeight() = maybeHeight("right-sliders") ?: 145

    private fun createButton(id: String, className: String, text: String, callback: ((Event) -> Unit)?): HTMLButtonElement {
        val button = document.createElement("BUTTON") as HTMLButtonElement
        button.id = id
        button.addClass(className)
        button.onclick = callback
        button.innerText = text
        return button
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
        (1..Config.startFrogs()).forEach {
            World.allAgents.add(Agent.createFrog(World.grid))
        }
        (1..Config.startSmurfs()).forEach {
            World.allAgents.add(Agent.createSmurf(World.grid))
        }
        World.allNonFaction.clear()
        // Auto-size the population for this map + location (walkability is known now the grid is built);
        // held constant afterwards by 1-for-1 replacement on recruit, so we never run out of recruits.
        // Curated showpiece (title-eligible) locations count as tourist hotspots → a crowd bonus.
        val tourist = Locations.byCoords(currentLng, currentLat)?.title ?: false
        Config.maxNonFaction = Config.npcPopulation(Sim.width, Sim.height, World.walkability, tourist)
        World.createNonFaction(callback, Config.maxFor())
    }

    private fun createAgentsAndPortals(callback: () -> Unit) = createPortals(fun() {
        createAgents(callback)
    })

    private fun onMapload() = fun(grid: Grid) {
        World.grid = grid
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
        MapController.startBuildCinematic() // gentle orbit while portals + people spawn
        LoadingOverlay.stage(LoadingOverlay.PCT_WORLD, "Building world…")
        createAgentsAndPortals {
            LoadingOverlay.detail("Computing routes & starting simulation…")
            // Start the game with nothing selected (the vector field flashes itself on new portals).
            Scene3D.selected = null
            if (World.userFaction == null) {
                chooseUserFaction(Faction.random())
            }
            createQSliders(World.userFactionOrThrow())
            GameLoop.start { tick() }
            World.isReady = true
            document.getElementById("top-controls")?.removeClass("invisible") // reveal the toolbar now
            document.getElementById(LOCATION_LABEL_ID)?.removeClass("invisible") // …and the location name (in #hudTop, not the toolbar)
            Attribution.collapse() // we've left the title → tuck the map credit into its (i)
            MapController.showSatellite() // terrain stays grayscale (set at map-load) until the fade below
            Navigation.setup()
            MapController.bindInteractions(::onMapClick, ::onMapMove)
            LoadingOverlay.done()
            MapController.stopBuildCinematicAndHome() // settle to the Home view (top-down over the play area)
            if (coloredMap) MapController.fadeInColor() else MapController.setColored(false) // colour eases in post-build
            Scene3D.onTerrainChanged() // sample the DEM height grid (objects sit on the terrain)
            // Once the Home view has settled (buildings on screen), build our own building meshes +
            // colliders (so debris lands on roofs and the sun casts real building shadows).
            window.setTimeout({ MapController.buildBuildingColliders() }, 1600)
            SunController.setSpeed(false) // the intro's fast sun sweep eases to a slow drift in-game
        }
    }

    /** Record (and name in the top bar) the location the world actually loaded at. */
    private fun setLoadedLocation(lng: Double, lat: Double, name: String) {
        currentLng = lng
        currentLat = lat
        currentLocationName = name.replace("%20", " ").ifBlank { "Custom location" }
        document.getElementById(LOCATION_LABEL_ID)?.textContent = currentLocationName
    }

    // Load at the URL's lng/lat (falling back to DEFAULT), naming it from the URL or [fallbackName].
    private fun loadUrlLocation(center: GeoCoords?, fallbackName: String) =
        setLoadedLocation(center?.lng ?: Locations.DEFAULT.lng, center?.lat ?: Locations.DEFAULT.lat, GameUrl.name() ?: fallbackName)

    private fun createLocationLabel(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        span.id = LOCATION_LABEL_ID
        span.addClass("topLocation", "displayFont", "invisible") // hidden through title/onboarding; shown when the world is ready
        span.textContent = currentLocationName
        return span
    }

    /** "Menu" button → a small popup with New Game (re-onboard) and Reset (reload this location). */
    private fun createMenuSpan(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        span.addClass("menuSpan")
        val menu = document.createElement("div") as HTMLDivElement
        menu.addClass("gameMenu", "invisible")
        menu.append(createButton("menuNewGame", "menuItem displayFont", "New Game") { doNewGame() })
        menu.append(createButton("menuReset", "menuItem displayFont", "Reset") { doReset() })
        menu.append(createButton("menuShare", "menuItem displayFont", "Copy link") { copyShareLink() })
        menu.append(createButton("menuSave", "menuItem displayFont", "Save") { saveGame() })
        // Gameplay + visual settings (sectioned; gameplay knobs persist to GameplayPrefs + show in TUNING LAB).
        MenuControls.settings(menu)
        // Build version footer (timestamp + git-sha), so any deployed build is identifiable.
        val version = document.createElement("div") as HTMLDivElement
        version.addClass("menuVersion")
        version.textContent = BuildInfo.LABEL
        menu.append(version)
        val button = createButton("menuButton", "topButton displayFont", "MENU") {
            menu.classList.toggle("invisible")
        }
        span.append(button)
        span.append(menu)
        return span
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
        hint.textContent =
            "“$currentLocationName” is only ${(World.walkability * 100).toInt()}% walkable — mostly water/blocked. Pick another."
        screen.append(hint)
        screen.append(createButton("gateNewGame", "topButton displayFont onboardStart", "Choose another location") { doNewGame() })
        document.body?.append(screen)
    }

    /** New Game: drop all URL params and reload → the onboarding flow runs from scratch. */
    private fun doNewGame() {
        val loc = document.location
        document.location?.href = (loc?.origin ?: "") + (loc?.pathname ?: "/")
    }

    /** Reset: reload onto the current location (deep link) → a fresh world at the same place. */
    private fun doReset() {
        navigateToLocation(currentLng, currentLat, currentLocationName)
    }

    private fun navigateToLocation(lng: Double, lat: Double, name: String) {
        document.location?.href = GameUrl.forNavigation(lng, lat, name)
    }

    /** Copy the shareable link (location + size + seed + tuning) to the clipboard, with brief feedback. */
    private fun copyShareLink() {
        val clipboard = window.navigator.asDynamic().clipboard
        if (clipboard != null) clipboard.writeText(GameUrl.forShare(currentLng, currentLat, currentLocationName))
        (document.getElementById("menuShare") as? HTMLButtonElement)?.let { btn ->
            btn.innerText = "Copied!"
            window.setTimeout({ btn.innerText = "Copy link" }, 1200)
        }
    }

    /** Save: download a small JSON holding the reproducible share link + a stats snapshot (seed-based save). */
    private fun saveGame() {
        val link = GameUrl.forShare(currentLng, currentLat, currentLocationName)
        val json = "{\"link\":\"$link\",\"tick\":${World.tick}," +
            "\"enlMu\":${World.calcTotalMu(Faction.ENL)},\"resMu\":${World.calcTotalMu(Faction.RES)}}"
        val a = document.createElement("a") as HTMLAnchorElement
        a.href = "data:application/json;charset=utf-8," + encodeURIComponent(json)
        a.download = "qgress-save.json"
        a.click()
    }
}
