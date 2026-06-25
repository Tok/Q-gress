package util

import World
import agent.Agent
import agent.Faction
import agent.StuckTracker
import config.*
import config.Sim
import extension.Canvas
import extension.Ctx
import extension.Grid
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.w3c.dom.*
import org.w3c.dom.events.Event
import portal.Portal
import system.Cycle
import system.Simulation
import system.display.DamageNumberFx
import system.display.PassabilityOverlay
import system.display.Scene3D
import system.display.SunController
import util.data.GeoCoords
import util.data.Line
import util.data.Pos
import util.ui.AudioDemo
import util.ui.Controls
import util.ui.Demo
import util.ui.DropRatesPanel
import util.ui.Footer
import util.ui.Icons
import util.ui.Inspector
import util.ui.LoadingOverlay
import util.ui.MenuControls
import util.ui.Onboarding
import util.ui.ShortcutsHelp
import util.ui.TuningPanel
import util.ui.VolumeControl
import kotlin.js.Json

@Suppress("UnusedParameter") // external JS global; param describes the contract
external fun encodeURIComponent(uri: String): String

object HtmlUtil {
    private var intervalID = 0
    private var coloredMap = true // terrain colour on (eases in after world-gen); Menu toggle flips it
    private const val PAUSE_BUTTON_ID = "pauseButton"
    private const val LOCATION_LABEL_ID = "locationLabel"
    private const val MIN_SPEED = 0.25
    private const val MAX_SPEED = 4.0

    // Sim-speed presets behind the toolbar buttons (mult, label, button id). "Max" = MAX_SPEED.
    private val SPEED_PRESETS = listOf(
        Triple(1.0, "×1", "speedBtnX1"),
        Triple(3.0, "×3", "speedBtnX3"),
        Triple(MAX_SPEED, "Max", "speedBtnMax"),
    )

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
    fun isQuickstart() = Config.quickStart

    private fun tick() {
        if (!World.isReady) return
        Simulation.stepEntities() // shared functional-core step (agents + NPCs + stuck tracker)
        window.requestAnimationFrame {
            DrawUtil.redraw()
            val userFaction = World.userFactionOrThrow()
            val factions = userFaction to userFaction.enemy()
            val enlMu = World.calcTotalMu(Faction.ENL)
            val resMu = World.calcTotalMu(Faction.RES)
            Cycle.updateCheckpoints(World.tick, enlMu, resMu)
            val firstMu = if (factions.first == Faction.ENL) enlMu else resMu
            val secondMu = if (factions.first == Faction.RES) enlMu else resMu
            Scale.setLeading(firstMu >= secondMu) // major key while the player's faction leads, else minor
            DrawUtil.redrawUserInterface(firstMu, secondMu, factions)
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

        // Offscreen ImageData factory for the passability-grid readback (never displayed). No
        // on-screen 2D canvas layer remains — the world renders in the three.js custom layer and
        // the HUD is DOM.
        World.bgCan = createOffscreenCanvas(Dim.width, Dim.height)

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
        leftGroup.append(createButton("homeButton", "topButton displayFont", "Home") { MapUtil.goHome() })
        leftGroup.append(createSpeedControls()) // Pause + ×1/×3/Max (Space toggles pause; -/+ keys nudge speed)
        bindKeyboardShortcuts() // Space=pause · Home=recenter · zoom/pan/speed/mute/Esc

        // Right group, far right: volume, then the Auto cam toggle (rightmost; on by default).
        val rightGroup = document.createElement("div") as HTMLDivElement
        rightGroup.addClass("toolbarGroup")
        rightGroup.append(createVolumeSpan())
        rightGroup.append(createAutoCamToggle())
        // Keep the toggle's highlight in sync — incl. when a manual move snaps the drift (and toggle) out.
        MapUtil.onAutoCamChanged = { on -> syncAutoCamToggle(on) }
        MapUtil.setAutoCam(true)

        // The loaded-location name stretches across the middle (flex-grows between the two groups). The
        // per-faction AI driver pickers ("AI vs AI") sit just left of the right group, up in the header.
        buttonDiv.append(leftGroup)
        buttonDiv.append(createLocationLabel())
        buttonDiv.append(util.ui.DriverControls.toolbarGroup())
        buttonDiv.append(rightGroup)
        controlDiv.append(buttonDiv)

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
        Onboarding.showMapSize(Config.startPortals, onBack = { onboardDrivers() }) { w, h, portals, quick ->
            Sim.setSize(w, h) // size first, so the location screen's play-area box is the real size
            Config.startPortals = portals
            Config.quickStart = quick
            onboardLocation()
        }
    }

    // Reload into the game via the deep-link URL — cleanly wipes the title sim, reusing the reload load path.
    private fun onboardLocation() {
        Onboarding.showLocation(onBack = { onboardMapSize() }) { lng, lat, name -> navigateToLocation(lng, lat, name) }
    }

    private fun centerOrDefault(): Json = GameUrl.lngLat()?.toJson() ?: Locations.DEFAULT.toJSON()

    private fun createCheckbox(id: String, labelText: String, onChange: (Boolean) -> Unit): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        val check = document.createElement("input") as HTMLInputElement
        check.id = id
        check.type = "checkbox"
        check.addClass("checkbox")
        check.onchange = {
            onChange(check.checked)
            null
        }
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label", "topLabel")
        label.innerHTML = labelText
        label.onclick = { check.click() }
        span.append(check)
        span.append(label)
        return span
    }

