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
- [x] **CRITICAL — non-blocking portal creation (coroutine yielding).** Done. `PathUtil.generateHeatMap`
  + `calculateVectorField` (+ `smooth`) are now `suspend` and `delay(0)`-yield per wavefront layer /
  every ~2000 cells / between smooth passes; `PathUtil.computeFieldAsync(location) { field -> … }`
  runs the pair on a `MainScope()` and calls back. Both call sites return-empty-now + async-fill:
  `Portal.create` (vectors `val`→`var`, `heatMap` passed `emptyMap()`) and
  `NonFaction.getOrCreateVectorField` (dedupe via a `pending` set). Stuck-agent gotcha fixed via
  `MovementUtil.headingTo(from, to)` — agents/NPCs head straight for the destination at unit magnitude
  while a field cell is empty (Agent re-samples `actionPortal.vectors` each tick; NPCs re-snapshot on
  re-target), instead of stalling on `Complex.ZERO`. _Pending: user's live agent-motion visual check._
  _Alternative not taken: a Web Worker_ (true parallelism, but needs grid serialization + a worker build)._
- [x] **Ship to GitHub** — done (live at `tok.github.io/Q-gress/`; 2D at `/2D/`; CI deploys `main`).

## 3D / rendering
- [ ] **Animate the world build (buildings inflate)** — during world creation, make the 3D buildings
  rise out of the ground (lerp their extrusion height / position up, or start vertically flattened and
  "pump" into shape) so the city inflates into place as the world loads, instead of popping in. Pairs
  with the existing staged loading overlay.
- [ ] **Stage 2 leftovers** — **shield visualization** (needs `Portal.deployMods` to actually store
  mods; today a stub); a richer field-up "whoosh".
- [ ] **Stage 3 — pathfinding scalability** — drop the per-portal full-map flow field; multi-mode
  nav (flow fields near, cheap nav far); ambient NPCs; continuous toggleable field viz.
- [ ] **Stage 4** — humanoid glTF models (ready: people are head-sized spheres at head height),
  pairs with the colony-management attributes (icebox).

## UI
- [ ] **Stage 2** — **Schematic** base view (reuse `SHADOW_STYLE`) + more toggleable info overlays
  (e.g. movement-penalty heatmap) alongside the existing Terrain/Vectors toggles.
- [ ] **Stage 4** — tuning-slider panel redesign (both factions, presets); ties into Phase 6.4
  (per-faction driver selection + AI-driven sliders animating read-only).
- [ ] **Stage 5 — a proper, polished UI (the end-state goal).** A cohesive visual theme + layout pass
  over the whole HUD / onboarding / menus (not the incremental panels we have now): consistent
  typography, spacing, panels and states; the tuning-slider panel (Stage 4) folded in; responsive to
  window size. This is the "real UI" we want in the end.

## Onboarding (Phase 7 leftovers)
- [ ] **Location selection**: Home / nearest city via Geolocation; a curated preset list; Random;
  surface the free-form search on the onboarding screen (it only exists in-game now).
- [ ] Real **per-stage load %** (especially flow-field computation).
- [ ] **Initial roster "roll"** — light flavour, not a gacha loop; ties to the icebox rarity tiers.
- [ ] **`?debug` dev tooling** — timing measurements + console logging to profile the long loads.

## Gameplay mechanics (planned)
- [ ] **Glyph hacking** — a skill-based alternative to a normal hack: **~3× the rewards**, but it
  **takes longer**, **requires skill**, and has a **chance to fail** (no / reduced reward on a miss).
  The agent's glyph skill (+ maybe portal level) sets the success odds + duration. The stronger
  collar animation (ENL cw / RES ccw, faster/wider/longer) + glassy sound already land
  (`HackFx`/`SoundUtil`); this is the reward/skill/timing model behind it. Lives in
  `Glypher`/`Portal.tryGlyph` + a glyph skill on `agent/Skills`; expose it as a high-risk/high-reward
  QAction the AI weighs (ties into Phase 6 — the net/LLM should learn when glyphing is worth it).
- [ ] **Portal mods (heat-sinks + others)** — deployable items that slot into a portal and modify it:
  - **Heat-sink** — reduces the portal hack cooldown (Common/Rare/V.Rare ≈ ×0.8/×0.5/×0.3; with
    several, the rarest applies fully and each next is halved) + an instant cooldown/burnout reset for
    the deploying agent on attach. (Confirmed against current Ingress.)
  - **Multi-hack** — more hacks before the cooldown kicks in.
  - **Shield** — defense; the visual is already stubbed (3D Stage 2) but needs `Portal.deployMods` to
    actually store deployed mods (currently a stub — no state to draw).
  - **Force amp / turret / link amp** — combat + range modifiers (later).
  Needs a mod item type + real `Portal.deployMods` storage + a deploy-mod action + mod/shield
  visualization. Pairs with the colony-management items in the icebox.
- [ ] **Portal retaliation ("thunderbolts").** Portals **defend when attacked**: a tesla-coil-style
  **bolt flash** arcs from the portal to the attacker on hit (like Ingress's portal-attack feedback),
  with a **thunderbolt sound**. Model TBD — pure VFX/audio first, then optional retaliation damage
  (higher-level / shielded portals zap harder). The same bolt VFX + sound feed the **title-screen
  demo** (below). **Reference the `qlippostasis` project** for the thunderbolt visuals + sounds (the
  glass-shatter sound was already ported from there).

## Spectacular title / faction screen (planned)
- [ ] The **CHOOSE YOUR FACTION** screen is the first thing a player sees — make it a showpiece:
  the big **Q-GRESS** wordmark (done), centred ENL/RES buttons (done), then a **randomized 3D physics
  demo** behind it: ~3 portals running **non-scripted** random actions — hack/glyph spins, capture
  shatter+regrow, full shatter+respawn (reusing **cannon-es** + `ShatterFx`/`HackFx`), XMP bursts, and
  **thunderbolt** retaliation — plus drifting **particles** and a generative **ambient/thunder sound**
  bed. The 3D part is a **standalone three.js scene** (not the MapLibre custom layer, which loads
  later) — likely shares portal geometry with `Scene3D` via a small extracted builder. Reference
  `qlippostasis` for the title thunderbolts + sound.
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
- [x] **Seedable RNG (done)** — `Util.random()` is now a seedable **mulberry32** (it was the sole
  randomness source), so the same seed reproduces a world. Already powers **shareable links**
  (lng/lat/name + size + seed → the exact world; "Copy link" in the Menu); the AI match harness (6.1)
  builds on it. _Remaining for 6.0: the `FactionPolicy` API + `Observation`/`SliderVector`._

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
