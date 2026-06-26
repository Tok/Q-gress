package portal

import Factory
import World
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FieldTest {

    @BeforeTest
    @AfterTest
    fun clean() = World.allPortals.clear()

    private fun portalAt(x: Int, y: Int) = Portal.create(Pos(x, y))
    private fun fieldOf(a: Portal, b: Portal, c: Portal) =
        requireNotNull(Field.create(a, b, c, Factory.agent())) { "field should form for distinct portals" }

    @Test
    fun agentSwitchEquality() = with(Factory) {
        val (origin, primary, secondary) = portalTriple()
        val field = Field.create(origin, primary, secondary, agent())
        val switched = Field.create(origin, primary, secondary, agent())
        assertEquals(field, switched)
    }

    @Test
    fun factionSwitchEquality() = with(Factory) {
        val (origin, primary, secondary) = portalTriple()
        val field = Field.create(origin, primary, secondary, frog())
        val switched = Field.create(origin, primary, secondary, smurf())
        assertEquals(field, switched)
    }

    @Test
    fun anchorSwitchEquality() = with(Factory) {
        val (origin, primary, secondary) = portalTriple()
        val field = Field.create(origin, primary, secondary, agent())
        val switched = Field.create(origin, secondary, primary, agent())
        assertEquals(field, switched)
    }

    @Test
    fun originSwitchEquality() = with(Factory) {
        val (origin, primary, secondary) = portalTriple()
        val field = Field.create(origin, primary, secondary, agent())
        val switched = Field.create(primary, origin, secondary, agent())
        assertEquals(field, switched)
    }

    @Test
    fun anchorRotationEquality() = with(Factory) {
        val (origin, primary, secondary) = portalTriple()
        val field = Field.create(origin, primary, secondary, agent())
        val rotated = Field.create(secondary, origin, primary, agent())
        assertEquals(field, rotated)
    }

    @Test
    fun noDuplicatedPortalsInField() = with(Factory) {
        val (origin, primary, secondary) = portalTriple()
        val linker = agent()
        assertFailsWith(IllegalStateException::class) {
            Field.create(origin, origin, origin, linker)
        }
        assertFailsWith(IllegalStateException::class) {
            Field.create(origin, primary, primary, linker)
        }
        assertFailsWith(IllegalStateException::class) {
            Field.create(origin, secondary, secondary, linker)
        }
    }

    // --- MU / area (the fitness objective) -----------------------------------
    // calculateMu == calculateArea == Heron's area, integer-truncated, ÷100, floored at 1 MU.

    @Test
    fun areaMatchesHeronsFormulaScaledDown() {
        // A 300–400–500 right triangle: Heron area = 60000 px² → ÷100 → 600 MU.
        val field = fieldOf(portalAt(0, 0), portalAt(300, 0), portalAt(0, 400))
        assertEquals(600, field.calculateArea(), "Heron area 60000 ÷ 100 = 600")
        assertEquals(field.calculateArea(), field.calculateMu(), "MU is the scaled area (today)")
    }

    @Test
    fun aDegenerateTriangleStillScoresAtLeastOneMu() {
        // Collinear distinct portals → zero geometric area → the max(1, …) floor keeps it at 1 MU.
        val field = fieldOf(portalAt(0, 0), portalAt(10, 0), portalAt(20, 0))
        assertEquals(1, field.calculateArea(), "a flat field floors at 1 MU, never 0")
    }

    @Test
    fun aLargerTriangleScoresMoreMu() {
        val small = fieldOf(portalAt(0, 0), portalAt(300, 0), portalAt(0, 400))
        World.allPortals.clear()
        val large = fieldOf(portalAt(0, 0), portalAt(600, 0), portalAt(0, 800))
        assertTrue(large.calculateMu() > small.calculateMu(), "doubling the legs grows the MU (≈ 4×)")
    }

    // --- coverage geometry (field layering / nesting detection) --------------

    @Test
    fun coveringDetectionSeesPortalsInsideButNotOutsideOrOnTheField() {
        val origin = portalAt(0, 0)
        val primary = portalAt(300, 0)
        val secondary = portalAt(0, 400)
        val field = fieldOf(origin, primary, secondary)
        assertTrue(field.isCoveringPortal(portalAt(50, 50)), "a portal well inside the triangle is covered")
        assertFalse(field.isCoveringPortal(portalAt(400, 400)), "a portal outside the triangle is not covered")
        assertFalse(field.isCoveringPortal(origin), "a portal that anchors the field is never 'covered' by it")
    }
}