    private fun createVolumeSpan(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label", "topLabel", "topIcon", "volumeIcon")
        label.id = "soundLabel"
        val slider = document.createElement("input") as HTMLInputElement
        slider.id = "volumeSlider"
        slider.type = "range"
        slider.min = "0.0"
        slider.max = "1.0"
        slider.step = "0.05"
        slider.value = SoundUtil.DEFAULT_VOLUME.toString()
        slider.addClass("slider", "volumeSlider")
        VolumeControl.build(label, slider) // speaker icon (click = mute) + slider, shared with the title screen
        span.append(label)
        span.append(slider)
        return span
    }

    // Four compact buttons replace the old pause button + speed slider: Pause (toggles, Space-bound) and
    // the ×1 / ×3 / Max presets (active one highlighted; -/+ keys still nudge).
    private fun createSpeedControls(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        span.addClass("toolbarGroup", "speedControls")
        span.append(createButton(PAUSE_BUTTON_ID, "topButton displayFont speedBtn", "Pause") { togglePause() })
        SPEED_PRESETS.forEach { (mult, label, id) ->
            span.append(createButton(id, "topButton displayFont speedBtn", label) { selectSpeed(mult) })
        }
        refreshSpeedButtons()
        return span
    }

    /** The Auto cam toggle button (icon-only, rightmost). Snaps out when a manual move cancels the drift. */
    private fun createAutoCamToggle(): HTMLButtonElement {
        val btn = createButton("autoCamToggle", "topButton displayFont autoCamBtn", "") { MapUtil.setAutoCam(!MapUtil.isAutoCamOn()) }
        btn.innerHTML = Icons.CAM
        btn.title = "Auto cam"
        return btn
    }

    /** Pick a sim-speed preset; resumes first if currently paused (so a speed button always plays). */
    private fun selectSpeed(mult: Double) {
        if (intervalID == -1) togglePause() // -1 == paused
        setSpeed(mult)
    }

    // Highlight the preset matching the live speed (none while paused, i.e. intervalID == -1).
    private fun refreshSpeedButtons() {
        SPEED_PRESETS.forEach { (mult, _, id) ->
            val active = intervalID != -1 && mult == speedMult
            (document.getElementById(id) as? HTMLElement)?.let { if (active) it.addClass("active") else it.removeClass("active") }
        }
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
        MapUtil.loadMaps(Locations.DEFAULT.toJSON(), demo = true, callback = fun(grid: Grid) {
            World.grid = grid
            World.isReady = true
            Navigation.setup()
            MapUtil.enable3D()
            // Sandbox map interaction: LMB places a portal on empty ground (a ground ring previews
            // place vs select) or selects the one under the cursor; RMB shatters the nearest. The
            // effects (Hack / Fire XMP) are panel buttons that act on the selected portal.
            MapUtil.bindPortalDemo(
                fun(event: dynamic) {
                    val pos = MapUtil.eventToSimPos(event) ?: return
                    SoundUtil.enableAudio()
                    if (Demo.xmpOnClick()) {
                        Scene3D.playXmpBurst(pos, Demo.xmpLevel()) // detonate at the click point (plays the XMP sound)
                        return
                    }
                    if (Scene3D.clickShowcase(pos, Demo.portalLevel(), Demo.portalColorValue())) {
                        SoundUtil.playPortalCreationSound(pos)
                    }
                },
                fun(event: dynamic) {
                    val pos = MapUtil.eventToSimPos(event) ?: return
                    SoundUtil.enableAudio()
                    Scene3D.removeShowcaseNear(pos)
                    SoundUtil.playGlassShatterSound(pos, 0.4, 0.9)
                },
                fun(event: dynamic) {
                    Scene3D.updateDemoCursor(MapUtil.eventToSimPos(event))
                },
            )
            intervalID = document.defaultView?.setInterval({ demoTick() }, Time.minTickInterval) ?: 0
            Demo.showControls(center)
        })
    }

