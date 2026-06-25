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
        // Seeds chosen so fields actually form (headless field formation is seed-sensitive) — otherwise the
        // match is a vacuous 0–0 tie that proves nothing about ranking.
        val table = Tournament.roundRobin(grid(), drivers, seeds = listOf(3, 5))

        assertEquals(2, table.size)
        assertEquals("builder", table.first().name, "the active builder ranks above the idle driver")
        val builderStanding = table.first { it.name == "builder" }
        assertEquals(4, builderStanding.matches, "one pair, both faction sides, 2 seeds = 4 matches")
        assertTrue(builderStanding.avgMargin() > 0.0, "the builder has a positive MU margin")
        assertTrue(builderStanding.wins >= 1, "and actually wins")
    }

    @Test
    fun isDeterministic() {
        val drivers = listOf(Driver("a") { SliderVectorPolicy(builder()) }, Driver("b") { null })
        val first = Tournament.roundRobin(grid(), drivers, seeds = listOf(5))
        val second = Tournament.roundRobin(grid(), drivers, seeds = listOf(5))

        assertEquals(first.map { it.name to it.avgMargin() }, second.map { it.name to it.avgMargin() })
    }
}
