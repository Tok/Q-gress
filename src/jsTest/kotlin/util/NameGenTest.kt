package util

import agent.Faction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NameGenTest {
    @Test
    fun handlesAreUniqueWithinARoster() {
        Util.seed(12345)
        NameGen.reset()
        val names = (1..400).map { NameGen.handle(Faction.ENL, "Berlin") }
        assertEquals(400, names.toSet().size, "handles must be unique (they double as lookup keys)")
    }

    @Test
    fun handlesArePlausiblyShaped() {
        Util.seed(999)
        NameGen.reset()
        val names = (1..200).map { NameGen.handle(Faction.RES, "Paris") }
        assertTrue(names.all { it.isNotBlank() }, "no blank handles")
        assertTrue(names.all { it.length in 2..32 }, "handles stay a sane length")
        assertTrue(names.all { it.none(Char::isWhitespace) }, "handles have no spaces")
    }

    @Test
    fun locationFlavourCanAppear() {
        Util.seed(7)
        NameGen.reset()
        val names = (1..200).map { NameGen.handle(Faction.ENL, "Berlin") }
        assertTrue(names.any { it.contains("Berlin", ignoreCase = true) }, "location should flavour some handles")
    }
}