    private fun demoTick() {
        if (World.isReady) window.requestAnimationFrame { DrawUtil.redraw() }
    }

    private fun initWorld(center: Json) {
        // Size + seed from a shared link (if present) → reproduce the exact world; else a fresh seed
        // (captured for sharing). Set before generation, since Util.random is the sole RNG source.
        GameUrl.size()?.let { Sim.setSize(it.first, it.second) }
        // Apply the rest of the onboarding settings if present in the URL (deep link / reload handoff).
        GameUrl.portals()?.let { Config.startPortals = it }
        GameUrl.npcMultiplier()?.let { Config.npcMultiplier = it }
        GameUrl.quickstart()?.let { Config.quickStart = it }
        GameUrl.round()?.let { Sim.roundField = it }
        Util.seed(GameUrl.seed() ?: Util.freshSeed())
        Onboarding.close() // dismiss the onboarding screen (it loads without a reload)
        // Staged loading overlay, up before the first tile request (the world build runs ~2 min on Big).
        LoadingOverlay.show()
        MapUtil.loadMaps(center, callback = onMapload())
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
        SoundUtil.enableAudio() // first user gesture → resume audio (autoplay policy)
        closePopup()
        if (World.userFaction != null) {
            console.warn("Faction ${World.userFaction} was already chosen.")
            return
        }
        World.userFaction = fact
        LoadingOverlay.setAccent(fact.color) // tint the loading screen with the chosen faction
    }

    private fun resetInterval() {
        intervalID = document.defaultView?.setInterval({ tick() }, currentTickMs()) ?: 0
    }

    private fun togglePause() {
        intervalID = pauseHandler(intervalID) { tick() }
        refreshSpeedButtons()
    }

    private fun bindKeyboardShortcuts() = Shortcuts.bind(
        Shortcuts.Handlers(
            command = { onShortcutCommand(it) },
            zoom = { MapUtil.zoomBy(it) },
            pan = { dx, dy -> MapUtil.panBy(dx, dy) },
            buildingOpacity = { MapUtil.nudgeBuildingOpacity(it) },
            speedDelta = { setSpeed(speedMult + it) },
        ),
    )

    private fun syncAutoCamToggle(on: Boolean) =
        (document.getElementById("autoCamToggle") as? HTMLElement)?.let { if (on) it.addClass("active") else it.removeClass("active") }

    private fun onShortcutCommand(c: Shortcuts.Command) = when (c) {
        Shortcuts.Command.PAUSE -> togglePause()
        Shortcuts.Command.HOME -> MapUtil.goHome()
        Shortcuts.Command.CYCLE_TAB -> Footer.cycleTab()
        Shortcuts.Command.MUTE -> toggleMuteUi()
        Shortcuts.Command.AUTO_CAM -> MapUtil.setAutoCam(!MapUtil.isAutoCamOn()) // C — highlight syncs via onAutoCamChanged
        Shortcuts.Command.CLOSE -> escClose()
    }

    private fun toggleMuteUi() {
        // Reuse the speaker icon's own click handler (toggles mute + syncs slider + swaps the glyph) so the
        // M-key and the icon stay in lock-step; fall back to a bare toggle if the toolbar isn't built yet.
        val icon = document.getElementById("soundLabel") as? HTMLElement
        if (icon != null) icon.click() else SoundUtil.toggleMute()
    }

    private fun escClose() {
        if (Onboarding.isShowing()) { // mid-onboarding, Esc steps back a screen instead of closing panels
            Onboarding.back()
            return
        }
        ShortcutsHelp.close()
        DropRatesPanel.close()
        closePopup()
    }

