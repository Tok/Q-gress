package system

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComTest {

    @BeforeTest fun clear() = Com.clear()

    @Test
    fun messageCount() {
        Com.addMessage("1")
        Com.addMessage("2", Com.Importance.MAJOR)
        Com.addMessage("3", color = "#ff0000")
        assertEquals(3, Com.messageCount())
    }

    @Test
    fun capsTheBacklog() {
        repeat(Com.CAP * 2) { Com.addMessage(it.toString()) }
        assertTrue(Com.messageCount() <= Com.CAP, "the log never grows past CAP")
    }

    @Test
    fun preservesImportanceAndColour() {
        Com.addMessage("field", Com.Importance.MAJOR, "#03DC03")
        val entry = Com.entries().last()
        assertEquals(Com.Importance.MAJOR, entry.importance)
        assertEquals("#03DC03", entry.segments.single().color)
    }
}
