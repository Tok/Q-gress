package items.deployable

import Factory
import World
import agent.Agent
import items.types.HeatSinkType
import items.types.LinkAmpType
import items.types.MultihackType
import items.types.Rarity
import items.types.ShieldType
import items.types.VirusType
import util.Rng
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The deployable mod value classes ([LinkAmp], [Multihack], [Shield], [HeatSink]) and the [Virus] flip item:
 * every one delegates its rarity / abbreviation / level / owner-id / [ModType] to its type enum, so the tests
 * assert each getter resolves back to the underlying type's value. [Virus] has no level and must throw.
 */
class DeployableModsCoverageTest {

    private lateinit var owner: Agent

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        Rng.seed(11)
        owner = Factory.frog()
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
    }

    @Test
    fun linkAmpDelegatesToItsType() {
        val amp = LinkAmp(LinkAmpType.SBUL, owner)
        assertEquals(Rarity.VERY_RARE, amp.rarity)
        assertEquals("SBUL", amp.abbr)
        assertEquals(ModType.LINK_AMP, amp.modType())
        assertEquals(3, amp.getLevel())
        assertEquals("SBUL", amp.toString())
        assertEquals(owner.key(), amp.getOwnerId())
        assertEquals(0, amp.stickiness, "a link amp is not sticky (default)")
    }

    @Test
    fun multihackDelegatesToItsType() {
        val mh = Multihack(MultihackType.RARE, owner)
        assertEquals(Rarity.RARE, mh.rarity)
        assertEquals("RMH", mh.abbr)
        assertEquals(ModType.MULTIHACK, mh.modType())
        assertEquals(2, mh.getLevel())
        assertEquals("RMH", mh.toString())
        assertEquals(owner.key(), mh.getOwnerId())
    }

    @Test
    fun shieldDelegatesToItsTypeIncludingStickiness() {
        val shield = Shield(ShieldType.VERY_RARE, owner)
        assertEquals(Rarity.VERY_RARE, shield.rarity)
        assertEquals("VRS", shield.abbr)
        assertEquals(ModType.SHIELD, shield.modType())
        assertEquals(3, shield.getLevel())
        assertEquals(ShieldType.VERY_RARE.stickiness, shield.stickiness, "a shield overrides stickiness with its tier value")
        assertTrue(shield.stickiness > 0, "a very-rare shield clings on")
        assertEquals("VRS", shield.toString())
        assertEquals(owner.key(), shield.getOwnerId())
    }

    @Test
    fun heatSinkDelegatesToItsType() {
        val hs = HeatSink(HeatSinkType.COMMON, owner)
        assertEquals(Rarity.COMMON, hs.rarity)
        assertEquals("CHS", hs.abbr)
        assertEquals(ModType.HEAT_SINK, hs.modType())
        assertEquals(1, hs.getLevel())
        assertEquals("CHS", hs.toString())
        assertEquals(owner.key(), hs.getOwnerId())
    }

    @Test
    fun virusStringsAndOwnerButHasNoLevel() {
        val virus = Virus(VirusType.JARVIS_VIRUS, owner)
        assertEquals("JARVIS", virus.toString())
        assertEquals(owner.key(), virus.getOwnerId())
        assertFailsWith<NotImplementedError>("a virus has no level") { virus.getLevel() }
    }

    @Test
    fun everyModTypeIsDistinct() {
        val types = listOf(
            LinkAmp(LinkAmpType.RARE, owner).modType(),
            Multihack(MultihackType.COMMON, owner).modType(),
            Shield(ShieldType.COMMON, owner).modType(),
            HeatSink(HeatSinkType.COMMON, owner).modType(),
        )
        assertEquals(types.size, types.toSet().size, "each mod reports a distinct ModType")
        assertEquals(ModType.values().size, ModType.values().toSet().size)
    }
}
