# NN.md — custom neural-net faction driver (Phase 6, Track A)

> **Status: machinery built (6.2), not yet trainable.** `ai/net/` ships `Net` + `NetPolicy` +
> `Evolution` (all tested + deterministic). The blocker is the *fitness signal*, not the net: headless
> matches currently yield ~0 MU (fields rarely form), so there's nothing to select on — see
> [../PLAN.md](../PLAN.md) → *Phase 6.2* for the options. The shared substrate (policy API, headless
> `SimRunner`) is 6.0/6.1.

## The idea (the original "Q-gress")

Each faction is driven by a small purpose-built network whose **output layer _is_ the 19
behaviour sliders** (12 `QActions` + 7 `QDestinations`, each 0..1). Input is a normalized
`Observation` of world state; output is a `SliderVector`, re-evaluated at a slow cadence
(≈ once per checkpoint). The slider vector stays the action substrate — the net does **not**
replace per-agent `ActionSelector`.

## What's built (`ai/net/`)

- **`Net`** — a tiny fixed-topology MLP: `Observation.SIZE` (13) inputs → one hidden layer (tanh) →
  `SliderVector.SIZE` (19) outputs (sigmoid → 0..1). All weights in one flat genome array; pure
  deterministic forward pass.
- **`NetPolicy`** — wraps a `Net` as a `FactionPolicy`: maps the live `Observation` → a `SliderVector`,
  re-evaluated **once per scoring checkpoint** (not per action). `Evolution.train(...).bestPolicy(faction)`
  builds one from the winning genome.
- **`Evolution`** — a `(μ+λ)` GA (elitism + gaussian mutation; the env is non-differentiable + the reward
  episodic, so ES not gradient RL). Fitness = mean **per-checkpoint MU margin** over K seeded `SimRunner`
  matches. Its RNG is independent of `Util` (which `SimRunner` reseeds per match) → fully deterministic.

## Mechanism (decided)

- **Trainer = neuroevolution / evolution strategies**, not tabular Q-learning or gradient RL:
  the env is non-differentiable, the reward (MU margin) is sparse/episodic, and the action space
  is 18 continuous values — that fits ES + self-play (fast headless match → fitness number;
  embarrassingly parallel). The "Q" in Q-gress is heritage branding; the mechanism is honest.
- Tiny MLP (`ai/net/`), hand-rolled forward pass, weights as a flat array; trained over the
  headless match harness in a Web Worker / Node; best genome serialized to JSON and loaded by a
  `NetPolicy`. **Visualization** (layers/activations + chosen agent paths) is the payoff.

## Open questions (resolve at 6.2)
- Net topology (hidden sizes), ES variant (CMA-ES vs `(μ,λ)` vs GA), self-play league + Hall-of-Fame.
- Exact `Observation` schema (shared with the LLM driver — see [LLM.md](LLM.md)).
- Fitness shaping: pure MU-margin vs rewarding *interesting* play (lead changes / diversity) to
  avoid rediscovering the recruit-rush degenerate.

See [ARCHITECTURE.md](ARCHITECTURE.md) for where the slider seam (`ActionSelector.q`) lives.