    private var speedMult = 1.0
    private fun currentTickMs() = (Time.minTickInterval / speedMult).toInt().coerceAtLeast(1)

    /** Set the sim speed multiplier; restarts the tick interval (paused stays paused) and scales
     *  animations. Walking/actions follow automatically — they run per tick, which now ticks faster. */
    private fun setSpeed(mult: Double) {
        speedMult = mult.coerceIn(MIN_SPEED, MAX_SPEED)
        Scene3D.animationSpeed = speedMult // visual FX (hack spin, deploy, shatter, build-in) track the speed
        if (intervalID != -1) {
            document.defaultView?.clearInterval(intervalID)
            intervalID = document.defaultView?.setInterval({ tick() }, currentTickMs()) ?: 0
        }
        refreshSpeedButtons()
    }

    private fun pauseHandler(intervalID: Int, tickFunction: () -> Unit): Int {
        val pauseButton = document.getElementById(PAUSE_BUTTON_ID) as HTMLButtonElement
        return if (intervalID != -1) {
            pauseButton.innerText = "Resume"
            document.defaultView?.clearInterval(intervalID)
            -1
        } else {
            pauseButton.innerText = "Pause"
            document.defaultView?.setInterval({ tickFunction() }, currentTickMs()) ?: 0
        }
    }

    fun isBlockedByMapbox(pos: Pos) = isInMapboxArea(pos) || isInOsmArea(pos)

    private fun isInMapboxArea(pos: Pos): Boolean {
        val area = Line(Pos(-20, Dim.height - 40), Pos(90, Dim.height))
        return pos.x > area.from.x &&
            pos.x <= area.to.x &&
            pos.y > area.from.y &&
            pos.y <= area.to.y
    }

    private fun isInOsmArea(pos: Pos): Boolean {
        val w = Dim.width
        val area = Line(Pos(w - 280, Dim.height - 30), Pos(w, Dim.height))
        return pos.x > area.from.x &&
            pos.x <= area.to.x &&
            pos.y > area.from.y &&
            pos.y <= area.to.y
    }

    private fun onMapClick(event: dynamic) {
        SoundUtil.enableAudio() // first user gesture → resume audio (autoplay policy)
        // Ground point under the cursor; MapLibre fires "click" only for a click, not after a drag.
        val pos = MapUtil.eventToSimPos(event) ?: return
        // Portals are *discovered*, not placed by the player: a click only selects the portal under
        // the cursor (or deselects). Manual placement/removal lives only in the /#demo sandbox.
        if (pos.hasClosePortalForClick()) {
            Inspector.select("portal:" + pos.findClosestPortal().id)
        } else {
            Inspector.select(null)
        }
    }

