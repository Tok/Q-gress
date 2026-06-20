# CLAUDE.md

Guidance for Claude Code (and humans) working in this repository.

## What this is

**Q-Gress** is a browser-based simulation of the mobile game *Ingress*. Two factions —
**ENL** ("frogs", green) and **RES** ("smurfs", blue) — fight to capture portals, link
them, and create control fields over a real-world map. It is a *simulation*, not a
playable map game: the user tunes AI behaviour with sliders and watches the two factions
play themselves out. No real Ingress/portal data is used; everything is generated.

Live (legacy) build: https://tok.github.io/Q-gress/

## The vision (north star)

The endgame is **AI-vs-AI**: each faction is driven by a client-side **Gemma** model
(running in-browser, e.g. via MediaPipe LLM Inference / WebGPU) that tunes the faction's
behaviour sliders. ENL-Gemma vs RES-Gemma, each possibly tuned differently. The name
"Q-Gress" comes from the original idea of Q-learning agents tuning the per-action values.

This is **desktop-only** by design (WebGL pixel reads + heavy canvas work + WebGPU LLM
inference). Mobile is explicitly out of scope and should be blocked with a notice.

## Tech stack (LEGACY — being modernized)

The repo was last touched ~2018 and is frozen on dead tooling. Do not assume it builds.

- **Language:** **Kotlin 2.4.0** compiled to JavaScript (IR).
- **Build:** **Gradle 9.5** with the **`kotlin("multiplatform")`** plugin, single `js()`
  target. The **browser** environment produces the webpack app bundle (`Q-Gress.js`);
  **unit tests run in Node** (Mocha) for speed and to suit the pure-logic core. JS
  dependencies are locked in `kotlin-js-store/yarn.lock` (committed).
- **Build JVM: JDK 21 (LTS), on purpose** — detekt's latest can't run on JDK 25 (see the
  header note in `build.gradle.kts`). This is invisible to the product (we ship JS).
- **Quality gates (enforced):** **ktlint** (format/style, configured in `.editorconfig`)
  and **detekt** (lint + cyclomatic-complexity limits, `config/detekt/detekt.yml` with a
  legacy `baseline.xml`). A **pre-commit hook** (`.githooks/pre-commit`, installed via
  `./gradlew installGitHooks`) blocks commits that fail these.
- **Maps:** still legacy in source — **Mapbox GL JS v0.51.0** + **OpenLayers v5.3.0** in
  `index.html` (OpenLayers via the dead `cdn.rawgit.com`). Phase 2 replaces these with
  **MapLibre GL**.
- **Coverage:** not yet wired — Kover has no Kotlin/JS support; real coverage arrives with
  the functional-core split (see `PLAN.md`).
- The old compiled output under `published/` and the legacy npm/Gradle files under
  `legacy-build/` are retained only for reference; the live build emits to `build/`.

See `PLAN.md` for the modernization roadmap, stack decisions, and engineering standards.

## Build, test & dev workflow

```bash
./gradlew installGitHooks            # one-time: enable the pre-commit gate (core.hooksPath)
./gradlew compileKotlinJs            # compile main sources
./gradlew jsNodeTest                 # run unit tests in Node (fast, headless)
./gradlew ktlintFormat               # auto-fix formatting
./gradlew ktlintCheck detekt         # the quality gate the pre-commit hook runs
./gradlew jsBrowserDevelopmentRun    # run the app via webpack dev server (browser)
./gradlew jsBrowserDistribution      # build the browser bundle -> build/dist/js/...
./gradlew kotlinUpgradeYarnLock      # refresh kotlin-js-store/yarn.lock after dep changes
```

Requires **JDK 21** on `JAVA_HOME` and (for browser tests, currently disabled) Chrome on
`CHROME_BIN`. The app is desktop-only and needs WebGL.

## Architecture

Entry: `Main.kt` → `window.onload` → `HtmlUtil.load()`. The world is a fixed **1200×800
canvas** (`config/Dim.kt`) layered over Mapbox map `<div>`s.

Source layout under `src/jsMain/kotlin/` (tests in `src/jsTest/kotlin/`):

- **`World.kt`** — global mutable game state (agents, portals, grid, tick counter, the
  selected user faction). Singleton `object`.
