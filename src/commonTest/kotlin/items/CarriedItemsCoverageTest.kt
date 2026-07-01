package items

import Factory
import World
import agent.Agent
import items.level.PowerCubeLevel
import items.level.UltraStrikeLevel
import items.level.XmpLevel
import util.Rng
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The carried (inventory) item value classes — [UltraStrike], [PowerCube], [XmpBurster]: their [toString]
 * labels, `getLevel`, `getOwnerId`, and the `create` factory overloads (int level → level enum). Level /
 * damage / cost live on the level enums; here we check the item forwards them.
 */
class CarriedItemsCoverageTest {

    private lateinit var owner: Agent

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        Rng.seed(13)
        owner = Factory.frog()
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
    }

    @Test
    fun ultraStrikeForwardsLevelOwnerAndLabel() {
        val us = UltraStrike(UltraStrikeLevel.FIVE, owner)
        assertEquals(5, us.getLevel())
        assertEquals("US5", us.toString())
        assertEquals(owner.key(), us.getOwnerId())
        assertEquals(UltraStrikeLevel.FIVE, us.level)
    }

    @Test
    fun powerCubeForwardsLevelOwnerAndLabelAndFactoryClips() {
        val pc = PowerCube.create(owner, 3)
        assertEquals(3, pc.getLevel())
        assertEquals("PC3", pc.toString())
        assertEquals(owner.key(), pc.getOwnerId())
        assertEquals(PowerCubeLevel.THREE, pc.level)
        // The int overload routes through PowerCubeLevel.valueOf, which clips to 1..8.
        assertEquals(PowerCubeLevel.EIGHT, PowerCube.create(owner, 99).level, "an over-high level clips to 8")
    }

    @Test
    fun xmpBursterForwardsLevelOwnerAndLabelAndFactoryClips() {
        val xmp = XmpBurster.create(owner, 7)
        assertEquals(7, xmp.getLevel())
        assertEquals("XMP7", xmp.toString())
        assertEquals(owner.key(), xmp.getOwnerId())
        assertEquals(XmpLevel.SEVEN, xmp.level)
        assertEquals(XmpLevel.ONE, XmpBurster.create(owner, 0).level, "an under-low level clips to 1")
        assertEquals(XmpLevel.EIGHT, XmpBurster.create(owner, XmpLevel.EIGHT).level, "the enum overload passes through")
    }
}
