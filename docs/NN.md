# NN.md — custom neural-net faction driver (Phase 6, Track A)

> **Status: stub / prep.** Design notes for the custom-network AI driver. The work itself is
> scoped in [../PLAN.md](../PLAN.md) → *Phase 6 (Track A: custom net + neuroevolution)*; flesh
> this file out when 6.2 starts. The shared substrate (policy API, headless harness) is 6.0/6.1.

## The idea (the original "Q-gress")

Each faction is driven by a small purpose-built network whose **output layer _is_ the 18
behaviour sliders** (11 `QActions` + 7 `QDestinations`, each 0..1). Input is a normalized
`Observation` of world state; output is a `SliderVector`, re-evaluated at a slow cadence
(≈ once per checkpoint). The slider vector stays the action substrate — the net does **not**
replace per-agent `ActionSelector`.

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