- **`agent/`** — the AI. `Agent` (faction members), `NonFaction` (recruitable NPCs),
  movement, skills, and:
  - **`agent/action/`** — `ActionSelector` is the brain: every tick each agent picks an
    action by **weighted-random selection** (`Util.select`) over candidate actions.
  - **`agent/action/cond/`** — one object per action (`Recruiter`, `Hacker`, `Linker`,
    `Deployer`, `Attacker`, `Recharger`, `Glypher`, `Explorer`, `Recycler`).
  - **`agent/qvalue/`** — `QActions` and `QDestinations` define the tunable behaviours.
    Each `QValue` has a base `weight`; the **slider value (0..1) × weight** is the
    selection probability. Sliders exist per faction (`...SliderEnl` / `...SliderRes`).
- **`portal/`** — portals, resonators, links, fields, XM, cooldowns, level/quality.
- **`items/`** — bursters, power cubes, resonators, mods, levels.
- **`config/`** — `Config` (game balance constants), `Dim` (geometry), `Location` (the
  hard-coded dropdown of playable locations), `Constants`, colors/styles/time.
- **`system/`** — `Cycle`/`Checkpoint` (scoring over time), `Com` (message log), and
  **`system/display/`** (all canvas + DOM/UI rendering, incl. the sliders/tables).
- **`util/`** — `Util` (random/select helpers), `MapUtil` (Mapbox lifecycle + grid build),
  `HtmlUtil` (DOM/UI construction + the main tick loop), `PathUtil` (vector-field
  pathfinding), `DrawUtil`, geometry under `util/data/`.
- **`external/`** — thin `external` declarations for Mapbox (`MapBox.kt`) and the Web Audio
  API.

### How the map becomes a playfield (important)

`MapUtil` instantiates **three** Mapbox maps over hidden divs: a satellite-ish `initialMap`,
the visible street `map`, and a `shadowMap` styled for pixel reading. After the shadow map
loads, `MapUtil.addGrid()` calls **`gl.readPixels()` on the shadow map's WebGL canvas** and
turns the pixels into a passability **`Grid`** (`extension/Grid.kt`): bright pixels =
walkable streets, dark = impassable buildings. Pathfinding vector fields are computed on
this grid (`PathUtil`).

**Zoom is hard-coded to 18** (`MapUtil.ZOOM/MIN_ZOOM/MAX_ZOOM`) because the grid, the
pixel-to-meter factor (`Dim.pixelToMFactor`), portal sizes, and deployment ranges are all
implicitly calibrated to zoom 18. This is the root of "zoom not working consistently" and
the blocker for dynamic zoom — changing zoom requires rebuilding the grid and rescaling
the geometry.

### Locations

`config/Location.kt` is a fixed `enum` of ~11 places (lng/lat) shown in a dropdown
(`HtmlUtil.createDropdown`). Selecting one reloads the page with `?lng=&lat=&name=` URL
params. There is no free-form / "play my hometown" entry yet (a planned feature — geocoding
+ arbitrary coordinates).

## Conventions

- Kotlin, official code style (`kotlin.code.style=official`). 4-space indent.
- Game state lives in the `World` singleton; rendering is separated under `system/display`.
- Randomness goes through `Util.random()` (wraps JS `Math.random()`), not `kotlin.random`.
  (Being migrated to an **injectable/seedable** RNG so logic is deterministically testable.)
- Tests live in `src/jsTest/kotlin/` mirroring the main package layout; they must compile
  cleanly to JS.
- The Mapbox access token is currently inlined in `index.html` (a public `pk.` token).

## Engineering standards (enforced)

Quality and testability are first-class. See `PLAN.md` → "Engineering standards" for the
full policy. In short:

- **Functional core, imperative shell.** Keep game logic in **pure functions** (state in →
  decision/new-state out, no I/O, no global mutation). Isolate side effects (canvas/DOM,
  Mapbox/WebGL, audio, timers, `World` mutation, RNG) at the edges. Prefer
  `pure(state) → effect` over logic that reaches into the DOM/`World` directly.
- **Test everything pure**, deterministically (injected RNG). Aim for high coverage
  (≥80% on the functional core; selection/balance logic near 100%).
- **Commits are gated** by a pre-commit hook (in-repo via `core.hooksPath`): formatting
  (ktlint), linting + **complexity limits** (detekt), and tests + coverage floor (kover).
  Don't bypass it; fix the code. The same checks run in CI.
- Keep commits small and green.

## Working agreements for this effort

- Branch is **`develop`**. Commit there; **do not push** until something works end-to-end.
- See **`PLAN.md`** for goals, sequencing, and open decisions. Keep it updated as the
  source of truth for the modernization.
