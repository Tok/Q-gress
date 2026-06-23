# FEATURES.md — what's shipped

A record of completed work, so [PLAN.md](../PLAN.md) can stay future-only. Grouped by area,
newest themes roughly last. Commit hashes are illustrative pointers, not exhaustive.

## Toolchain & build (Phase 1)
- Migrated the dead 2018 build to **Kotlin 2.4 → JS (IR)**, **Gradle 9.5**, build JVM **JDK 21**
  (detekt can't run on JDK 25). Sources under `src/jsMain` / `src/jsTest`.
- Replaced removed/relocated APIs (`kotlin.browser.*`/`kotlin.dom.*` → `kotlinx-browser`,
  `toUpperCase`→`uppercase`, `Double.toByte()`, literal `js()`, null-safety).
- Existing tests compile + green in **Node** (Mocha). Karma/Chrome wiring kept (disabled).
- **ktlint + detekt** (complexity limits, baselined) wired into Gradle; **pre-commit hook**
  (`.githooks/`, `core.hooksPath`) enforces the **full gate** — ktlintFormat (auto-fix + restage)
  then compile + ktlintCheck + detekt + jsNodeTest. Gradle config cache on.

## Maps & the zoom bug (Phase 2)
- Dropped the dead rawgit OpenLayers include; **Mapbox GL → MapLibre GL 5.24**
  (`external/MapLibre.kt`), no token. Keyless tiles: OpenFreeMap (street/vector) + Esri imagery.
- Authored open styles: positron street, satellite (+ openmaptiles 3D buildings), and the
  **black-bg / white-streets shadow mask** for the passability grid.
- Fixed grid/zoom reliability: `preserveDrawingBuffer`, wait for `idle` before `readPixels`,
  select the shadow canvas by container query. Fixed `[0,0]` center sentinel + empty-collection
  `max/min` crashes. Web assets → `src/jsMain/resources`; webpack bundle serves `index.html`
  (`start.sh`); retired `published/*.js`.

## Locations & navigation (Phases 3–4, 3D nav)
- **"Play your hometown"**: free-form geocoding via keyless **Nominatim/OSM** → `?lng=&lat=&name=`
  recenter, works on any host.
- Navigation handed to **MapLibre's own handlers** (left-drag pan, right-drag rotate+tilt, wheel
  zoom, unrestricted; NavigationControl block). Optional WASD / Q-E / R-F keys (`Navigation`).
- **Mini-globe inset** (`util/ui/MiniMap`) — circular synced overview with a FLAT/GLOBE toggle.
- Presets are labelled **"Name, City, Country"**; POI labels follow the same form. The catalogue is
  **externalized to `resources/locations.json`** (edit freely — no Kotlin build needed), loaded at
  startup into the `Locations` registry (`config/Location`: a `Location` data class + a pure, unit-tested
  parser; only `RED_SQUARE` stays in code as the sync default + fetch-failure fallback). Each entry has a
  `title` flag marking it eligible as a title-screen fallback. This is also the seam for future scenario
  sharing.
- **Home button** — recenter top-down over the play area (pitch 0) to find the action again.
- **Shareable links** — URLs carry faction + lng/lat/name + size (w/h) + portal/NPC counts + round +
  quickstart + **seed** + both factions' **tuning sliders** (`GameUrl`, which owns all URL read/build);
  Menu **"Copy link"** copies a link reproducing the exact world, and **"Save"** downloads a small JSON
  (that link + a stats snapshot) — a seed-based save. Backed by a **seedable mulberry32 RNG**
  (`Util.random`, the sole randomness source) so the same seed → the same world (rosters included; the
  live map grid is the one non-seed input).

## 3D rendering & the glass visual overhaul
- **Foundation**: three.js custom layer camera-synced to the MapLibre matrix (`system/display/
  Scene3D`, `external/Three`). The simulation stays 2D; only rendering went 3D.
