package util

import Factory
import World
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The portal-naming seam (PLAN Phase-B groundwork): a new portal asks for a map-derived name through
 * [Names.sink], falling back to the pure [PortalNameGen]. Proves (a) the default headless sink
 * ([NoOpPortalNamer]) yields a generated (non-blank) name, and (b) an installed namer is consulted and its
 * name is used. Mirrors the Effects / Audio / Nav seam tests.
 */
class NamesSeamTest {

    @BeforeTest
    fun clean() {
        World.allPortals.clear()
    }

    @AfterTest
    fun restore() {
        World.allPortals.clear()
        Names.reset()
    }

    @Test
    fun portalGetsAGeneratedNameWithDefaultSink() {
        // Default Node sink is NoOpPortalNamer → nameFor returns null → PortalNameGen fallback.
        val portal = Factory.portal()

        assertTrue(portal.name.isNotBlank(), "a headless portal still gets a generated name")
    }

    @Test
    fun portalAdoptsTheInstalledNamersName() {
        Names.install(FixedNamer("Trafalgar Square"))

        val portal = Factory.portal()

        assertEquals("Trafalgar Square", portal.name, "the portal adopts the name the installed sink provides")
    }
}

/** A test [PortalNamer] that always returns the same name. */
private class FixedNamer(private val name: String) : PortalNamer {
    override fun nameFor(location: Pos): String = name
}
