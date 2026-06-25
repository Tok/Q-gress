# PLAN.md — Q-Gress roadmap (what's next)

Branch: `develop` · Owner: @zirteq

**Future work only.** *Shipped* → [docs/FEATURES.md](docs/FEATURES.md); *how it fits together* →
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md); AI-driver design → [docs/NN.md](docs/NN.md) +
[docs/LLM.md](docs/LLM.md). Completed work lives in the **git log**, not here — keep this file to the point.

## ★ Next session — start here
1. **3D portal names** (high priority) — see *3D / rendering*.
2. **Visual NN trainer** (Phase 6.5) — the full handoff is under *Phase 6*.

## ⚑ Verify in-browser first (`./start.sh`)
Built headless recently, not yet confirmed on screen — eyeball these, then move on:
- **Buildings (parallel mode)** — at e.g. **Red Square**: gaps gone; our meshes **and** MapLibre's gap-fillers
  both bob on an XMP. **Watch for z-fighting / double-look** where the two overlap (both visible now) → if it
  reads badly, do the per-building-replacement refinement (*3D*). `OwnBuildings.PARALLEL_MODE=false` to compare.
- **Hack/glyph centrifuge** — top o-rings tilt out *with* the rods and fall with them on shatter / reso-kill.
- **NET tab** — maximize/collapse compacts it; activation diagram + genome heatmap legible; 16×16 → 4 columns.
- **"Who plays?" onboarding** — grid aligned, selection clearly highlighted, picks take effect in-game.
- **LLM driver** (`WebLlmClient`) — the WebGPU model actually loads + drives a faction (only un-headless bit).

## North star
Q-Gress becomes an **AI-vs-AI sandbox**: each faction (ENL/RES) is driven by an agent whose **output _is_ the
17 behaviour sliders** — a custom net and/or an in-browser LLM. A human can play any side; any two brains can
be matched. **Desktop-only**; mobile is blocked.

## 3D / rendering
- [ ] **★ 3D portal names (high priority).** Render each portal's name as a 3D label in the scene
  (billboarded sprite / text mesh above the orb), not just in the DOM HUD. Names already resolve from map
  POI/street data (`PortalNames`) — this is the presentation: faction-tinted, legible against buildings,
  LOD/cull + fade at distance, and de-cluttered when portals crowd (hide all but the nearest/biggest).
- [ ] **Buildings — per-building replacement** *(the parallel-mode follow-up).* Today both sets render (ours
  on top, MapLibre filling gaps). Cleaner end-state: hide **only** the MapLibre footprints we have our own
  mesh for, so the gap-fillers and our look match and there's no overlap/z-fight. Needs matching our synthetic
  centroid keys to MapLibre feature ids (now that the `openmaptiles` source carries `generateId`) — or a
  custom building layer we fully own.
- [ ] **Terrain-aware shatter ground.** The cannon-es plane is flat z=0, so shards/pole sink to sea level on
  high ground; sample the DEM under the blast (maybe a Menu exaggeration slider); resample if the play area
  moves (ties into the grand-game movable field).
- [ ] **Explosion shader tuning (optional).** GLSL consts in `XmpShaders.VOLUME_FRAG` (`NOISE_FREQ`,
  `DISPLACE`, `DENSITY_GAIN`, `STEPS`) + the rise/grow curve in `XmpBurst.update`; promote to uniforms if the
  fireball needs frequent live tuning.
- [ ] **Pathfinding scalability.** Heat map is a bucketed Dijkstra (O(cells), async via
  `PathUtil.computeFieldAsync`) but still **per-portal full-map**. Want: multi-mode nav (flow fields near,
  cheap nav far), a coarser `pathResolution` lever, ambient NPCs, a field viz.
- [ ] **Humanoid glTF models** — people are head-sized spheres at head height today; swap in real models
  (pairs with the colony-management attributes, icebox).

## UI
- [ ] **Schematic base view** (reuse `SHADOW_STYLE`) + more toggleable info overlays (e.g. a
  movement-penalty heatmap) alongside the existing Terrain/Vectors toggles.
- [ ] **Per-faction tuning presets** — save/recall named slider sets (the slider↔AI auto-move link is done).
- [ ] **Stage 5 — the polished end-state UI.** A cohesive visual-theme + layout pass over the whole
  HUD/onboarding/menus building on the dock: consistent typography, spacing, panels and states; responsive to
  window size. The "real UI" we want to ship behind.

## Onboarding
- [ ] **Map-size pass.** (a) A **warning popup for large maps** — "reduced FPS to be expected" before the
  build, so the player opts in knowingly. (b) **Optimize the three size presets** (Small / Normal / Large) —
  profile + tune each (portal count, NPC density, build time, runtime FPS) so each hits a sane quality/perf
  target. (c) **Drop square maps** if any remain (the round-field option already squares; rectangular presets
  are the keepers) — confirm and remove.
