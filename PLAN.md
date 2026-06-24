# PLAN.md — Q-Gress roadmap (what's next)

Branch: `develop` · Owner: @zirteq

Future TODOs only. For *what's already shipped* see [docs/FEATURES.md](docs/FEATURES.md);
for *how the system fits together* see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md); for the
AI-driver design notes see [docs/NN.md](docs/NN.md) + [docs/LLM.md](docs/LLM.md).

## North star

Q-Gress becomes an **AI-vs-AI sandbox**: each faction (ENL/RES) is driven by an agent whose
**output layer _is_ the 19 behaviour sliders** — a custom net and/or an in-browser LLM. Human
can play any side; any two brains can be matched. **Desktop-only**; mobile is blocked. Until
the AI layer lands, the slider sim is the substrate we keep hardening.

## 3D / rendering
- [ ] **Terrain follow-ups** (DEM heights shipped). Terrain-aware **shatter ground** — the cannon-es
  plane is still flat z=0, so shards/pole sink to sea level on high ground; maybe a Menu exaggeration
  slider; resample the height grid if the play area ever moves (ties into the grand-game movable field).
- [ ] **Explosion shader tuning pass (optional).** The new fireball exposes GLSL consts in
  `XmpShaders.VOLUME_FRAG` (`NOISE_FREQ`, `DISPLACE`, `DENSITY_GAIN`, `STEPS`) + the rise/grow curve in
  `XmpBurst.update`. If it reads too dense/sparse or too small once seen live, these are the knobs;
  consider promoting them to uniforms if frequent tuning is wanted.
- [x] **Stage 2 leftover** — richer link + field-up sounds: linking glissandos between the two
  portals' notes (deeper/longer for long links); fielding plays a swelling triad from the three side
  lengths in a register set by the field's area. (`SoundUtil.playLinkingSound` / `playFieldingSound`)
- [ ] **Stage 3 — pathfinding scalability** — the heat map is now a bucketed Dijkstra (O(cells), all
  field gen async via `PathUtil.computeFieldAsync`); still **per-portal full-map**. Remaining: multi-mode
  nav (flow fields near, cheap nav far), coarser `pathResolution` lever, ambient NPCs, field viz.
- [ ] **Stage 4** — humanoid glTF models (ready: people are head-sized spheres at head height),
  pairs with the colony-management attributes (icebox).

## UI
The HUD is shipped: tuning controls left (`util/ui/TuningPanel`, with a read-only 0–1 bar mode via
`?readonly` / Menu "Lock tuning"), history right, scoreboard top, and a full-width tabbed/collapsible
footer (`util/ui/Footer`: EVENT LOG / AGENTS). Remaining:
- [ ] **Stage 2** — **Schematic** base view (reuse `SHADOW_STYLE`) + more toggleable info overlays
  (e.g. movement-penalty heatmap) alongside the existing Terrain/Vectors toggles.
- [ ] **Stage 4** — tuning-slider polish: per-faction presets, and **wire the read-only bar mode to
  the AI driver** (Phase 6.4) so AI-driven sliders animate live — `TuningPanel.refresh()` is the hook;
  it reads the inputs each frame, the driver just needs to write them at checkpoint cadence.
- [ ] **Stage 5 — a proper, polished UI (the end-state goal).** A cohesive visual theme + layout pass
  over the whole HUD / onboarding / menus building on the dock: consistent typography, spacing, panels
  and states; responsive to window size. This is the "real UI" we want in the end.

## Onboarding (Phase 7 leftovers)
- [ ] **Location selection**: Home / nearest city via Geolocation; a curated preset list; Random;
  surface the free-form search on the onboarding screen (it only exists in-game now).
