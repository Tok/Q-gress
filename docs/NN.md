# NN.md — custom neural-net faction driver

> `ai/net/` provides `Net` / `NetArch` / `NetPolicy` / `Evolution` (all tested + deterministic); a baked
> champion is bundled and beats the baseline. You can **train one live** in the **TRAIN** tab and watch any
> net **think** in the **BRAINS** tab. Possible refinements (fitness shaping, self-play league) are in the
> roadmap.

## The idea (the original "Q-gress")

Each faction is driven by a small purpose-built network whose **output layer _is_ the 17 behaviour
sliders** (10 `QActions` + 7 `QDestinations`, each 0..1). Input is a normalized `Observation` of world
state; output is a `SliderVector`, re-evaluated at a slow cadence (**once per scoring checkpoint**). The
slider vector stays the action substrate — the net does **not** replace per-agent `ActionSelector`.

## What's built (`ai/net/`)

- **`Net`** — a small MLP: `Observation.SIZE` (13) inputs → the hidden layers → `SliderVector.SIZE` (17)
  outputs (sigmoid → 0..1). All weights in one flat genome array; pure deterministic forward pass, with a
  `forwardTraced` variant that returns per-layer activations for the visualizer.
- **`NetArch`** — the architecture: any number/width of hidden layers + bias + hidden `activation`
  (default **two hidden layers of 16**, tanh). The trainer offers two width dropdowns (**4 / 8 / 16 / 24 /
  32** each → 4×4 … 32×32). `genomeSize()` / `layerOffset()` make the flat genome arch-aware.
- **`NetPolicy`** — wraps a `Net` as a `FactionPolicy`: live `Observation` → `SliderVector`, cached and
  re-evaluated **once per checkpoint** (installing it costs no more per-action than a slider read).
- **`Evolution`** — a resumable `(μ+λ)` GA (elitism + gaussian mutation; the env is non-differentiable and
  the reward episodic, so ES, not gradient RL). Fitness = mean **summed per-checkpoint MU margin** (own −
  foe) over K seeded `SimRunner` matches. Its RNG is independent of `Util` (which `SimRunner` reseeds per
  match) → fully deterministic given (grid, seed, config). `Evolution.Session.stepGenome()` scores one
  genome at a time so a UI can show live progress; `train()` loops it.
- **`GenomeIO` / `NetStore` / `Champion`** — JSON encode/decode of genome + arch + fitness; persistence in
  `localStorage` (survives reload); a baked default champion as the bundled net.

## Where to see it

- **TRAIN tab** (`util/ui/TrainerPanel`) — pick population / generations / mutation / per-layer widths /
  activation, hit **Train**: one genome per `setTimeout` (UI never blocks), a fitness curve that climbs,
  a champion genome preview. **Save** to `NetStore`, **Install** as a faction driver, or **Download / Load**
  the champion JSON to share. A second section runs a **leaderboard** (round-robin over the live grid). The
  whole run is bracketed by `HeadlessRun` (snapshot + tick-pause + silenced FX) so the live game is untouched.
- **BRAINS tab** (`util/ui/BrainsPanel` + `util/ui/NetVizPanel`) — per faction: the **live activation
  diagram** (layers as node columns, edges lit by contribution, the top output's "chosen path") above the
  **genome heatmap**, plus arch / fitness / driving-input / peak-hidden / favoured actions.

## Mechanism (decided)

- **Neuroevolution / ES**, not tabular Q-learning or gradient RL: non-differentiable env, sparse/episodic
  reward (MU margin), continuous action space → fast headless match → fitness number, embarrassingly
  parallel. The "Q" in Q-gress is heritage branding; the mechanism is honest ES.

## Open questions / next
- ES variant (CMA-ES vs the current GA), a self-play **league + Hall-of-Fame** (the baked champion is the
  current opponent).
- **Fitness shaping**: pure MU-margin can rediscover the recruit-rush degenerate — reward *interesting*
  play (lead changes / field layering) instead.
- A **library of trained nets per `NetArch`** (so the onboarding per-arch pick is meaningful).
- Real line-coverage once the functional core moves to `commonMain` + a `jvm()` test target.

The `Observation` schema is shared with the LLM driver — see [LLM.md](LLM.md). See
[ARCHITECTURE.md](ARCHITECTURE.md) for where the slider seam (`ActionSelector.q`) lives.
