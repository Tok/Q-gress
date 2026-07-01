package ai.net

import agent.Faction
import ai.HeuristicPolicy
import ai.MatchSetup
import ai.SimRunner
import config.Config
import extension.Grid
import system.grid.GridFixture
import kotlin.test.Test

/**
 * Headless champion **bake** harness (the reusable rebake code; driven by `scripts/bake-champs.sh`). Heavy, so
 * it's gated off the normal test run: it only executes when `BAKE_CHAMPS=1` is in the environment (the script
 * sets it, along with `-PmochaTimeout` to lift the 60 s per-test cap). It trains one net per [NetArch] in the
 * TRAIN-tab sweep (two hidden layers, each width in [WIDTHS] → the 25 combos the onboarding per-arch pick
 * offers) against the adaptive [HeuristicPolicy] baseline — a real opponent, not the uniform-slider default the
 * old champion beat — and commits, per arch, the ELITE genome with the best **held-out** (unseen-seed) margin,
 * so a champion that merely overfit its fixed training seeds is never shipped. Each winner is printed as
 * `BAKEGENOME|<label>|<json>` for the script to harvest into [ChampionGenomes].
 */
class ChampionBake {

    private fun grid() = GridFixture("BAKE", ARENA_W, ARENA_H, 2, GridFixture.rleEncode(List(ARENA_W * ARENA_H) { true })).toGrid()

    private fun nowMs(): Double = js("Date.now()").unsafeCast<Double>()

    // A js() argument must be a constant, so read the whole env object in JS and index it (by the Kotlin
    // [name]) here — the script sets BAKE_CHAMPS=1 / BAKE_BENCH=1 to turn a run on.
    private fun envFlag(name: String): Boolean {
        val env: dynamic = js("(typeof process !== 'undefined' && process.env) ? process.env : null")
        return env != null && env[name] == "1"
    }

    private fun archs(): List<NetArch> = WIDTHS.flatMap { h1 -> WIDTHS.map { h2 -> NetArch(listOf(h1, h2)) } }

    @Test
    fun bench() {
        if (!envFlag("BAKE_BENCH")) return
        val grid = grid()
        val straightMs = time(STRAIGHT_SAMPLES) { s ->
            SimRunner.runMatch(grid, s, BAKE_TICKS, MatchSetup(flowFields = false))
            SimRunner.reset()
        }
        val fieldMs = time(FIELD_SAMPLES) { s ->
            SimRunner.runMatch(grid, s, BAKE_TICKS, MatchSetup(flowFields = true))
            SimRunner.reset()
        }
        val perArch = POP * GENS * MATCHES_PER_EVAL
        println("BAKE bench: straight-line ${fmt(straightMs)} ms/match, flow-field ${fmt(fieldMs)} ms/match")
        println("BAKE bench: one arch ~= ${fmt(perArch * straightMs / 1000.0)} s train; ${archs().size} archs")
    }

    @Test
    fun bakeSweep() {
        if (!envFlag("BAKE_CHAMPS")) return
        val grid = grid()
        val archs = archs()
        val start = nowMs()
        archs.forEachIndexed { i, arch ->
            val config = EvolutionConfig(
                populationSize = POP,
                generations = GENS,
                elite = ELITE,
                matchesPerEval = MATCHES_PER_EVAL,
                arch = arch,
                matchTicks = BAKE_TICKS,
                setup = MatchSetup(flowFields = false), // train cheap (straight-line); select on held-out below
            )
            val bake = bakeArch(grid, config)
            val elapsed = fmt((nowMs() - start) / 1000.0)
            println(
                "BAKE ${arch.label()} (${i + 1}/${archs.size}) heldOut=${fmt(bake.heldOut)} " +
                    "trainBest=${fmt(bake.trainFit)} @${elapsed}s",
            )
            println("BAKEGENOME|${arch.label()}|${GenomeIO.encode(bake.genome, arch, bake.heldOut)}")
        }
        println("BAKE done: ${archs.size} archs in ${fmt((nowMs() - start) / 1000.0)}s")
    }

    private class Bake(val genome: DoubleArray, val heldOut: Double, val trainFit: Double)

    // Train one arch, then pick the committed genome from the elite pool by HELD-OUT margin (unseen seeds under
    // the live flow-field movement model) — the training-best genome routinely overfits the fixed training
    // seeds (huge train fitness, negative held-out), so selection, not training rank, decides the champion.
    private fun bakeArch(grid: Grid, config: EvolutionConfig): Bake {
        val session = Evolution.Session(grid, BAKE_SEED, config) { HeuristicPolicy(Faction.RES) }
        while (!session.done) session.step()
        val candidates = listOf(session.bestGenome) + session.elites()
        val scored = candidates.map { g -> g to heldOutScore(g, config.arch) }
        val best = scored.maxByOrNull { it.second } ?: (session.bestGenome to 0.0)
        return Bake(best.first, best.second, session.bestFitness)
    }

    // Mean per-checkpoint MU margin over VALIDATE_SEEDS unseen seeds, under real flow-field pathing (the live
    // serve model) vs the same HeuristicPolicy baseline. Seeds are disjoint from the training seeds.
    private fun heldOutScore(genome: DoubleArray, arch: NetArch): Double {
        val net = Net.fromGenome(genome, arch)
        val grid = grid()
        var total = 0.0
        repeat(VALIDATE_SEEDS) { s ->
            val r = SimRunner.runMatch(
                grid,
                BAKE_SEED + HELD_OUT_SEED_BASE + s,
                BAKE_TICKS,
                MatchSetup(flowFields = true),
                policyEnl = NetPolicy(net, Faction.ENL),
                policyRes = HeuristicPolicy(Faction.RES),
            )
            total += r.checkpointMuSum(Faction.ENL) - r.checkpointMuSum(Faction.RES)
            SimRunner.reset()
        }
        return total / VALIDATE_SEEDS
    }

    private inline fun time(samples: Int, block: (Int) -> Unit): Double {
        val start = nowMs()
        repeat(samples) { block(it + 1) }
        return (nowMs() - start) / samples
    }

    private fun fmt(d: Double) = ((d * 100).toInt() / 100.0).toString()

    companion object {
        private const val ARENA_W = 60 // matches the original champion bake (60x40, inscribed round arena)
        private const val ARENA_H = 40
        private const val BAKE_SEED = 42
        private const val HELD_OUT_SEED_BASE = 10_000 // held-out validation seeds sit far from the training seeds
        private val BAKE_TICKS = Config.ticksPerCycle // one full scoring cycle — win it by scoring the most MU across its checkpoints

        // Train cheap on few seeds (fast), then SELECT the champion from the elite pool by held-out fitness —
        // robustness comes from selection, not a huge training budget. ~16x12x3 = 576 train matches per arch.
        private const val POP = 16
        private const val GENS = 12
        private const val ELITE = 6 // a wider elite pool → more held-out candidates to select the champion from
        private const val MATCHES_PER_EVAL = 3
        private const val VALIDATE_SEEDS = 6 // held-out matches per candidate (the selection signal)

        private val WIDTHS = listOf(4, 8, 16, 24, 32) // the TRAIN-tab per-hidden-layer widths → 25 archs

        private const val STRAIGHT_SAMPLES = 20
        private const val FIELD_SAMPLES = 5
    }
}
