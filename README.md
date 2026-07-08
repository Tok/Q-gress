<p align="center">
  <a href="https://tok.github.io/Q-gress/"><img src="assets/images/banner-readme.png" alt="Q-Gress" width="100%"></a>
</p>

# Q-Gress

[![CI](https://github.com/Tok/Q-gress/actions/workflows/ci.yml/badge.svg)](https://github.com/Tok/Q-gress/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Tok/Q-gress/branch/main/graph/badge.svg)](https://codecov.io/gh/Tok/Q-gress)
[![desktop only](https://img.shields.io/badge/platform-desktop%20only-informational)](#desktop-only)
[![Kotlin/JS](https://img.shields.io/badge/Kotlin%2FJS-2.4-7F52FF)](https://kotlinlang.org/docs/js-overview.html)

A browser-based **simulacrum of the mobile game *Ingress*** (modelled on its **~2018-era** mechanics —
resonators, links, fields, XM, bursters, mods), rebuilt in **3D**. Two factions — **ENL** ("frogs",
green) and **RES** ("smurfs", blue) — capture portals, link them, and create control fields over a
**real-world map**. It's a *simulacrum, not a playable map game*: each faction's behaviour is a set of
**sliders**, and an **AI driver's output _is_ those sliders** — so you pick each side's brain and watch
**AI vs AI** play itself out (or drive a side by hand). **No real Ingress/portal data is used** —
everything is generated. (Modern additions like drones / Machina are out of scope for now.)

<p align="center">
  <a href="https://tok.github.io/Q-gress/"><img alt="Play Q-Gress live" src="https://img.shields.io/badge/PLAY%20Q--GRESS%20LIVE-2ea043?style=for-the-badge&labelColor=2ea043"></a>
</p>

The longer-term direction (the project is named for **Q-learning**) is in [`PLAN.md`](PLAN.md).

## Desktop only

This is **desktop-only by design** — it needs WebGL pixel readback, heavy three.js rendering, and
(eventually) in-browser WebGPU inference. Touch-only / mobile devices are detected and shown a notice
instead. Use **Chrome / Brave / Edge** on a desktop with a mouse.

## Highlights

- **3D world** rendered with **three.js** as a **MapLibre** custom layer over a real map (satellite +
  3D buildings); the simulation itself stays 2D.
- An **abstract-glass** look: portals are metal-pole + glass-orb vessels with resonator rods, links
  are glass pipes, control fields are plasma sheets — grayscale vessels, faction colour as the only
  tint. Physics **shatter**, volumetric **XMP** fireballs, **hack** centrifuge animations.
- Real **portal names** from map POI/street data; per-terrain movement; **3D positional audio**.
- A DOM HUD: a live MU "covered area" scoreboard, a per-metric **history dashboard**, and a collapsible
  tabbed footer — sortable **AGENTS** + **PORTALS** tables, the **EVENT LOG**, and the AI tabs below.
  A bottom-left **"?"** opens the one controls + keyboard-shortcuts reference.
- **AI vs AI — pick each side's brain.** Every faction's 17 behaviour sliders can be driven by:
  - a **custom neural net** — a configurable MLP (two hidden layers, each **4 / 8 / 16 / 24 / 32** wide)
    trained **headless by neuroevolution**, with a **bundled champion per architecture**
    ([`resources/champions/`](https://github.com/Tok/Q-gress/tree/main/src/jsMain/resources/champions), one
    JSON each). Train/compare your own in the **Train NN** screen (opened from the menu / the BRAINS tab) —
    it evolves a challenger **against that arch's current champion** and you **save / install / download /
    load** it as JSON to share; the full 25-architecture sweep runs headless via the [batch
    scripts](#build-test--run);
  - an **adaptive heuristic** (presses the attack when behind, consolidates into fields when ahead,
    refuels when low on XM);
  - an **experimental in-browser LLM** (**WebLLM / MLC on WebGPU** — Qwen 2.5, Llama 3.2, Gemma 2,
    SmolLM, picked per faction; it asks the model for a slider vector each checkpoint, falling back to
    the heuristic until it replies). Opt-in (it needs a real WebGPU GPU); the UI auto-swaps to the f32
    model build when the GPU lacks `shader-f16` and shows a WebGPU capability readout + troubleshooting.
  - …or **Manual** — drive your own side with the sliders.

  The **BRAINS** tab is the per-faction window into all of this — your side vs the opponent, each with a
  driver-appropriate card: the net's **live activation diagram + genome heatmap + driving-input/peak-hidden
  readouts**, or the LLM's **model / status / prompt / reply / chosen actions**. Headless match harness,
  deterministic training, an in-game leaderboard, JSON genome save/load — see
  [`docs/NN.md`](docs/NN.md) / [`docs/LLM.md`](docs/LLM.md).
- **Play any location** (geocoded) and **shareable links** that reproduce a world from
  `lng/lat/size/seed` (the RNG is seedable).

See the shatter / XMP / hack effects in isolation in the
**[effects sandbox](https://tok.github.io/Q-gress/#demo)** (`/#demo`).

## Build, test & run

Requires **JDK 21** on `JAVA_HOME` (the build runs on 21 because detekt can't run on 25; the product
ships JS only). The app needs a desktop browser with WebGL.

```bash
./gradlew installGitHooks            # one-time: enable the pre-commit quality gate
./start.sh                           # build + serve + open the app in your browser
./gradlew jsBrowserDevelopmentRun    # alternatively: the webpack dev server
./gradlew jsNodeTest                 # run the unit tests in Node (fast, headless)
./gradlew ktlintCheck detekt         # formatting + lint + complexity gate
./gradlew jsBrowserDistribution      # production bundle → build/dist/js/productionExecutable
```

**Training the AI (headless batch).** The bundled champions — one JSON per architecture under
[`src/jsMain/resources/champions/`](https://github.com/Tok/Q-gress/tree/main/src/jsMain/resources/champions)
— are regenerated by two scripts; the in-app **Train NN** screen is for training/comparing a single
architecture interactively (see [`docs/NN.md`](docs/NN.md)):

```bash
./scripts/bake-champs.sh             # gen 1: bake a champion per arch vs the adaptive heuristic baseline (headless, hours)
./scripts/bake-champs.sh --bench     # just time a match to size the run first
./scripts/train-champs.sh            # gen 2+: tournament fresh challengers vs the current champions (NN-vs-NN)
./scripts/train-champs.sh --overall  # cross-arch ladder report (which champion is strongest overall)
```

## CI / quality

Every push and PR runs a GitHub Actions pipeline ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)):
**JDK 21 → ktlintCheck + detekt (complexity limits) + jsNodeTest**, and on the main branch it builds
the production bundle and **deploys it to GitHub Pages**. A local **pre-commit hook** (`.githooks/`,
installed via `installGitHooks`) runs the same gate so commits land green.

**Line-coverage uploads to [Codecov](https://codecov.io/gh/Tok/Q-gress)** (`koverXmlReport`). It
measures the **pure functional core** (`commonMain`) via the `jvm()` test target — Kover can't
instrument Kotlin/JS, so the browser/WebGL/three.js shell in `jsMain` isn't counted. As more game
logic moves into the testable core (see `PLAN.md`), the covered surface grows.

## Project docs

- [`PLAN.md`](PLAN.md) — roadmap / what's next.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — how the system fits together.
- [`docs/FEATURES.md`](docs/FEATURES.md) — the feature set.
- [`docs/RELEASE.md`](docs/RELEASE.md) — the release / deployment plan (incl. the preserved original 2D version).
- [`docs/NN.md`](docs/NN.md) · [`docs/LLM.md`](docs/LLM.md) — the two AI drivers.
- [`CLAUDE.md`](CLAUDE.md) — how to work in this repo (conventions + standards).

## Third-party / copyright

- *Ingress* and the concept of Ingress are © **Niantic, Inc.** Q-Gress scrapes **no** data from
  Ingress and there is no intention to use real portal data; all agents and portals are generated.
- Map tiles © OpenStreetMap / OpenMapTiles / OpenFreeMap and Esri / Maxar / Earthstar (see the in-app
  attribution).
- Fonts (self-hosted, **SIL Open Font License 1.1** — see `fonts/OFL.txt`): **Chakra Petch** (display /
  wordmark) and **Coda** (body / data).