- **Abstract-glass apparatus** look (à la qlippostasis), grayscale vessel + faction tint only:
  - `GlassShader` (Fresnel rim + emission + vnoise smudges, double-shell thick walls,
    camera-tracking rim via `updateEye`).
  - Portals = **metal pole + rubber gasket + glass orb** (φ-scaled by level L1→L8), **grow-on-spawn**
    + **level-up tween**. **Selection** keeps the faction hue and just lights the orb brighter
    (`GlassShader.SELECT_BRIGHT`) — no more neutral-looking white tint. A faction-neutral **name + level
    billboard** floats above each portal's orb (cached `CanvasTexture` sprite, Chakra Petch).
  - **Resonators** — 8 colour-coded rods in rubber slot-rings, real-time from `resoMap()`, grow
    with the pole, **fall on shatter** (cannon-es physics rods), **hack spin + top-jointed
    centrifuge** (`HackFx`); shatter physics in `ShatterFx` (pole sinks, shards + resos **arc up-and-out
    from the XMP blast**, then fall). The blast push comes from the shared **`BlastModel`** — one
    **3D mushroom-cloud-centre origin** (above the terrain, rising with level) + **distance falloff**,
    energy ∝ level / distance — the *same* model the title wordmark uses, so normal-play shatters and
    the title letters react with one unified physics.
  - **Links → a single glowing pipe** (`linkGeo`/`orientTube`, `Materials.linkPipe` — one solid
    self-illuminated rod, replacing the old glass-shell + plasma-core "3-in-1") with bright ball-joints.
  - **Fields → plasma** sheets (`PlasmaShader`, animated; fill-in + dissolve + collapse sound).
  - **XMP** — volumetric raymarched **mini-nuke** (`XmpShaders`/`XmpBurst`), detonates at the agent.
    The field morphs from an initial fireball into a **rising mushroom** — a torus cap (the rising
    donut / vortex ring) that climbs + spreads, plus a stem — carved into pyroclastic billows by
    rotation-decorrelated fbm displacement (no smooth-sphere look, no grid streaks). The hot core stays
    emissive while cool smoke is gradient-lit + translucent (warm, not a black blob); it starts tiny and
    fast-expands, then **dissolves gradually** (cooling colour + thinning alpha — a smoke tail). Plus a
    3D **torus** ground shockwave ring (reads right on the 3D terrain).
  - **Stray XM** rendered as glowing additive motes (`Materials.xmGlow`).
  - **Lifecycle registry** `Spawns` (per-entity first-seen, survives the sync rebuild) drives
    spawn/teardown animations; `FieldFx` dissolves.
  - **Portal-defense bolts** (`BoltFx`) — a tesla-coil **fractal-lightning** bolt + flash light arcs
    from an enemy portal's orb to the agent that hacks/attacks it (ported from the title; see "portal
    zap" under gameplay).
  - **Agent XM gauge** — a vertical bar above each agent's action coin; its height scales with the
    agent's **XM capacity** (level) and a faction-coloured fill (black = drained) shows current XM.
    Resonators **deploy out of the bar** (rise straight up, then arc into the slot — `DeployFx`).
  - **Detailing**: round **ball-joints** at each link end (`Materials.linkNode`), a **top o-ring** on
    each resonator (stays with the pole on shatter), **mod solids** as **translucent, self-luminous
    energy shapes** (`Materials.modSolid` — not the old chrome) with a **bold double-wire glowing cage**
    (`Materials.modWire`, `EdgesGeometry`) in a **slowly tumbling tetrahedron**; shatter blast energy
    scales with **XMP level**.
- **Buildings occlude the sim by default** (the 3D pass shares the map depth buffer); a Menu
  accessibility toggle **"Show through buildings"** restores the always-on-top draw (XMP/explosions stay
  on top regardless). Action coins are occluded by portals but not buildings in that mode.
- **Real portal names** from map data (`util/PortalNames`: POI/street query on the shadow source).
- **GLB compaction** (`95dda03`, Blender): `shattered_flask.glb` 2.43 MB → **657 KB** (strip unused
  UVs/materials, weld verts, smooth normals; all 12 shatter variants preserved). Editable source at
  `assets/blender/shattered_flask.blend`.
- **Demo sandbox** (`/#demo`): place/select/remove portals, Upgrade/Downgrade/Link/Hack/Glyph,
  grow-in animations, gray backdrop toggle. **XMP** fires at the selected portal via the X1–8 buttons,
  or — with the **"Fire XMP on click"** toggle on — detonates at the map click point (chosen X-level).