- [ ] Real **per-stage load %** (especially flow-field computation).
- [ ] **Initial roster "roll"** — light flavour, not a gacha loop; ties to the icebox rarity tiers.
- [ ] **`?debug` dev tooling** — _started_: `?debug` now adds a grid connectivity self-check log,
  stuck/loop agent detection (3D marker + HUD count), and `?debug=capture` (preset fixture export).
  _Remaining_: timing measurements + console logging to profile the long loads.
  _Handoff (medium priority):_ run `?debug=capture` once in-browser, drop the downloaded
  `PresetFixtures.kt` into `src/jsTest/kotlin/util/`, and commit — that flips `PresetConnectivityTest`
  from a synthetic-only harness into a real per-preset audit gate (currently ships with an empty
  placeholder). The user will do this capture pass later.

## Gameplay mechanics (planned)
- [ ] **Glyph hacking** — a skill-based alternative to a normal hack: **~3× the rewards**, but it
  **takes longer**, **requires skill**, and has a **chance to fail** (no / reduced reward on a miss).
  The agent's glyph skill (+ maybe portal level) sets the success odds + duration. The stronger
  collar animation (ENL cw / RES ccw, faster/wider/longer) + glassy sound already land
  (`HackFx`/`SoundUtil`); this is the reward/skill/timing model behind it. Lives in
  `Glypher`/`Portal.tryGlyph` + a glyph skill on `agent/Skills`; expose it as a high-risk/high-reward
  QAction the AI weighs (ties into Phase 6 — the net/LLM should learn when glyphing is worth it).
- [ ] **Aim skill (XMP / Ultra Strike accuracy)** — a per-agent skill on `agent/Skills`: a high-aim
  agent detonates **closer to the portal centre** (max damage); a low-aim agent's blasts land **farther
  off-centre**, so XMP/Ultra-Strike damage falls off with that miss distance. Models skill spread
  across the roster + makes Ultra Strike (small radius) reward good aim. Feeds the damage calc + the
  blast VFX origin; another lever for Phase 6 to learn around.
- [ ] **Portal-mod follow-ups** (shields / heat sinks / viruses shipped; link amps inactive). Heat-sink
  **instant cooldown/burnout reset** for the deploying agent on attach; **multi-hack** mod; **activate
  link amps** (range/outbound-link/SBUL); the **Ultra Strike** weapon + targeted mod-stripping honouring
  shield `stickiness`; a **3D key** model; a per-game **drop-rate tuning UI** (`DropRates` is already
  centralized — Menu → Drop rates; `docs/MECHANICS.md`).