- [ ] **Location selection polish** — Home / nearest city via Geolocation; a curated preset list; Random;
  surface the free-form search on the onboarding screen (it only exists in-game now).
- [ ] **Real per-stage load %** (especially flow-field computation).
- [ ] **Initial roster "roll"** — light flavour, not a gacha loop; ties to the icebox rarity tiers.
- [ ] **`?debug` dev tooling** — has the grid self-check + stuck-agent detection + `?debug=capture`. Remaining:
  load-timing/profiling logs; and the handoff — run `?debug=capture` once in-browser, drop the downloaded
  `PresetFixtures.kt` into `src/jsTest/kotlin/util/` and commit, flipping `PresetConnectivityTest` from a
  synthetic harness into a real per-preset audit gate.

## Gameplay mechanics
- [ ] **Glyph hacking** — a skill-based hack: **~3× rewards**, but **longer**, **needs skill**, **can fail**.
  Glyph skill (+ portal level) sets odds + duration. The collar animation + glassy sound already land
  (`HackFx`/`SoundUtil`); this is the reward/skill/timing model in `Glypher`/`Portal.tryGlyph` + a glyph skill
  on `agent/Skills`, exposed as a high-risk/high-reward QAction the AI learns to weigh.
- [ ] **Recruiting as an agent skill + items.** Rate is self-balancing now (`Recruiter.selectionWeight` ×
  `Balance.recruitFactor`). Next: a per-agent **skill** (`agent/Skills`) some are better at, plus **items**
  (e.g. *beer* — a temporary recruit boost) — a characterful lever instead of a flat faction rate.
- [ ] **Aim skill (XMP / Ultra-Strike accuracy)** — a per-agent skill: high-aim detonates **closer to portal
  centre** (max damage), low-aim lands **off-centre** (damage falls off with miss distance). Makes the
  small-radius Ultra-Strike reward good aim; feeds the damage calc + blast VFX origin; another AI lever.
- [ ] **Portal-mod follow-ups** (shields/heat-sinks/viruses ship; link amps inactive): heat-sink **instant
  cooldown reset** on attach; a **multi-hack** mod; **activate link amps** (range/outbound/SBUL); the
  **Ultra-Strike** weapon + targeted mod-stripping honouring shield `stickiness`; a **3D key** model; a
  per-game **drop-rate tuning UI** (`DropRates` is centralized — Menu → Drop rates; `docs/MECHANICS.md`).

## Grand game — multiple locations & a living field *(big, exploratory)*
- [ ] **Movable / expandable play field** — the playable area can **grow** or **shift** over a game (captured
  territory / objectives push the boundary). Grid + flow-field + border + overlays already key off
  `Sim.fieldRadius()` / `isInPlayArea`, so the field is the seam to make dynamic (re-mask + re-sample on change).
- [ ] **Multiple linked locations (a "grand game")** — run several real-world locations at once: **one focused
  sim** at full fidelity + **off-site locations in a simplified/abstract form** (aggregate MU/portal counts,
  cheap tick, no 3D) to bound cost. Locations connect (shared roster, cross-site links/objectives).
- [ ] **Roster management across sites** — a player **roster of ~16–32** spread over the locations,
  allocated/moved between the focused sim and the off-sites — a meta layer over the sliders (the AI should
  reason at both the local-tactical and roster/strategic level). *Open:* the off-site model (pure stats vs
  coarse grid), travel/relocation cost, and how cross-site links/fields score.

## Title / faction screen
- [ ] **Precompute the title world** — serialize the fixed title location's grid + portal positions + flow
  fields (extend the `GridCapture` fixture pattern) and load them instead of the live shadow-readback + async
  field compute, so the title sim is instant. Pairs with the Phase-6 grid fixtures.
