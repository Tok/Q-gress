package ai

import agent.qvalue.QActions
import agent.qvalue.QDestinations
import util.GridFixture
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The driver tournament ranks a productive driver above an idle one, fairly (both faction sides) + deterministically. */
class TournamentTest {

    private fun grid() = GridFixture("MU", 60, 40, 2, GridFixture.rleEncode(List(60 * 40) { true })).toGrid()

    @AfterTest
    fun tearDown() = SimRunner.reset()

    // A driver that captures, deploys, links + seeks open ground — it should build real fields.
    private fun builder() = SliderVector.uniform(0.1)
        .with(QActions.CAPTURE, 1.0)
        .with(QActions.DEPLOY, 1.0)
        .with(QActions.LINK, 1.0)
        .with(QActions.HACK, 0.8)
        .with(QDestinations.MOVE_TO_UNCAPTURED, 0.8)

    // A driver that barely acts — near-zero on everything.
    private fun idle() = SliderVector.uniform(0.02)

    @Test
    fun activeDriverOutranksAnIdleOne() {
        val drivers = listOf(
            Driver("builder") { SliderVectorPolicy(builder()) },
            Driver("idle") { SliderVectorPolicy(idle()) },
        )
        // Headless field formation is seed-sensitive, so aggregate over several seeds: the active builder's
        // edge over an idle driver is robust in the mean even if any single seed is a vacuous 0–0 tie (any
        // sim tweak shifts which seeds form fields, so don't pin the property to one or two).
        val seeds = listOf(1, 2, 3, 4, 5, 6, 7, 8)
        val table = Tournament.roundRobin(grid(), drivers, seeds = seeds)

        assertEquals(2, table.size)
        assertEquals("builder", table.first().name, "the active builder ranks above the idle driver")
        val builderStanding = table.first { it.name == "builder" }
        assertEquals(seeds.size * 2, builderStanding.matches, "one pair, both faction sides, per seed")
        assertTrue(builderStanding.avgMargin() > 0.0, "the builder has a positive MU margin")
        assertTrue(builderStanding.wins >= 1, "and actually wins")
    }

    @Test
    fun sessionSteppingMatchesRoundRobin() {
        // Driving the resumable Session one match at a time must equal the all-at-once roundRobin (which is
        // literally a while-loop over step()).
        val g = grid()
        val drivers = listOf(
            Driver("builder") { SliderVectorPolicy(builder()) },
            Driver("idle") { SliderVectorPolicy(idle()) },
        )
        val viaRoundRobin = Tournament.roundRobin(g, drivers, seeds = listOf(3))
        SimRunner.reset()
        val session = Tournament.Session(g, drivers, listOf(3))
        var steps = 0
        while (!session.done) {
            session.step()
            steps++
        }
        assertEquals(session.total, steps, "stepped through every scheduled match")
        assertEquals(
            viaRoundRobin.map { it.name to it.avgMargin() },
            session.standings().map { it.name to it.avgMargin() },
            "stepping a Session ≡ roundRobin",
        )
    }

    @Test
    fun isDeterministic() {
        val drivers = listOf(Driver("a") { SliderVectorPolicy(builder()) }, Driver("b") { null })
        val first = Tournament.roundRobin(grid(), drivers, seeds = listOf(5))
        val second = Tournament.roundRobin(grid(), drivers, seeds = listOf(5))

        assertEquals(first.map { it.name to it.avgMargin() }, second.map { it.name to it.avgMargin() })
    }
}