## UI / HUD (canvas → DOM)
- The **entire HUD is DOM** and themeable. `StatsPanel` (MU "covered area" bars + time/tick +
  action **LOG**), **history dashboard** `HistoryPanel` (per-metric uPlot sparklines — MU + Portals
  + Links + Fields + Agents over time, with live ENL/RES values; `Checkpoint` snapshots all of them),
  `TopAgentsPanel` leaderboard, `Inspector` (selected entity), `Controls` legend + desktop-only gate.
- **Unified tabbed dock** (`util/ui/Dock`): the scattered corner panes are consolidated into one
  right-docked panel with **NOW / HISTORY / TUNE** tabs (one view at a time) — NOW holds the
  scoreboard + leaderboard + LOG, HISTORY the sparklines, TUNE the sliders. It's the only
  pointer-interactive HUD surface (rest stays click-through for map gestures). The `Inspector` stays a
  separate contextual panel (top-left, clear of the dock), unified to the same glass frame.
- **Merged tuning panel** (`util/ui/TuningPanel`): the two slider panes (Actions + Destinations) are
  now one flat list (Actions, a divider, then Destinations). The DOM `<input>` per (q-value × faction)
  is still the value store `ActionSelector.q` reads. A **read-only mode** (`?readonly=true` or the
  Menu "Lock tuning" toggle) swaps the sliders for 0–1 progress bars that mirror the values
  (`refresh()` re-syncs them) — the hook for future agent-vs-agent matches where the player can't tune.
  Action icons render from the **hi-res** (supersampled) canvas + shown small via CSS, so they're crisp.
- **Portals are discovered, not placed**: manual portal **placement** and **deletion** are removed
  from the real game (the map click only selects/deselects; the Inspector has no Remove button). The
  `/#demo` sandbox keeps LMB-place / RMB-shatter for showcasing.
- **3D terrain (DEM heights)**: the map is no longer flat — a keyless **terrarium DEM** source (AWS
  open Terrain Tiles) drives MapLibre `setTerrain`, so the satellite drapes over real elevation. Game
  objects **sit on the terrain**: `Scene3D` samples a coarse elevation grid (`groundZ`, bilinear, via
  `simPosToLngLat` + `queryTerrainElevation`) and offsets every ground-anchored z (portals, agents,
  NPCs, stray XM, links/fields, labels, the deploy/loot/pickup FX). Menu **"3D terrain"** toggle
  (default on); degrades to flat if the DEM is unavailable. (The cannon-es shatter ground stays flat —
  a known approximation.)
- **Top toolbar** reorganized: Menu far-left (with overlay toggles + Lock-tuning inside it), Home, and a
  seamless **sim-speed segmented control** — Pause / ×1 / ×3 / Max butted together (active speed
  highlighted; Pause is Space-bound; `-`/`+` still nudge) replacing the old pause button + slider. Far
  right: **Volume** then the **Auto cam** toggle, both **icon-only** (`util/ui/Icons`) so they don't read
  as one label. Toolbar stays hidden until the world is ready.
