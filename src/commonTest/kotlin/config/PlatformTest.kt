package config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The `config` core is now commonMain; this exercises it (and the [Platform] expect/actual seam) from
 * common test code. Under the Node test runtime there's no browser, so [Platform] reports headless and
 * [Dim] takes its fixed fallbacks — proving the seam resolves and a lifted config object reads correctly.
 */
class PlatformTest {

    @Test
    fun headlessReportsNoBrowser() {
        assertFalse(Platform.isBrowser(), "the Node test runtime is not a browser")
    }

    @Test
    fun dimFallsBackToFixedSizeWhenHeadless() {
        assertEquals(1200, Dim.width, "headless width falls back to 1200")
        assertEquals(800, Dim.height, "headless height falls back to 800")
    }

    @Test
    fun liftedConfigDefaultsLoad() {
        assertEquals(5, Config.minPortals, "a lifted config constant is readable from commonTest")
    }
}