    private fun onMapMove(event: dynamic) {
        // Hover affordance only — highlight the portal under the cursor + show its spinning 3D name ticker.
        val pos = MapUtil.eventToSimPos(event)
        if (pos != null && pos.hasClosePortalForClick()) {
            Scene3D.setBuildMarker(pos, "portal")
            Scene3D.setHoveredPortal(pos.findClosestPortal())
        } else {
            Scene3D.setBuildMarker(null, "")
            Scene3D.setHoveredPortal(null)
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

    private fun createOffscreenCanvas(w: Int, h: Int): Canvas {
        val canvas = document.createElement("canvas") as Canvas
        canvas.width = w
        canvas.height = h
        return canvas
    }

    fun preRender(w: Int, h: Int, drawFun: (CanvasRenderingContext2D) -> Unit): Canvas {
        val offscreen = createOffscreenCanvas(w, h)
        val offscreenCtx = getContext2D(offscreen)
        drawFun(offscreenCtx)
        return offscreen
    }

    fun getContext2D(canvas: Canvas): Ctx = canvas.getContext("2d") as Ctx

    private fun createPortals(callback: () -> Unit) {
        val total = Config.startPortals
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
        MapUtil.enable3D()
        // Sample the DEM height grid BEFORE spawning portals (+ schedule retries as tiles stream in), so the
        // flow-field arrows that flash during world-gen sit on the terrain instead of at sea level. Without
        // this the first build often shows the vectors too low (heights weren't ready); a re-sample after the
        // Home view settles (below) keeps them accurate.
        Scene3D.onTerrainChanged()
        MapUtil.startBuildCinematic() // gentle orbit while portals + people spawn
        LoadingOverlay.stage(LoadingOverlay.PCT_WORLD, "Building world…")
        createAgentsAndPortals {
            LoadingOverlay.detail("Computing routes & starting simulation…")
            // Start the game with nothing selected (the vector field flashes itself on new portals).
            Scene3D.selected = null
            if (World.userFaction == null) {
                chooseUserFaction(Faction.random())
            }
            createQSliders(World.userFactionOrThrow())
            resetInterval()
            World.isReady = true
            document.getElementById("top-controls")?.removeClass("invisible") // reveal the toolbar now
            MapUtil.showSatellite() // terrain stays grayscale (set at map-load) until the fade below
            Navigation.setup()
            MapUtil.bindInteractions(::onMapClick, ::onMapMove)
            LoadingOverlay.done()
            MapUtil.stopBuildCinematicAndHome() // settle to the Home view (top-down over the play area)
            if (coloredMap) MapUtil.fadeInColor() else MapUtil.setColored(false) // colour eases in post-build
            Scene3D.onTerrainChanged() // sample the DEM height grid (objects sit on the terrain)
            // Once the Home view has settled (buildings on screen), build our own building meshes +
            // colliders (so debris lands on roofs and the sun casts real building shadows).
            window.setTimeout({ MapUtil.buildBuildingColliders() }, 1600)
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
        span.addClass("topLocation", "displayFont")
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
        menu.append(createButton("menuDropRates", "menuItem displayFont", "Drop rates") { DropRatesPanel.toggle() })
        menu.append(createButton("menuShortcuts", "menuItem displayFont", "Shortcuts") { ShortcutsHelp.show() })
        // Overlay toggle lives in the menu now (no longer always-visible in the top bar). Vectors are
        // no longer toggled — they flash automatically for ~a second when a portal is created.
        // The "passability" map (greyscale walkability the grid is read from) — this is NOT the terrain.
        menu.append(createMenuCheckbox("passabilityToggle", "Passability") { PassabilityOverlay.setVisible(it) })
        // Accessibility: draw the whole sim over buildings so actions are never hidden (default off →
        // the realistic order where buildings can occlude portals/agents).
        val xray = createMenuCheckbox("xrayToggle", "Show through buildings") { Scene3D.drawOverBuildings = it }
        (xray.firstChild as? HTMLInputElement)?.checked = Scene3D.drawOverBuildings
        menu.append(xray)
        // Damage-number animations on/off (on by default).
        val dmgNums = createMenuCheckbox("damageNumbersToggle", "Damage numbers") { DamageNumberFx.enabled = it }
        (dmgNums.firstChild as? HTMLInputElement)?.checked = DamageNumberFx.enabled
        menu.append(dmgNums)
        // The single combat-dynamics knob (0 = realistic/tanky, slow board … 1 = portals flip very easily).
        // Drives shield mitigation + weapon drops + attack eagerness + the underdog comeback. Live-tunable.
        menu.append(MenuControls.slider("Combat dynamics", Config.combatDynamism) { Config.combatDynamism = it })
        // Fade the 3D buildings when crowded areas hide the action.
        menu.append(MenuControls.slider("Buildings", 0.9) { MapUtil.setBuildingOpacity(it) })
        // Building-shake intensity (0 = off … 2 = 200%).
        menu.append(
            MenuControls.slider("Building shake", Config.buildingShakeMultiplier, 0.0, 2.0, 0.1) {
                Config.buildingShakeMultiplier = it
            },
        )
        // Build version footer (timestamp + git-sha), so any deployed build is identifiable.
        val version = document.createElement("div") as HTMLDivElement
        version.addClass("menuVersion")
        version.textContent = BuildInfo.LABEL
        menu.append(version)
        val button = createButton("menuButton", "topButton displayFont", "Menu") {
            menu.classList.toggle("invisible")
        }
        span.append(button)
        span.append(menu)
        return span
    }

    /** Toggle a small panel listing the live (tunable) drop rates — transparency for the player. */
    /** A [createCheckbox] styled as a row inside the game menu dropdown. */
    private fun createMenuCheckbox(id: String, labelText: String, onChange: (Boolean) -> Unit): HTMLSpanElement {
        val span = createCheckbox(id, labelText, onChange)
        span.addClass("menuCheck")
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