## Grand game — multiple locations & a living field (planned, big)
A core-gameplay direction beyond a single static arena:
- [ ] **Movable / expandable play field.** The playable area can **grow** or **shift** over a game
  (the circle/rect isn't fixed at onboarding) — captured territory or objectives push the boundary.
  The grid + flow-field + border + overlays already key off `Sim.fieldRadius()` / `isInPlayArea`, so
  the field is the natural seam to make dynamic (re-mask + re-sample on change).
- [ ] **Multiple linked locations (a campaign / "grand game").** Q-Gress runs at **several real-world
  locations at once**: **one focused sim** the player actually watches at full fidelity, plus the
  **off-site locations simulated in a simplified/abstract form** (aggregate MU/portal counts, cheap
  tick, no 3D) to keep cost bounded. Locations connect (shared roster, cross-site links/objectives).
- [ ] **Roster management across sites.** The player maintains an **agent roster of ~16–32** spread
  over the locations, allocating/moving them between the focused sim and the simplified off-sites —
  a meta layer on top of the per-faction sliders. Ties into Phase 6 (the AI driver should reason at
  both the local-tactical and the roster/strategic level).
  - *Open questions:* what the simplified off-site model is (pure stats vs a coarse grid), how
    travel/relocation between sites works (time/cost), and how cross-site links/fields score.

## Title / faction screen
The **CHOOSE YOUR FACTION** screen is a showpiece: a **real 3D extruded Q-GRESS wordmark** (brand
font, camera-locked, springs away from XMP blasts) + a compact ENL/RES menu that fades in ~1s after the
letters land, over a **real `Scene3D` mini-sim** (`util/ui/TitleSim`) — a round arena with ~8 portals, a
3-v-3 agent roster (one L3/L5/L8 each side, equipped: level-matched XMPs, resos/cubes, keys, an L8
shield) and ~30 NPCs, driven by the actual tick loop / AI, with a dramatic fly-in + a slow center-facing
orbiting camera, 3D terrain, colour fade, and a GitHub footer link. Wiped by the onboarding reload
(HtmlUtil's reload handoff). It runs the same renderer/FX as the game (no parallel code). Remaining:
- [ ] **Precompute the title world to cut load time.** Serialize the fixed title location's **grid +
  portal positions + flow fields** (extend the `GridCapture` fixture pattern) and load them instead of
  doing the live shadow-readback + async field compute — so the title sim is instant. Pairs with the
  6.1 grid fixtures.
- [ ] **FreeCamera flight path** (optional): fly the camera *position* over the terrain while
  `lookAtPoint(centre)` keeps the action framed (today the cam orbits a fixed centre — MapLibre's
  default camera can't decouple position from look-at without FreeCamera).
- [ ] Drifting **particles** + a generative **ambient** bed (the portal-defense thunderbolts + the
  wordmark's XMP-blast reaction already land — the title inherits the game FX).

## Phase 6 — AI-vs-AI (the Q-gress payoff)

**Decided:** build **both** AI drivers on **one** shared substrate, so any faction can be Human /
Net / LLM independently and fight in any combination. Track design lives in [docs/NN.md](docs/NN.md)
(custom net + neuroevolution) and [docs/LLM.md](docs/LLM.md) (in-browser LLM). The slider vector
stays the action substrate (the net/LLM only re-tunes the 19 sliders at checkpoint cadence — it does
**not** replace per-agent `ActionSelector`).

**Fitness objective (what we optimize for):** each faction maximizes its **Mind Units at every
checkpoint** (and overall) — MU is total controlled **field area**, so the goal is a *team effort to
create and maintain the largest fields*: use the available portals/space to **layer** fields so total
area is maximized, and hold it across the cycle. So fitness = the **sum/average of checkpoint MU**
(rewarding sustained large fields), not just final MU — `Cycle` already snapshots per-checkpoint MU.

**6.0 — Substrate I: programmatic policy API + determinism** _(prereq, no AI yet)_ — **DONE** (`ai/`)
- [x] **`FactionPolicy`** (`ai/FactionPolicy.kt`) — `ActionSelector.q()` now reads the faction's installed
  policy × the QValue weight; the registry `FactionPolicies` defaults each faction to `DomSliderPolicy`
  (the tuning sliders, or `0.1` headless) → zero gameplay change. A driver calls `FactionPolicies.set`.
- [x] **`Observation` (pure)** (`ai/Observation.kt`) — `observe(faction): DoubleArray`, a fixed 13-slot
  normalized vector (cycle fraction, MU/link/field share, portal control, roster fill, avg level + XM per
  side). Read-only over `World`; deterministic. The NN/LLM input.
- [x] **`SliderVector` (pure)** (`ai/SliderVector.kt`) — **19** named slots (12 `QActions` + 7
  `QDestinations`, stable `ORDER`) ↔ encode/decode + clamp; `SliderVectorPolicy` wraps one as a
  `FactionPolicy`. (PLAN earlier said "18" — actual count is 19.)
- Seedable RNG is **done** — `Util.random()` is a seedable mulberry32; same seed reproduces a world
  (powers shareable "Copy link" + the 6.1 match harness).
- Also made the Q-value system **headless-safe** (`QActions` skips UI icons, `DomSliderPolicy` skips the
  DOM read when there's no `window`) so the substrate inits in Node — a prereq for the 6.1 SimRunner.

**6.1 — Substrate II: headless match harness** _(the training engine; pays off the functional-core split)_
- _Groundwork done:_ the **audio** + Q-value systems are headless-safe (`SoundUtil` self-guards via a
  lazy graph + `isMuted()`; `QActions`/`DomSliderPolicy` skip DOM/icons headless) — SimRunner needs that.
- _Functional-core split, Stage 1 **DONE** — the effect-sink seam (`system/effect/`):_ the crash-prone
  visual effects logic fired inline (XMP burst, hack/deploy/reward FX, retaliation bolt, portal shatter,
  reso drop, flow-field flash) now route through `Fx.sink` (an installable `Effects` interface, mirroring
  `FactionPolicies`). `BrowserEffects` forwards 1:1; `NoOpEffects` (headless default) is the no-op, so the
  tick loop runs in Node without touching three.js (was: `Portal.remove()` → unguarded `Scene3D.shatterPortal`
  forced a lazy geometry → crash). `EffectsSeamTest` proves it. Zero browser-visible change.
- _Functional-core split, Stage 2 **DONE** — sync pathfinding (`PathUtil.computeFieldSync`):_ a non-suspend
  twin of `computeFieldAsync` (same bucketed-Dijkstra heat map + vector field + smoothing, shared pure
  helpers → deterministic) that returns the field inline, no coroutine / no frame-yield. Headless field
  compute is opt-in via `Config.headlessFieldCompute` (default off → unit tests unchanged, agents bee-line);
  when on, `Portal.create` + `NonFaction.getOrCreateVectorField` compute fields synchronously. `PathUtilSyncTest`
  covers it. Fields are computed once per portal / per unique offscreen destination (cached), not per tick,
  so the earlier "too slow" worry was really the async coroutine never running in a sync loop — now moot.
- _Spike finding (resolved by Stages 1–2):_ the original `SimRunner` spike died because the sim wasn't
  synchronous/headless (renderer crashes + async field gen). Both are now fixed. Remaining for Stage 3:
- [ ] **`SimRunner`** — `runMatch(gridFixture, policyEnl, policyRes, seed, maxTicks): MatchResult`, a
  synchronous tick loop (set `Config.headlessFieldCompute = true`, seed RNG, build the grid from a fixture,
  drive `World` ticks) capturing **per-checkpoint MU** (the fitness signal), with effects on `NoOpEffects`.
  Watch-outs from the spike: `NonFaction.findNearestTo` throws on an empty roster; seed + reset
  `FactionPolicies`/`Fx`/`World` between matches.
- [ ] **Grid fixtures** — serialize a built `Grid` (+ portal seeds) to committed JSON so matches
  reproduce without live tiles / `readPixels`. (The synthetic open grid worked in the spike; real-tile
  fixtures still need the `?debug=capture` pass.) `GridFixture` already does the RLE serialization.
- [x] **Sync, fast pathfinding for headless** — `PathUtil.computeFieldSync` (Stage 2 above). If it ever
  proves too slow on large real-tile grids, the levers are multi-mode nav (flow near, cheap far) / a
  coarser `pathResolution`; fields are cached per portal/destination so it's not per-tick.

**6.2 — Track A: custom net + neuroevolution** → [docs/NN.md](docs/NN.md)
- [ ] Tiny MLP (`ai/net/`, output = 19 sliders); ES/self-play trainer; `NetPolicy` (JSON genome);
  live **activation + path visualization**. Exit: beats the default-slider baseline over K seeded
  matches, loads into the live game, activations visualized.

**6.3 — Track B: in-browser LLM driver** → [docs/LLM.md](docs/LLM.md)
- [ ] `LlmPolicy` at checkpoint cadence (state → prompt → JSON slider vector, schema-validated,
  defensive fallback); transformers.js and/or Gemma/MediaPipe behind one interface; reasoning panel.
  Exit: an LLM drives a faction end-to-end in-browser, sim stays smooth.

**6.4 — Mix, match & human-vs-AI**
- [ ] Per-faction driver selection (Human / Net / LLM) in onboarding + the Stage-4 slider panel →
  any combination; AI-driven sliders animate read-only. Tournament/eval view (round-robin → leaderboard).

**Cross-cutting:** desktop-only + **WebGPU** gating; seeds via `?seed=`; **balance risk** — pure
win-maximizing self-play may rediscover the recruit-rush degenerate (below), so keep tuning `Config`
and consider shaping fitness for *interesting* play (follow-up, not v1).

## Open decisions
- **Coverage tooling.** Kover has no Kotlin/JS support. Real line-coverage arrives with the
  **functional-core split** (extract pure logic into `commonMain` + a `jvm()` test target, run Kover
  there). Until then the gates are ktlint + detekt + tests. (6.0/6.1 are the natural place to start
  the split.)
- **Tighten max line length 140 → 120.** Line length is now enforced at **140** (`.editorconfig`).
  Dropping to **120** is deferred: ktlint auto-wraps offenders into more physical lines, which inflates
  detekt's `LargeClass` count — so it needs to land alongside the class extractions below (`Scene3D`,
  and any others near the 600-line cap like `HtmlUtil`/`MapUtil`). Do it as a dedicated refactor pass.

## Balance note (recruit-rush, root cause)
`ActionSelector` picks by weighted-random over `slider × weight`. Recruiting adds agents (up to a
cap), and more agents = more actions/tick = a compounding snowball, so rushing recruitment beats
balanced play. Phase 5 gave recruiting an **XM cost + diminishing returns**; deeper "no strategy
dominates" validation is iterative (playtest, or a future headless strategy-comparison harness — see
6.1). Self-play fitness shaping is the AI-era lever.

## Under consideration (icebox)
- **Weather simulation (gameplay + atmosphere).** Simulate weather at the location — **rain**, fog,
  snow, day/night — as both a visual layer (particles / sky + fog tint, building on the new MapLibre
  sky) and a **gameplay modifier**: e.g. rain makes agent **resolve/battery deplete quicker**, fog cuts
  hack/attack range, snow slows movement. Could be random per-game, seasonal, or pulled from a real
  weather API for the chosen location. Pairs with the colony-management battery/accu idea below.
  - **A directional sun** (with the weather/time-of-day): a real key light so the chrome poles cast a
    highlight + the terrain gets shading (today there's only ambient + a faint fixed sun → the chrome
    reflects a static gradient env, not the scene). Together with a **render-to-cubemap / PMREM** of the
    sky+terrain, the chrome/glass would reflect the *actual* skybox + terrain (currently approximated by
    a static gradient env map in `Materials`). Sun direction drives time-of-day + shadow mood.
- **Colony-management / roster (gameplay expansion).** Per-entity attributes (endurance/speed/agility/
  radius, building on `agent/Skills`+`AgentSize`); **rarity-tiered agents** (randomised attributes but
  **no gambling UX** — the player *manages* composition, not a gacha loop); **items** (skateboards,
  **jet-skis** for water traversal → makes marina/bridge presets playable, power-banks, second phones);
  **battery/accu %** (depleted phone → the player leaves the scene). Pairs with the 3D humanoid work.
- **Null-safety hardening.** Audit every `!!` and replace with `?.`/`?:`/`requireNotNull`/early return
  (same NPE/`NoSuchElement` hazard class as the empty-collection `max/min` crashes already fixed).
- **Rework the movement / pathfinding model.** Derive walkability/penalties from the **vector-tile road
  geometry** (query features / GeoJSON) and/or a graph/navmesh, instead of reading rasterized shadow
  pixels. Decouples the sim from the screen and unblocks dynamic zoom — natural partner of the
  functional-core split + the 6.1 grid fixtures.
- **Going 3D (gameplay).** A pitched/3D camera breaks the top-down screen→grid mapping; needs a
  decoupled simulation grid or a 3D pathfinding model. Revisit after the functional-core split. (3D
  *buildings* in the top-down satellite view already work.)
- **Walkable roofs / more from our own building meshes.** Now that we mesh every building ourselves
  (`OwnBuildings`, fed by OSM/Overpass via `BuildingTiles`, streamed by `BuildingStream` — see
  FEATURES), the door is open for agents to path over roofs, per-building destruction, etc. The
  pbf/`@mapbox/vector-tile` decoder still in the tree (`external/VectorTile.kt`) can also pull the
  **road/water/landcover** layers for the movement-model rework below (real geometry instead of
  rasterized shadow pixels).
- **Building perf + lifecycle (follow-ups from today's Overpass switch).**
  - **Many shadow-casters.** Each building is its own mesh (needed for per-mesh shake). A dense city +
    streaming can reach 1000s of shadow-casting meshes; if FPS suffers, **merge** static buildings into
    a few `BufferGeometry` batches (keep only the play-area ones individually shakeable).
  - **Reset wiring.** `OwnBuildings.clear()` + `BuildingStream.reset()` exist but aren't called on
    world-regen yet, so an old city's meshes/coverage can linger — wire them into the reset path.
  - **Overpass politeness.** A long fly-around fires repeated Overpass queries; add a per-bbox
    **response cache** (and/or a fallback mirror) so we don't hammer the public instance / hit limits.
- **Building-cast shadow polish.** Our meshes cast + receive sun shadows now; revisit shadow-map
  resolution / cascade once the sun obscurables (clouds) land so crowded blocks read cleanly. The sun's
  ortho shadow camera is sized to the play area — **streamed** buildings beyond it won't cast/receive
  until that's widened or made to follow the view.
- **TTS announcements (low priority).** Speak important events (captures, recruits, new fields, cycle
  changes) via the Web Speech API (`speechSynthesis`), throttled so it doesn't spam; per-faction
  voices a nice touch; off by default, behind a toggle + the master volume.
- **Extract the demo/showcase subsystem from `Scene3D`.** Scene3D keeps hitting detekt `LargeClass`
  (currently suppressed); the bulk of the remaining size is the self-contained sandbox code (`Showcase`
  / `DemoLink`, place/click/remove/hack/xmp/step/link/update showcase, demo cursor). Move it to a
  `Showcases` object (Scene3D build helpers go `internal`), then drop the `LargeClass` suppress.
- **Modern Ingress (post-2018) — optional/future.** We're keeping the ~2018 scope, but a 2026 vs 2018
  gap review surfaced a few worth considering, most-aligned first:
  - **Machina (the red AI "third faction").** Added post-2018; the closest real-Ingress analogue to our
    AI-vs-AI north star — a rules-driven faction that spawns on a cycle, auto-links/auto-resonates,
    decays, and is easy to clear, giving ENL/RES something neutral-hostile to react to. (Complexity L.)
  - **Checkpoint / septicycle scoring** — sample MU at fixed checkpoints and average per cycle (not
    just live MU); a real objective for the brain. Pure logic, deterministic-testable. (S/M.)
  - **Ultra Strike + flip cards (ADA/JARVIS)**; the **hacking-economy triad** (heat-sink / multi-hack /
    Portal Fracker, + ITO transmuter) shaping farm rate + item mix; **drones** (remote hack). Skip the
    real-world/social bits (media, fireworks, kinetic capsules, battle beacons, CORE sub, Apex).
- **Evaluate NVIDIA Komodo.** (Per user.) Investigate what it offers and whether it fits Q-Gress
  (rendering / AI / acceleration?) before committing — scope unknown; an evaluation item.
- **Legacy 2D gameplay TODOs (from the old README), still open:** smarter agent behaviour (more
  destinations, **swarming**); an inventory/capacity limit; **ultra-striking** (multi-XMP combo); more
  items (XM-tanks, quantum capsules — overlaps the colony-management items above); an FPS / perf
  readout (pairs with `?debug`); unit tests for fielding + deploying. (Shielding is tracked under
  portal mods / 3D Stage 2; the NN is Phase 6.)

## Constraints / agreements
- Commit to `develop`; **no pushing** until something works end-to-end.
- Keep `CLAUDE.md` + this file + `docs/` current as work lands; no overlapping info between them.
- Desktop-only; do not invest in mobile support.
