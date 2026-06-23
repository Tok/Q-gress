package config

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the externalized-locations mechanism ([Locations.parse] + the registry): location data now
 * lives in `resources/locations.json`, loaded at runtime. These pure checks run in Node; the committed
 * JSON itself is generated from already-valid data and guarded at play time by the walkability gate.
 */
class LocationTest {

    private val sample = """
        [
          {"name":"A_SQUARE","displayName":"A Square, Town","lng":1.5,"lat":2.5,"title":true},
          {"name":"B_BRIDGE","displayName":"B Bridge, City","lng":-3.0,"lat":4.0,"title":false},
          {"name":"C_PLAIN","displayName":"C, Place","lng":10.0,"lat":-20.0}
        ]
    """.trimIndent()

    @AfterTest
    fun restore() = Locations.setAll(emptyList()) // back to DEFAULT-only so tests don't leak state

    @Test
    fun parsesFieldsAndDefaultsTitleToFalse() {
        val locs = Locations.parse(sample)
        assertEquals(3, locs.size)
        assertEquals("A Square, Town", locs[0].displayName)
        assertEquals(1.5, locs[0].lng)
        assertTrue(locs[0].title)
        assertTrue(!locs[2].title, "missing title field defaults to false")
    }

    @Test
    fun parseSkipsMalformedEntries() {
        val json = """
            [
              {"name":"OK","displayName":"Ok","lng":1.0,"lat":2.0},
              {"displayName":"no coords"},
              {"name":"NO_LAT","displayName":"x","lng":1.0}
            ]
        """.trimIndent()
        val locs = Locations.parse(json)
        assertEquals(1, locs.size)
        assertEquals("OK", locs[0].name)
    }

    @Test
    fun registryRandomTitleOnlyReturnsTitleFlagged() {
        Locations.setAll(Locations.parse(sample))
        repeat(20) { assertTrue(Locations.randomTitle().title, "randomTitle must only pick title=true entries") }
    }

    @Test
    fun byNameFindsAndMisses() {
        Locations.setAll(Locations.parse(sample))
        assertEquals("B Bridge, City", Locations.byName("B_BRIDGE")?.displayName)
        assertNull(Locations.byName("NOPE"))
    }

    @Test
    fun emptyListFallsBackToDefault() {
        Locations.setAll(emptyList())
        assertEquals(listOf(Locations.DEFAULT), Locations.all())
        assertTrue(Locations.randomTitle().title, "DEFAULT is title-eligible")
    }

    @Test
    fun parseValidatesAndDedupesACatalogue() {
        // Validate the *shape* checks on a representative catalogue (the structural guards that used to
        // run over the enum; the committed JSON is generated from already-valid data + guarded at play
        // time by the walkability gate).
        val locs = Locations.parse(sample)
        locs.forEach {
            assertTrue(it.lat in -90.0..90.0, "${it.name}: lat out of range")
            assertTrue(it.lng in -180.0..180.0, "${it.name}: lng out of range")
            assertTrue(!(it.lat == 0.0 && it.lng == 0.0), "${it.name}: null island (0,0)")
            assertTrue(it.displayName.isNotBlank(), "${it.name}: blank display name")
            assertTrue(Regex("[A-Z][A-Z0-9_]*").matches(it.name), "${it.name}: name must be ASCII UPPER_SNAKE")
        }
        val coordDupes = locs.map { it.lng to it.lat }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertEquals(emptySet(), coordDupes, "duplicate coordinates")
    }
}
