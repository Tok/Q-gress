package util.ui

import World
import agent.Faction
import config.Location
import config.Sim
import extension.Grid
import kotlinx.browser.window
import system.display.Scene3D
import util.MapUtil
import util.Util
import util.data.Pos

/**
 * The faction-screen backdrop is the **real** game renderer: a tiny demo `Scene3D` with a few portals
 * auto-hacking / XMPing / linking / leveling, so the title looks exactly like in-game (it reuses the
 * showcase API — no parallel rendering). It runs behind the (transparent) faction menu and is wiped by
 * the reload when onboarding finishes (the game has no in-place teardown; see HtmlUtil's reload handoff).
 */
object TitleSim {
    private const val N = 5
    private const val TICK_MS = 850
    private const val MAX_LEVEL = 8
    private const val UPPER_BAND = 0.4 // place portals in the upper 40% so they clear the wordmark/buttons
    private val colors = listOf(Faction.ENL.color, Faction.RES.color, "#cfcfcf")

    private var interval = 0
    private var started = false
    private val positions = mutableListOf<Pos>()

    fun start() {
        if (started) return
        started = true
        World.userFaction = Faction.ENL
        MapUtil.loadMaps(Location.DEFAULT.toJSON(), demo = true, callback = fun(grid: Grid) {
            World.grid = grid
            World.isReady = true
            MapUtil.enable3D()
            MapUtil.setDemoSatellite(true) // satellite backdrop (game-like) without the heavy grid readback
            placePortals()
            interval = window.setInterval({ tick() }, TICK_MS)
        })
    }

    /** Stop the auto-driver (the scene itself is torn down by the onboarding reload). */
    fun stop() {
        if (interval != 0) window.clearInterval(interval)
        interval = 0
    }

    private fun placePortals() {
        positions.clear()
        for (i in 0 until N) {
            val pos = Pos(Sim.width * (i + 1) / (N + 1), (Sim.height * UPPER_BAND).toInt())
            positions.add(pos)
            Scene3D.placeShowcase(pos, randomLevel(), randomColor())
        }
    }

    private fun tick() {
        if (positions.isEmpty()) return
        val pos = positions[Util.randomInt(0, positions.size - 1)]
        Scene3D.clickShowcase(pos, randomLevel(), randomColor()) // select the portal at this slot
        when (Util.randomInt(0, 5)) {
            0, 1 -> Scene3D.hackActiveShowcase(Util.randomBool()) // hack / glyph
            2 -> Scene3D.xmpActiveShowcase(1 + Util.randomInt(0, MAX_LEVEL - 1))
            3 -> Scene3D.stepLastShowcaseLevel(if (Util.randomBool()) 1 else -1)
            4 -> Scene3D.linkLastShowcases()
            else -> { // capture: shatter + respawn in a (possibly) new faction colour
                Scene3D.removeShowcaseNear(pos)
                Scene3D.placeShowcase(pos, randomLevel(), randomColor())
            }
        }
    }

    private fun randomLevel() = 1 + Util.randomInt(0, MAX_LEVEL - 1)
    private fun randomColor() = colors[Util.randomInt(0, colors.size - 1)]
}
