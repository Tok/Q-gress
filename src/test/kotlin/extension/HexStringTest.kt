package extension

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HexStringTest {

    @Test
    fun ooToHexString() = assertEquals("00", 0.toHexString())

    @Test
    fun sevenToHexString() = assertEquals("77", 0x77.toHexString())

    @Test
    fun aaToHexString() = assertEquals("AA", 0xAA.toHexString())

    @Test
    fun ffToHexString() = assertEquals("FF", 0xFF.toHexString())

    @Test
    fun negativeToHexString() {
        assertFailsWith(IllegalStateException::class) {
            (-1).toHexString()
        }
    }

    @Test
    fun overMaxToHexString() {
        assertFailsWith(IllegalStateException::class) {
            (0xFF + 1).toHexString()
        }
    }
}
