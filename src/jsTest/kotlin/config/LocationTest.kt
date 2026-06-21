package config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Structural conformance of every [Location] preset — runs in Node, so it guards new presets as
 * they're added. NOTE: this can't check *walkability* (mostly-water marina/bridge spots) — that
 * needs the live map readback; the runtime gate (`World.walkability` < MIN_WALKABILITY in
 * `HtmlUtil.onMapload`) handles those, and jet-skis/skateboards will later make water traversable.
 */
class LocationTest {

    @Test
    fun coordinatesAreInRange() = Location.values().forEach {
        assertTrue(it.lat in -90.0..90.0, "${it.name}: lat ${it.lat} out of range")
        assertTrue(it.lng in -180.0..180.0, "${it.name}: lng ${it.lng} out of range")
    }

    @Test
    fun noNullIslandCoordinates() = Location.values().forEach {
        assertTrue(!(it.lat == 0.0 && it.lng == 0.0), "${it.name}: sits at null island (0,0) — likely a typo")
    }

    @Test
    fun displayNamesAreNonBlank() = Location.values().forEach {
        assertTrue(it.displayName.isNotBlank(), "${it.name}: blank display name")
    }

    @Test
    fun enumNamesAreAsciiUpperSnake() {
        val ascii = Regex("[A-Z][A-Z0-9_]*")
        Location.values().forEach {
            assertTrue(ascii.matches(it.name), "${it.name}: enum name must be ASCII UPPER_SNAKE (display can be unicode)")
        }
    }

    @Test
    fun displayNamesAreUnique() {
        val dupes = Location.values().map { it.displayName }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertEquals(emptySet(), dupes, "duplicate display names")
    }

    @Test
    fun coordinatesAreUnique() {
        val coords = Location.values().map { it.lng to it.lat }
        val dupes = coords.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertEquals(emptySet(), dupes, "duplicate coordinates (redundant presets)")
    }

    @Test
    fun defaultIsAValidPreset() {
        assertTrue(Location.values().contains(Location.DEFAULT))
    }
}