- **UI theme — glass, chrome & lasers, faction-branded.** One `--faction` CSS variable (set from the
  chosen faction in `LoadingOverlay.setAccent`) is the **only** chrome accent — a RES player never sees
  ENL green in the UI (and vice versa); it drives the loading bars/title, `accent-color` on all native
  controls (checkboxes, the buildings slider), tuning-slider thumbs, and the button hover glow. Buttons
  are **brushed-chrome** with a subtle hover lift + faction "laser" glow + press-in (it's a game); the
  always-on panels (menu, footer, loading, history, popup) are **black-rubber glass** (blur + faint
  faction-laser edge). Volume/Speed sliders stay deliberately grayscale.
- **Auto cam** (icon toggle, rightmost; **on by default**): a slow, slightly-randomized cinematic camera
  drift around the arena — the title-screen orbit reused in-game (`MapUtil.setAutoCam`/`autoCamLeg`), but
  much slower (~2.6× the title leg) and framing the whole arena (may pull a touch wider or push a little
  closer, but holds the picture; the title can push in for detail). **Wall-clock** (chained
  `setTimeout`/`easeTo`) → same pace at any sim speed. A manual **pan/rotate/tilt snaps it back out**
  (the toggle de-highlights via `onAutoCamChanged`); **zoom is exempt** (zooming while it drifts is fine).
- **Keyboard controls + sim speed** (`util/Shortcuts`, `util/ui/ShortcutsHelp`): Space pause, Home
  recenter, PageUp/Down (and `-`/`+`) zoom, WASD pan, `,`/`.` building transparency, `-`/`+` sim
  **speed**, Tab cycles the footer, M mute, Esc closes popups; a **"?" shortcuts help** popup lists
  them. The sim **speed buttons/keys** drive `Time` tick interval **and** `Scene3D.animationSpeed`, so
  every animation (hack spin, deploy/shatter, build-in, field shimmer) tracks the sim speed. (The
  **Auto cam** drift is the deliberate exception — wall-clock, decoupled from sim speed.)
- Map visuals: grayscale-terrain default (the satellite layer starts desaturated in-style → no colour
  flash; eases to colour) + Colored/Street views (`LayerView`), an **atmospheric skybox** (MapLibre
  `setSky`) above the horizon, white play-area border + upright **semi-transparent white boundary
  walls** (closed top/bottom rim rings) + dimmed out-of-bounds (`PlayAreaMask`), so the arena reads as a
  physical box. **Round play field by default** (bigger inscribed-circle arena on a square map, circular
  flow vectors). **Buildings inflate from the ground** during world build. Rule: faction colours for
  faction things only.
- **Dead 2D canvas layer removed** — `mainCanvas`/`uiCanvas` + the `#canvasLayer` div gone (world is
  the 3D layer, HUD is DOM); `bgCan` kept only as a detached `ImageData` factory for the grid.
- **Font**: shareware Amarillo USAF → **Chakra Petch** (SIL OFL 1.1, self-hosted), techno/sci-fi-HUD
  to fit the science-vs-luddite theme. Coda for text.

## Gameplay, balance & map playability
- **Balance (Phase 5)**: recruiting now **costs XM** + has **diminishing returns** near the cap, so
  recruit-rush is no longer a free snowball. Fixed a recruiting CME (`World.pendingAgents` buffer).
- **Plausible agent handles** (`util/NameGen`): Ingress-style names from themed word banks (per-faction
  flavour + adjectives/nouns/titles), CamelCase/snake/dot styling, light leet, numeric + `xX_…_Xx`
  wraps, and a location token — deduped per game. Replaces the old gibberish generator.
- **Map playability**: `GridConnectivity` carves corridors so no area is sealed off — and the gameplay
  carver (`connectIslands(grid, w, h)`) now also joins **on-screen** regions to each other directly, so
  two areas that both touch the off-screen ring no longer connect only via a long map-edge detour (the
  cause of agents wandering/looking stuck). `World.walkability` gate blocks mostly-water maps;
  `LocationTest` guards all presets; per-terrain movement penalties (landcover-graded shadow style →
  `PathUtil` flow magnitude) + Terrain overlay.
- **Stuck/loop recovery** (`StuckTracker`, always on): an agent/NPC whose **net displacement stays
  under one deployment range over a full sample window** (frozen against geometry, or caught in a
  vector-field spiral / off-screen-detour loop) is flagged, then un-stuck on the ~minute checkpoint
  cadence — **escalating**: first a temporary **bee-line** straight at the target (ignoring the looping
  field, `Agent.recoverIfStuck` / `NonFaction`), and if that doesn't free it (e.g. wedged on the
  play-area edge) a **re-target** to a fresh portal / new destination. (`?debug` adds a 3D marker +
  HUD count over the flagged entities.)
- **`?debug` diagnostics** (off by default, `util.Debug`, disabled in Node): grid-build connectivity
  self-check log (islands / on-screen islands / walkability, warns when unhealthy);
  **`?debug=capture`** sweeps every preset and downloads a `GridFixture` snapshot file, which
  `PresetConnectivityTest` audits offline in Node (single component + on-screen-connected + walkable).
