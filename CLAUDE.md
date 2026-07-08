# CLAUDE.md

Guidance for Claude Code (and humans) working in this repository. Kept to the point; deeper
docs live under `docs/`.

## What this is

**Q-Gress** is a desktop browser simulation of the mobile game *Ingress*. Two factions —
**ENL** ("frogs", green) and **RES** ("smurfs", blue) — capture portals, link them, and create
control fields over a real-world map. It is a **simulation, not a playable map game**: you tune
AI behaviour with sliders (per faction) and watch the two factions play themselves out. No real
Ingress/portal data is used; everything is generated.

**North star:** AI-vs-AI — each faction driven by a client-side model (custom NN and/or
in-browser LLM) whose output *is* the behaviour sliders. Desktop-only by design (WebGL pixel
reads + heavy 3D + future WebGPU inference); mobile is explicitly out of scope and blocked.

## Where things live

- **`docs/ARCHITECTURE.md`** — how the system fits together (entry point, `World`, the
  selection brain, map→grid pipeline, the 3D + DOM rendering, locations, toolchain).
- **`PLAN.md`** — concrete, standalone next steps (tuning, polish, technical improvements).
- **`docs/FUTURE.md`** — the far-horizon, interconnected vision (the "grand game" across locations + the
  roster/colony layer beneath it). Deliberately kept out of PLAN.
- **`docs/FEATURES.md`** — what's already shipped.
- **`docs/ACTIONS.md`** — the per-agent action machine: the tick loop, how the Q-value sliders pick an
  action, and the idle fallbacks (recruiting / discovery) with their timing + success odds.
- **`docs/MECHANICS.md`** — items / mods / drop rates and the links→fields→MU objective.
- **`docs/NN.md` / `docs/LLM.md`** — design notes for the two AI-driver tracks.

Keep these current as work lands, and **don't duplicate** info across them (architecture →
ARCHITECTURE, shipped → FEATURES, concrete next → PLAN, far-horizon vision → FUTURE, how-to-work → here).

## Tech stack

- **Kotlin 2.4 → JavaScript (IR)**, **Gradle 9.5**, single `js()` target. Browser bundle via
  the Kotlin/JS webpack; **unit tests run in Node** (Mocha).
- **Build JVM: JDK 21** on purpose (detekt's latest can't run on JDK 25; we ship JS, so the
  build JVM is a pure tooling detail).
- **Maps:** MapLibre GL JS (keyless, OpenFreeMap + Esri tiles). **3D:** three.js custom layer +
  cannon-es physics. **Charts:** uPlot. All via npm or CDN `external` declarations.
- JS deps locked in `kotlin-js-store/yarn.lock` (committed). No map token needed (MapLibre, keyless).

## Build, test & dev workflow

```bash
./gradlew installGitHooks            # one-time: enable the pre-commit gate (core.hooksPath)
./gradlew compileKotlinJs            # compile main sources
./gradlew jsNodeTest                 # run unit tests in Node (fast, headless)
./gradlew ktlintFormat               # auto-fix formatting
./gradlew ktlintCheck detekt         # style + complexity gate
./gradlew jsBrowserDevelopmentRun    # webpack dev server (browser)
./gradlew jsBrowserDistribution      # build the browser bundle
./gradlew kotlinUpgradeYarnLock      # refresh yarn.lock after dep changes
./start.sh                           # build + serve + open the app (desktop, needs WebGL)
```

Requires **JDK 21** on `JAVA_HOME`. The app is desktop-only and needs WebGL.

## Conventions

- Kotlin official style (`kotlin.code.style=official`), 4-space indent.
- Game state lives in the `World` singleton; the AI brain is `agent/action/ActionSelector`;
  rendering is 3D (`system/display/Scene3D` + shader/effect modules) with a **DOM HUD**
  (`system/ui/`). There is no 2D game canvas.
- Randomness goes through `Rng` (the seedable mulberry32 PRNG in `commonMain`, not `kotlin.random`);
  seed it for deterministic tests/worlds. The old `Util.random` facade is gone — call `Rng` directly.
- No `!!`; prefer `?:` / `requireNotNull` / `getValue` / early return.
- **Prefer `val` + transforms over reassignment** for *new* code (`val x = if/when/…`, `map`/`fold`/`sumOf`).
  But don't churn existing `var`s into functional form to chase a count: detekt's `VarCouldBeVal` already
  keeps gratuitous `var`s out (the gate is green → every `var` is genuinely reassigned), and the rest are
  legitimate — state-machine flags (`private set`), single-pass min/max accumulators (the functional `minOf`×N
  is *N passes* — slower), and flags riding side-effecting loops. `obj.prop = …` on dynamic/DOM/three.js is the
  platform boundary, not a reassignment.
- **No off-tint grayscales.** UI grays must be neutral (`R == G == B`, e.g. `#a0a0a0`, `rgba(0,0,0,…)`),
  never a greenish/bluish cast (`#9aa6a0`, `rgba(24,28,34,…)`) — unless there's a deliberate reason
  (faction colours, 3D **material** tints where chrome reads cooler on purpose, data-viz encodings, the sky).
  When a tint is intentional, note why in a comment so it isn't "corrected" later.
- Tests mirror the main package layout under `src/jsTest/kotlin/` and must compile to JS.
- Commit raw Blender sources under `assets/blender/` (not just the exported `.glb` in `models/`).

## Engineering standards (enforced)

- **Functional core, imperative shell.** Keep game logic in **pure functions** (state in →
  decision/new-state out); isolate side effects (3D/DOM, MapLibre/WebGL, audio, timers, `World`
  mutation, RNG) at the edges. (Logic and effects are still partly entangled — the split is
  in-progress; see PLAN.)
- **Test everything pure**, deterministically (injected RNG). Aim high on the functional core
  (selection/balance logic near 100%).
- **Commits are gated** by the in-repo pre-commit hook (`.githooks/`, `core.hooksPath`): it runs
  the **full gate** — `ktlintFormat` (auto-fix + restage), then compile + ktlintCheck + detekt
  (complexity limits) + `jsNodeTest`. Don't bypass it; fix the code. Keep commits small and green.

## Working agreements

- Committing directly to **`main`** is fine for now on this project. **Do not push** until something works end-to-end.
- End commit messages with the `Co-Authored-By` trailer.
- The user does the visual checks (`./start.sh`); don't block on screenshots.
