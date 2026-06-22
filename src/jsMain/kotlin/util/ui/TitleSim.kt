package util.ui

import World
import agent.Faction
import config.Location
import config.Sim
import extension.Grid
import kotlinx.browser.window
import system.display.Scene3D
import util.MapUtil
import util.SoundUtil
import util.Util
import util.data.Pos
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The faction-screen backdrop is the **real** game renderer: a tiny demo `Scene3D` with a few portals
 * auto-hacking / XMPing / linking / leveling / capturing, so the title looks exactly like in-game (it
 * reuses the showcase API — no parallel rendering). 3D terrain, zoomed out to frame all portals, a
 * slow orbiting camera, no play-area border. Runs behind the (transparent) faction menu and is wiped
 * by the reload when onboarding finishes (the game has no in-place teardown; see HtmlUtil's handoff).
 */
object TitleSim {
    private const val N = 5
    private const val TICK_MS = 850
    private const val MAX_LEVEL = 8
    private const val RING_FRAC = 0.7 // portals on a ring at this fraction of the field radius (fill the frame)
    private const val SHATTER_REBUILD_MS = 1700 // let the shards fall before a captured portal rebuilds
    private val colors = listOf(Faction.ENL.color, Faction.RES.color, "#cfcfcf")

    private var interval = 0
    private var started = false
    private val positions = mutableListOf<Pos>()

    fun start() {
        if (started) return
        started = true
        World.userFaction = Faction.ENL
        Scene3D.showBorder = false // no boundary wall/mask on the title
        window.addEventListener("pointerdown", { SoundUtil.enableAudio() }) // autoplay: unlock on first gesture
        MapUtil.loadMaps(Location.DEFAULT.toJSON(), demo = true, callback = fun(grid: Grid) {
            World.grid = grid
            World.isReady = true
            MapUtil.enable3D()
            MapUtil.setDemoSatellite(true) // satellite backdrop (game-like) without the heavy grid readback
            MapUtil.startTitleCinematic() // 3D terrain + zoom out + orbit
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
        val cx = Sim.width / 2.0
        val cy = Sim.height / 2.0
        val r = Sim.fieldRadius() * RING_FRAC
        for (i in 0 until N) {
            val ang = i * 2.0 * PI / N
            val pos = Pos((cx + r * cos(ang)).toInt(), (cy + r * sin(ang)).toInt())
            positions.add(pos)
            Scene3D.placeShowcase(pos, randomLevel(), randomColor())
        }
    }

    private fun tick() {
        if (positions.isEmpty()) return
        val pos = positions[Util.randomInt(0, positions.size - 1)]
        Scene3D.clickShowcase(pos, randomLevel(), randomColor()) // select the portal at this slot
        val lvl = randomLevel()
        when (Util.randomInt(0, 5)) {
            0, 1 -> hack(pos, lvl)
            2 -> Scene3D.xmpActiveShowcase(lvl) // plays its own XMP sound
            3 -> Scene3D.stepLastShowcaseLevel(if (Util.randomBool()) 1 else -1) // plays its own up/down sound
            4 -> Scene3D.linkLastShowcases()
            else -> capture(pos)
        }
    }

    private fun hack(pos: Pos, level: Int) {
        val glyph = Util.randomBool()
        Scene3D.hackActiveShowcase(glyph)
        if (glyph) SoundUtil.playGlyphingSound(pos, level) else SoundUtil.playHackingSound(pos, level)
    }

    // Capture: shatter the portal, then rebuild after a beat so the shards have time to fall.
    private fun capture(pos: Pos) {
        positions.remove(pos) // don't act on this slot while it's mid-shatter
        Scene3D.removeShowcaseNear(pos)
        SoundUtil.playGlassShatterSound(pos, 0.4, 0.8)
        window.setTimeout({
            Scene3D.placeShowcase(pos, randomLevel(), randomColor())
            SoundUtil.playPortalCreationSound(pos)
            positions.add(pos)
        }, SHATTER_REBUILD_MS)
    }

    private fun randomLevel() = 1 + Util.randomInt(0, MAX_LEVEL - 1)
    private fun randomColor() = colors[Util.randomInt(0, colors.size - 1)]
}