- **Non-blocking flow fields** (`PathUtil.computeFieldAsync`): the per-portal heat map is a **bucketed
  Dijkstra** (cost = wave + terrain penalty, one frontier bucket per cost level → each cell touched
  once, O(cells) instead of the old re-scan-everything wavefront) + vector field, both `suspend` and
  yielding (`delay(0)`) every ~2000 cells / between smooth passes, computed on a `MainScope` and written
  back into `Portal.vectors` (and the offscreen field cache) when ready — so creating a portal/field no
  longer freezes the JS thread. All portal creation (world-gen + the in-game Explore action) is async. Agents
  fall back to a straight-line heading while a field is still empty (so they keep moving + re-sample
  the real field once it lands) instead of stalling on `Complex.ZERO`.
- **Link/field integrity** (`LinkFieldIntegrityTest`): fixed `Portal.findLinkableForKeys` no-crossing
  filter; no dangling links/fields on portal destroy/neutralise.
- Removed the dead attack/damage telegraph/queue system (`Attacks`/`Display`/`Damage`/`Queues`);
  damage applies inline, visualised by the 3D XMP burst.
- **Portal zap defense** (`Portal.retaliate`): hacking or attacking an **enemy** portal makes it
  retaliate — XM damage scaling with portal **level + shields** (mitigation) plus a `BoltFx` lightning
  bolt + thunder. Friendly/neutral portals don't zap. This makes XM the agent's **energy**: drained by
  zaps/deploys, refilled from stray XM + power cubes (the Ingress model; researched). Shatter blast
  energy also scales with XMP level.

## Onboarding (Phase 7, partial)
- **Ordered onboarding** faction → map-size → location → load (`util/ui/Onboarding`), in-memory
  (no reloads); `?local=true` auto-starts; deep links load directly.
- **Map size + portal density** presets (Small/Normal/Large, editable W/H + portal count).
- **Staged loading overlay** (`LoadingOverlay`) — map tiles → tracing roads/water/terrain (+ walkable %)
  → place names → grid → deploying agents → spawning people → routes, faction-tinted, translucent at
  build to reveal the spawning world. Fronted by a **fresh 3D extruded Q-GRESS wordmark**
  (`LoadingWordmark` — its own mini three.js renderer, since the title scene is torn down by the
  onboarding reload) that **shrinks in + drifts**, with a faction laser glow; falls back to the DOM
  wordmark text if WebGL/font fails.
- **Title / faction screen is a real `Scene3D` mini-sim** (`util/ui/TitleSim`): a round arena with a
  3-v-3 levelled roster (L3/L5/L8 each side) + ~30 NPCs running the actual tick loop / AI behind the
  menu, fronted by a real **3D extruded Q-GRESS wordmark** (brand font via `FontLoader`/`TextGeometry`,
  camera-locked, letters spring away from XMP blasts), a dramatic fly-in + slow center-facing orbit,
  grayscale→colour fade, and a GitHub footer link. The faction menu fades in ~1s after the letters land.
  Wiped by the onboarding reload (no in-place teardown). Same renderer/FX as the game.
  - **Title location** isn't fixed: it opens on the **player's home** when location was *already*
    shared (silent, no permission prompt — `GeoLocator.homeIfPermitted` via the Permissions API),
    otherwise a **random iconic location** from a curated photogenic subset (`Location.randomTitle`).
    Everything builds live (no precomputed paths). A location that's **unplayable at the small round
    title size** (e.g. a home over open water — a player on a ship) fails the same walkability gate as
    the live game (`GridConnectivity.MIN_WALKABILITY`, checked after the live readback) and **falls back**
    to an iconic location, forcing the known-good default on the final retry.
- **World-build framing** (`MapUtil.startBuildCinematic`): the build camera keeps its 3D tilt but a
  viewport bottom-padding lift (`liftViewToCentre`, stable under the bearing spin) raises the
  play-area centre to mid-screen from the start — so the first portals / flow-field vectors read
  centre-frame instead of sinking to the bottom; cleared on `goHome`.

## Audio
- **True 3D positional audio** (`ba82492`): every positional sound routes through a `PannerNode` at
  its sim-space metre position, with an `AudioListener` driven by the live camera each frame
  (`Scene3D.render` → `SoundUtil.updateListener`; eye from `GlassShader.eye()`, forward/up by
  unprojecting the inverse projection). Distance attenuation + front/back + elevation that track
  rotate/pitch/zoom (HRTF, inverse distance model). Replaced the screen-projected stereo pan.
  Because the listener rides the (far) camera, a large reference distance + gentle rolloff + a **master
  boost through a limiter** (`DynamicsCompressor`) keep the action audible instead of crushed by
  distance attenuation.