- [ ] **FreeCamera flight path (optional)** — fly the camera *position* over the terrain while
  `lookAtPoint(centre)` keeps the action framed (today it orbits a fixed centre — MapLibre's default camera
  can't decouple position from look-at without FreeCamera).
- [ ] **Drifting particles + a generative ambient bed** (the defense thunderbolts + wordmark XMP-reaction
  already land — the title inherits the game FX).

## Phase 6 — AI-vs-AI (the payoff)
**Substrate is shipped** (see FEATURES + git log): the policy seam (`FactionPolicy`/`FactionPolicies`), the
`Observation` → `SliderVector` contract, the deterministic headless `SimRunner` match harness + `WorldSnapshot`,
the **custom-net track** (`Net`/`NetArch`/`Evolution`/`NetPolicy`, a baked **16×16** champion that beats the
baseline, JSON genome save/load via `GenomeIO`/`NetStore`, the NET activation + genome viz), the adaptive
`HeuristicPolicy`, the **in-browser LLM track** (`LlmPolicy`/`WebLlmClient` + reasoning panel, browser-only),
the `Tournament` eval engine, and per-faction driver selection (top toolbar + the onboarding step).

**Fitness objective:** maximize **summed per-checkpoint MU** (sustained field area), not just final MU — a
team effort to layer fields across the cycle. The net/LLM only re-tunes the 17 sliders at checkpoint cadence;
it does **not** replace per-agent `ActionSelector`.

Remaining:
- [ ] **★ 6.5 — Visual NN trainer** *(next session)* — an in-browser trainer in a new **TRAIN** footer tab:
  pick a `NetArch` (layers/widths/bias/activation) + `EvolutionConfig` (pop/gens/mutation), run `Evolution`
  live with a **fitness curve** + champion preview (reuse the genome heatmap + activation viz), then **save**
  the winner to `NetStore` / install it as a driver. Headless pieces all exist; this is the UI + live-run
  wiring:
  1. **Make `Evolution` resumable** — an `Evolution.Session(grid, seed, config, opponent)` holding the
     population, with `step(): Double` = one generation (returns champion fitness) + `bestGenome`/`bestFitness`/
     `generation`/`done`; refactor `train()` to loop it (existing tests still cover it); add a `SessionTest`.
  2. **Chunk it off the UI** — call `session.step()` per `setTimeout(…, 0)`, yielding between generations; keep
     the in-browser default config **small** (pop ~10–12, short matches) so each step is sub-second (serious
     training stays headless).
  3. **Don't clobber the live game** — wrap the run in `WorldSnapshot.capture()` → train → `restore()`, **pause
     the tick loop** (`HtmlUtil.tick()` early-returns on `!World.isReady`, line 76 — gate it on a `training`
     flag), and install `NoOpEffects` via `Fx` for the duration (no stray 3D FX).
  4. **Champion actions** — show fitness; "Save to `NetStore`"; "Install as ENL/RES driver".
- [ ] **In-game leaderboard UI** — a button wrapping `Tournament` in the **same run harness** as the trainer
  (snapshot + pause + `Fx` no-op), showing the `Standing` table, so net variants (different `NetArch`) can be
  ranked head-to-head. Build right after the trainer (shared plumbing).
- [ ] **Grid fixtures** — serialize a built `Grid` (+ portal seeds) to committed JSON so headless matches
  reproduce without live tiles / `readPixels`. `GridFixture` does the RLE serialization; the real-tile fixtures
  still need the `?debug=capture` pass.
- [ ] **Clean-eval flag** (nice-to-have) — training with the anti-runaway mechanics OFF (`dominanceDecay`/
  `leaderDistraction`/`comebackMax` = 0) gives a slightly cleaner gradient; a flag on `Evolution`/`MatchSetup`.
- [ ] **Per-side net-architecture / variant pick in onboarding** — surface `NetArch` choices up front (feeds
  the trainer + the leaderboard's variant matchups).
- [ ] **icebox — download / upload trained nets.** `GenomeIO` JSON-ables a genome+arch (~16 KB at 16×16): a
  "download champion" (Blob → file) + "load from file/paste", and a small saved-net library — share/version
  nets outside `localStorage`.
- [ ] **icebox — genome/action-set versioning.** Once training is serious AND the action set grows (more
  `QActions`/observation slots), stamp a **schema version** in the genome JSON and refuse/migrate incompatible
  nets (the `inputs`/`outputs` dim check is the seed). Not needed pre-release while the layout churns.

**Cross-cutting AI risk:** pure win-maximizing self-play may rediscover the recruit-rush degenerate (below) —
keep tuning `Config` and consider shaping fitness for *interesting* play (a follow-up lever, not v1).

## Open engineering decisions
- **Coverage tooling.** Kover has no Kotlin/JS support; real line-coverage needs the functional-core split
  (pure logic → `commonMain` + a `jvm()` test target, run Kover there). Until then: ktlint + detekt + tests.
- **Tighten max line length 140 → 120.** Deferred: ktlint auto-wrapping inflates detekt's `LargeClass` count,
  so it must land alongside the class extractions (`Scene3D`, and any near the 600-line cap like
  `HtmlUtil`/`MapUtil`). A dedicated refactor pass.
- **Extract the demo/showcase subsystem from `Scene3D`** — most of Scene3D's `LargeClass` bulk is the
  self-contained sandbox code; move it to a `Showcases` object (build helpers go `internal`), then drop the
  `LargeClass` suppress.
- **Null-safety hardening** — audit remaining `!!` → `?.`/`?:`/`requireNotNull`/early return (same hazard
  class as the empty-collection `max/min` crashes already fixed).

## Balance risk (recruit-rush)
`ActionSelector` picks by weighted-random over `slider × weight`; recruiting adds agents (→ more actions/tick →
a compounding snowball), so a recruit-rush can beat balanced play. Mitigations are in (XM cost + diminishing
returns; `Balance.recruitFactor` self-corrects roster sizes; `Balance.attackBoost` lets the side behind flip
the board; fair shuffled agent order). Deeper "no single strategy dominates" validation stays iterative
(playtest + the headless harness); **self-play fitness shaping** is the AI-era lever.

## Under consideration (icebox)
- **Weather (gameplay + atmosphere).** Rain/fog/snow/day-night as a visual layer **and** a modifier (rain
  drains agent battery faster, fog cuts hack/attack range, snow slows movement) — random/seasonal or from a
  real weather API for the location. Pairs with the colony-management battery idea.
  - **A directional sun** (with time-of-day): a real key light so chrome poles cast highlights + terrain gets
    shading; plus a render-to-cubemap/PMREM of sky+terrain so chrome/glass reflect the *actual* scene (today a
    static gradient env in `Materials`). Sun direction drives shadow mood.
- **Colony-management / roster.** Per-entity attributes (endurance/speed/agility/radius on `agent/Skills` +
  `AgentSize`); **rarity-tiered agents** (randomised attributes, **no gambling UX** — manage composition, not a
  gacha); **items** (skateboards, **jet-skis** → makes marina/bridge presets playable, power-banks, second
  phones); **battery %** (depleted phone → the player leaves). Pairs with the 3D humanoid work.
- **Movement/pathfinding rework.** Derive walkability/penalties from **vector-tile road geometry** (features /
  GeoJSON) and/or a navmesh instead of reading rasterized shadow pixels — decouples the sim from the screen and
  unblocks dynamic zoom + a pitched/3D camera. Natural partner of the functional-core split. (The
  pbf/`@mapbox/vector-tile` decoder in `external/VectorTile.kt` can pull road/water/landcover layers.)
- **Walkable roofs / per-building destruction** — now that we mesh every building (`OwnBuildings`), agents
  could path over roofs, buildings could be destructible, etc.
- **Building perf + lifecycle** — (a) a dense streamed city can reach 1000s of shadow-casting meshes; if FPS
  suffers, **merge** static buildings into a few `BufferGeometry` batches (keep play-area ones individually
  shakeable); (b) wire `OwnBuildings.clear()` + `BuildingStream.reset()` into world-regen (they exist, unused);
  (c) a per-bbox **Overpass response cache** so a long fly-around doesn't hammer the public instance; (d) widen
  / view-follow the sun's ortho shadow camera so **streamed** buildings beyond the play area cast/receive.
- **TTS announcements (low pri)** — speak key events (captures, fields, cycle changes) via `speechSynthesis`,
  throttled, per-faction voices, off by default behind a toggle + master volume.
- **Modern Ingress (post-2018), optional** — most-aligned first: **Machina** (the red AI "third faction" — the
  closest analogue to our AI-vs-AI north star: a rules-driven faction that spawns, auto-links, decays, is easy
  to clear); the **hacking-economy triad** (heat-sink / multi-hack / Fracker + ITO) shaping farm rate; **drones**
  (remote hack). Skip the real-world/social bits.
- **Evaluate NVIDIA Komodo** (per user) — investigate fit (rendering / AI / acceleration?) before committing.
- **Legacy 2D TODOs still open** — smarter agent behaviour (more destinations, **swarming**); an
  inventory/capacity limit; an FPS/perf readout (pairs with `?debug`); unit tests for fielding + deploying.

## Known glitches (low priority)
- **Loading-count fraction glyph.** In world-creation progress, counts like `1/2` and `1/4` render as the
  precomposed ½/¼ glyph (`LoadingOverlay.detail("… ($done/$total)")`). `font-feature-settings: "frac" 0` on
  `.loadingDetail` is already set but **doesn't fix it** — next time try also disabling `dlig`/`clig`/`liga`
  (or `font-variant-ligatures: none`), or break the sequence (a hair-space / ZWNJ between the digit and the
  slash). Cosmetic, non-critical.

## Constraints / agreements
- Commit to `develop`; **no pushing** until something works end-to-end.
- Keep `CLAUDE.md` + this file + `docs/` current as work lands; no overlapping info between them (future →
  here, shipped → FEATURES, how → ARCHITECTURE).
- Desktop-only; do not invest in mobile support.
