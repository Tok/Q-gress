package system

import config.Config
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComTest {

    @BeforeTest fun clear() = Com.clear()

    @Test
    fun messageCount() {
        Com.addMessage("1")
        Com.addMessage("2")
        Com.addMessage("3")
        assertEquals(3, Com.messageCount())
    }

    @Test
    fun messageLimit() {
        (0..(Config.comMessageLimit * 3)).forEach {
            Com.addMessage(it.toString())
            assertTrue(Com.messageCount() <= Config.comMessageLimit)
        }
    }
}
