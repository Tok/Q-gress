# PLAN.md — Q-Gress roadmap (what's next)

Branch: `develop` · Owner: @zirteq

Future TODOs only. For *what's already shipped* see [docs/FEATURES.md](docs/FEATURES.md);
for *how the system fits together* see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md); for the
AI-driver design notes see [docs/NN.md](docs/NN.md) + [docs/LLM.md](docs/LLM.md).

## North star

Q-Gress becomes an **AI-vs-AI sandbox**: each faction (ENL/RES) is driven by an agent whose
**output layer _is_ the 18 behaviour sliders** — a custom net and/or an in-browser LLM. Human
can play any side; any two brains can be matched. **Desktop-only**; mobile is blocked. Until
the AI layer lands, the slider sim is the substrate we keep hardening.

## Near-term queue
- [ ] **Re-enable + fix the title-screen portals.** Temporarily **disabled**
  (`TitleScene3D.PORTALS_ENABLED = false`) — the thunderbolts still fire (sky-bolt fallback), but the
  portals' look wasn't right. They're built at the game's proportions × `TITLE_SCALE` with the level
  derived from 8 reso slots (chrome pole + GlassShader orb + reso rods), but the orb/pole sizing, reso
  collar layout, camera framing and lighting need a visual pass before turning the flag back on.

## 3D / rendering
- [ ] **Terrain follow-ups** (DEM heights shipped). Terrain-aware **shatter ground** — the cannon-es
  plane is still flat z=0, so shards/pole sink to sea level on high ground; maybe a Menu exaggeration
  slider; resample the height grid if the play area ever moves (ties into the grand-game movable field).
- [ ] **Explosion shader tuning pass (optional).** The new fireball exposes GLSL consts in
  `XmpShaders.VOLUME_FRAG` (`NOISE_FREQ`, `DISPLACE`, `DENSITY_GAIN`, `STEPS`) + the rise/grow curve in
  `XmpBurst.update`. If it reads too dense/sparse or too small once seen live, these are the knobs;
  consider promoting them to uniforms if frequent tuning is wanted.
- [ ] **Stage 2 leftover** — a richer field-up "whoosh" when a control field forms.
- [ ] **Stage 3 — pathfinding scalability** — drop the per-portal full-map flow field; multi-mode
  nav (flow fields near, cheap nav far); ambient NPCs; continuous toggleable field viz.
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
- [ ] **Portal retaliation ("thunderbolts").** Portals **defend when attacked**: a tesla-coil-style
  **bolt flash** arcs from the portal to the attacker on hit (like Ingress's portal-attack feedback),
  with a **thunderbolt sound**. Model TBD — pure VFX/audio first, then optional retaliation damage
  (higher-level / shielded portals zap harder). The same bolt VFX + sound feed the **title-screen
  demo** (below). **Reference the `qlippostasis` project** for the thunderbolt visuals + sounds (the
  glass-shatter sound was already ported from there).

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
The **CHOOSE YOUR FACTION** screen is a showpiece: the **Q-GRESS** wordmark + compact ENL/RES buttons
over a **real `Scene3D` mini-sim** (`util/ui/TitleSim`) — a round arena with ~8 portals, a 3-v-3 agent
roster (equipped: varied XMPs, resos/cubes, keys, a shield each side) and ~30 NPCs, driven by the
actual tick loop / AI, with a dramatic fly-in + a slow center-facing orbiting camera, 3D terrain, fast
colour fade, and a GitHub footer link. Wiped by the onboarding reload (HtmlUtil's reload handoff). It
runs the same renderer/FX as the game (no parallel code). Remaining:
- [ ] **Precompute the title world to cut load time.** Serialize the fixed title location's **grid +
  portal positions + flow fields** (extend the `GridCapture` fixture pattern) and load them instead of
  doing the live shadow-readback + async field compute — so the title sim is instant. Pairs with the
  6.1 grid fixtures.
- [ ] **FreeCamera flight path** (optional): fly the camera *position* over the terrain while
  `lookAtPoint(centre)` keeps the action framed (today the cam orbits a fixed centre — MapLibre's
  default camera can't decouple position from look-at without FreeCamera).
- [ ] Drifting **particles** + a generative **ambient** bed; **thunderbolt retaliation** lands via the
  game's portal-defense work (below), so the title inherits it for free.

## Phase 6 — AI-vs-AI (the Q-gress payoff)

**Decided:** build **both** AI drivers on **one** shared substrate, so any faction can be Human /
Net / LLM independently and fight in any combination. Track design lives in [docs/NN.md](docs/NN.md)
(custom net + neuroevolution) and [docs/LLM.md](docs/LLM.md) (in-browser LLM). The slider vector
stays the action substrate (the net/LLM only re-tunes the 18 sliders at checkpoint cadence — it does
**not** replace per-agent `ActionSelector`).

**6.0 — Substrate I: programmatic policy API + determinism** _(prereq, no AI yet)_
- [ ] **`FactionPolicy`** — replace the DOM slider read in `ActionSelector.q()` with a per-faction
  policy source; default `DomSliderPolicy` = today's behaviour (zero gameplay change).
- [ ] **`Observation` (pure)** — `observe(world, faction): DoubleArray`, fixed normalized feature
  vector (MU + Δ, per-faction counts, tick fraction, avg agent XM/level, …). The NN/LLM input.
- [ ] **`SliderVector` (pure)** — 18 named slots ↔ `QActions`+`QDestinations`; encode/decode + clamp.
- Seedable RNG is **done** — `Util.random()` is a seedable mulberry32; same seed reproduces a world
  (powers shareable "Copy link" + the 6.1 match harness). Remaining for 6.0: the three items above.

**6.1 — Substrate II: headless match harness** _(the training engine; pays off the functional-core split)_
- [ ] **`SimRunner`** — `runMatch(gridFixture, policyEnl, policyRes, seed, maxTicks): MatchResult`,
  tick loop with rendering/audio/DOM stubbed at the shell boundary.
- [ ] **Grid fixtures** — serialize a built `Grid` (+ portal seeds) to committed JSON so matches
  reproduce without live tiles / `readPixels`.
- [ ] **Speed** — accelerated in Node (CI) and a Web Worker (in-tab training). Exit: a full match
  runs headless & deterministic, hundreds/min.

**6.2 — Track A: custom net + neuroevolution** → [docs/NN.md](docs/NN.md)
- [ ] Tiny MLP (`ai/net/`, output = 18 sliders); ES/self-play trainer; `NetPolicy` (JSON genome);
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
