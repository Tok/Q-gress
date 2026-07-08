package ai.net

import ai.Observation
import ai.SliderVector
import kotlin.js.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Guards the per-arch champion library: every baked genome decodes for this build and runs a valid forward pass. */
class ChampionLibraryTest {

    @AfterTest
    fun tidy() = ChampionLibrary.reset() // drop any override an install test left on the singleton

    @Test
    fun everyBakedGenomeDecodesAndRuns() {
        val archs = ChampionLibrary.bakedArchs()
        assertTrue(archs.isNotEmpty(), "the library has at least the default champion")
        archs.forEach { arch ->
            val net = GenomeIO.decode(ChampionLibrary.jsonFor(arch))
            assertEquals(arch.label(), net.arch.label(), "the genome for $arch decodes to that same arch")
            val out = net.forward(DoubleArray(Observation.SIZE) { 0.5 })
            assertEquals(SliderVector.SIZE, out.size, "the ${arch.label()} champion drives every slider slot")
            assertTrue(out.all { it in 0.0..1.0 }, "the ${arch.label()} champion outputs valid 0..1 weights")
        }
    }

    @Test
    fun anUntrainedArchFallsBackToTheDefault() {
        // A width the sweep never bakes (7 isn't in 4/8/16/24/32) resolves to the default champion.
        assertEquals(ChampionLibrary.defaultJson(), ChampionLibrary.jsonFor(NetArch(listOf(7, 7))))
    }

    @Test
    fun theDefaultChampionIsInTheLibrary() {
        assertEquals(ChampionLibrary.DEFAULT_LABEL, NetArch.DEFAULT.label(), "the default arch label matches the library key")
        assertTrue((GenomeIO.fitnessOf(ChampionLibrary.defaultJson()) ?: 0.0) > 0.0, "the default champion has a positive fitness margin")
    }

    @Test
    fun installChampionReplacesThatArchsChampion() {
        val arch = NetArch(listOf(4, 4)) // "13 → 4 → 4 → 17"
        val json = ChampionLibrary.jsonFor(arch) // a valid genome for this build
        ChampionLibrary.installChampion(json) // installing a single champion must succeed…
        assertEquals(json, ChampionLibrary.jsonFor(arch), "…and become that arch's champion")
        assertEquals(25, ChampionLibrary.bakedArchs().size, "replacing an existing arch doesn't change the count")
    }

    @Test
    fun installChampionRejectsAGenomeBuiltForADifferentNetIo() {
        val doc = JSON.parse<Json>(ChampionLibrary.defaultJson())
        doc.asDynamic().inputs = NetArch.INPUTS + 7 // a genome trained against a different Observation layout
        assertFailsWith<IllegalArgumentException> { ChampionLibrary.installChampion(JSON.stringify(doc)) }
    }
}