- Per-event sounds (portal create/remove, field up/down, shatter, XMP, hack/glyph/deploy/link,
  checkpoint/cycle, marble "tok" NPC drop); non-positional events (checkpoint/cycle/fail) stay
  center-panned. **Volume slider** + master gain; audio resumes on first gesture.
- **Shared 8-note scale** (`SoundUtil.noteFor`, C2 natural-minor octave): the 8 portal/XMP levels map
  to 8 notes with **level 8 = the lowest**, so a portal's level is audible across every level-keyed
  sound — XMP boom + volley blips, hack/glyph whirs (+ glyph chime), and upgrade/downgrade notes — and
  the sim plays in one key.
- **Layered XMP explosion** (`SoundUtil.playXmpExplosion`): on top of the kept synthetic boom + noise
  blast, a detonation snap + chest-punch sub at the note + a long lowpassed rumble tail whose
  brightness and amplitude decay over the fireball's life — so the sound rises and dissolves with the
  mini-nuke mushroom animation.

## Mods, viruses, items & drop rates
- **4 mod slots per portal** (`portal/ModSlot`) holding a generic `Mod` (`items/deployable/Mod`):
  - **Shields** (`ShieldType`) reduce incoming **XMP damage** — `Portal.totalMitigation` (links +
    shields, capped 95%) is applied in `XmpBurster.dealDamage`.
  - **Heat sinks** (`HeatSinkType`, 20/50/70%) cut the portal **hack cooldown** — `Portal.cooldownFactor`
    (rarest full, each subsequent halved) feeds `handleCooldown`.
  - **Link amps** (`LinkAmpType`) are defined + drawable but **inactive** (never drop, no effect).
  - Rarity colours are shared (`items/types/Rarity`: mint / purple / pink).
- **Deploy** is one item per action (`Deployer`): a resonator if one fits, else a mod into a free slot
  — each with a sound (resonator **ding** / mod **clunk**) + animation.
- **Viruses** (`VirusType` ADA / JARVIS): the `Refactorer` action (new `ActionItem.VIRUS` + tuning
  slider) flips an **enemy** portal to the agent's faction (`Portal.refactor` reassigns resonators,
  drops mods) — colour change animates via `CaptureFx`, with a glitchy `playVirusSound`.
- **Items fall on loss**: resonators tumble out as each is destroyed (`removeReso` → `dropResonator`);
  mods tumble out when the portal is neutralized (`destroy` → `dropMods`) or fully shattered
  (`shatterPortal`) — cannon-es physics in `ShatterFx`.
- **Drop rates** are centralized + tunable in `config/DropRates` (single source; future per-game
  override), surfaced in-app via **Menu → Drop rates** and documented (with ~2018 Ingress sources) in
  `docs/MECHANICS.md`. Fixed a long-standing bug where viruses never dropped (integer `1/roll` = 0).
- **3D**: mods render inside the orb at tetrahedron vertices, shaped by type — **dodecahedron**
  (shield) / **pentagonal radiator** (heat sink) / **diagonal cube** (link amp) — rarity-coloured, plus
  a sci-fi **shield bubble** (`ShieldShader`: camera-tracking Fresnel + animated hex lattice + bloom
  tonemap) at φ× the orb when shielded, intensity scaling with mitigation. **Layered**: one bubble per
  deployed shield (up to 4), each shell a touch larger than the last; the shader dims the far hemisphere
  (`gl_FrontFacing`) + adds a body sheen so each bubble reads as a 3D shell, not a flat surface.
- **Keys**: surfaced as counts (leaderboard + inspector); no 3D model yet.

## Settled decisions
- **Modernize Kotlin/JS in place** (not a TS rewrite; not KMP) — keep the ~7k lines of tested logic.
- **MapLibre GL JS** as the map provider (open, keyless).
- **Build on JDK 21**, Kotlin 2.4, Gradle 9.5 (pure tooling choice; ships JS only).
