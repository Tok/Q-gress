package system.effect

import Factory
import World
import agent.Faction
import items.deployable.Mod
import portal.Octant
import portal.Portal
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The effect-sink seam (PLAN Phase 6.1 groundwork): game logic fires visual effects through [Fx.sink], so
 * it runs headless without touching the three.js renderer. Proves (a) the default headless sink lets the
 * tick-path logic run without crashing — `Portal.remove()` used to throw via the unguarded
 * `Scene3D.shatterPortal` forcing a lazy three.js geometry — and (b) logic actually reaches the installed
 * sink.
 */
class EffectsSeamTest {

    @BeforeTest
    fun clean() {
        World.allPortals.clear()
        World.allAgents.clear()
    }

    @AfterTest
    fun restore() {
        World.allPortals.clear()
        World.allAgents.clear()
        Fx.reset()
    }

    @Test
    fun portalRemoveRunsHeadlessWithDefaultSink() {
        // Default headless sink is NoOpEffects — remove() no longer references Scene3D, so no crash.
        val portal = Factory.portal(Faction.ENL)
        World.allPortals.add(portal)

        portal.remove()

        assertFalse(portal in World.allPortals, "removed portal is gone from the world")
    }

    @Test
    fun removeRoutesShatterToTheInstalledSink() {
        val fake = CountingEffects()
        Fx.install(fake)
        val portal = Factory.portal(Faction.ENL)
        World.allPortals.add(portal)

        portal.remove()

        assertEquals(1, fake.shatterPortal, "remove() fires exactly one portal shatter through the sink")
    }

    @Test
    fun retaliateRoutesBoltToTheInstalledSink() {
        val fake = CountingEffects()
        Fx.install(fake)
        val enemyPortal = Factory.portal(Faction.ENL) // owned by ENL

        enemyPortal.retaliate(Factory.smurf()) // a RES agent intrudes → the portal zaps back

        assertEquals(1, fake.fireBolt, "an enemy portal fires one retaliation bolt through the sink")
    }
}

/** A test [Effects] sink that records how many times each op was called. */
private class CountingEffects : Effects {
    var playXmpBurst = 0
    var showDamageNumber = 0
    var recordHack = 0
    var rewardFx = 0
    var recordDeploy = 0
    var collectXmFx = 0
    var fireBolt = 0
    var dropMods = 0
    var shatterPortal = 0
    var dropResonator = 0
    var flashVectorField = 0

    override fun playXmpBurst(location: Pos, level: Int, sound: Boolean) {
        playXmpBurst++
    }

    override fun showDamageNumber(portal: Portal, amount: Int) {
        showDamageNumber++
    }

    override fun recordHack(id: String, faction: Faction, glyph: Boolean, durationS: Double) {
        recordHack++
    }

    override fun rewardFx(portalLocation: Pos, level: Int, to: Pos, count: Int) {
        rewardFx++
    }

    override fun recordDeploy(id: String, octant: Octant, from: Pos) {
        recordDeploy++
    }

    override fun collectXmFx(from: Pos, to: Pos) {
        collectXmFx++
    }

    override fun fireBolt(from: Pos, fromLevel: Int, to: Pos, color: String) {
        fireBolt++
    }

    override fun dropMods(location: Pos, level: Int, mods: List<Mod>) {
        dropMods++
    }

    override fun shatterPortal(location: Pos, color: String, level: Int, resos: Map<Octant, Int>) {
        shatterPortal++
    }

    override fun dropResonator(location: Pos, level: Int, octantIndex: Int, resoLevel: Int) {
        dropResonator++
    }

    override fun flashVectorField(portalId: String) {
        flashVectorField++
    }
}
