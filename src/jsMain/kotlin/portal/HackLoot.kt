package portal

import agent.Agent
import config.Config
import config.DropRates
import items.PowerCube
import items.QgressItem
import items.UltraStrike
import items.XmpBurster
import items.deployable.HeatSink
import items.deployable.Multihack
import items.deployable.Resonator
import items.deployable.Shield
import items.deployable.Virus
import items.level.PowerCubeLevel
import items.level.ResonatorLevel
import items.level.UltraStrikeLevel
import items.level.XmpLevel
import items.types.HeatSinkType
import items.types.MultihackType
import items.types.ShieldType
import items.types.VirusType
import util.Rng

/**
 * The drop table for one portal hack — extracted from [Portal] (the SoC / god-object split, PLAN phase B).
 * Given the [hacker] and the effective hack level, rolls the resonators, weapons (XMP / Ultra-Strike), mods
 * (shields / heat sinks / multi-hacks), viruses and power cubes a hack yields. Pure drop-rolling over seeded
 * [Rng] and the [DropRates] / [Config] tunables; [Portal.hack] keeps the portal-specific bits (the key roll
 * and the AP/XM cost).
 */
object HackLoot {
    /** Everything a single hack drops (excludes the portal key + the energy cost, which need the portal). */
    fun rollDrops(hacker: Agent, level: Int): List<QgressItem> = obtainResos(hacker, level) +
        obtainXmps(hacker, level) +
        obtainUltraStrikes(hacker, level) +
        obtainShields(hacker) +
        obtainHeatSinks(hacker) +
        obtainMultihacks(hacker) +
        obtainVirus(hacker) +
        obtainPowerCubes(level, hacker)

    private fun obtainResos(hacker: Agent, level: Int): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        Quality.values().map { quality ->
            val selectedLevel = ResonatorLevel.find(level, quality).level
            while (Rng.random() < quality.chance) {
                stuff.add(Resonator.create(hacker, selectedLevel) as QgressItem)
            }
        }
        return stuff
    }

    private fun obtainXmps(hacker: Agent, level: Int): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        repeat(weaponDraws()) {
            // sim-tuning: more XMPs/hack so agents can sustain assaults
            Quality.values().map { quality ->
                val selectedLevel = XmpLevel.find(level, quality).level
                while (Rng.random() < quality.chance) {
                    stuff.add(XmpBurster.create(hacker, selectedLevel) as QgressItem)
                }
            }
        }
        return stuff
    }

    private fun obtainUltraStrikes(hacker: Agent, level: Int): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        repeat(weaponDraws()) {
            // US drops are rarer than XMP: one Bernoulli per draw, not a full quality cascade (as in Ingress).
            if (Rng.random() < DropRates.usDropChance) {
                stuff.add(UltraStrike(UltraStrikeLevel.find(level, Quality.GOOD), hacker))
            }
        }
        return stuff
    }

    // Live weapon-draw count per hack: the base XMP multiplier scaled by the menu "Weapon drops" slider
    // ([Config.weaponDropMultiplier], default 3× = tripled). Drives both XMP and Ultra-Strike yields.
    private fun weaponDraws(): Int = (DropRates.xmpDropMultiplier * Config.weaponDropMultiplier()).toInt().coerceAtLeast(1)

    private fun obtainShields(hacker: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        ShieldType.values().forEach {
            if (Rng.random() < DropRates.shieldChance.getValue(it)) {
                stuff.add(Shield(it, hacker))
            }
        }
        return stuff
    }

    private fun obtainHeatSinks(hacker: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        HeatSinkType.values().forEach {
            if (Rng.random() < DropRates.heatSinkChance.getValue(it)) {
                stuff.add(HeatSink(it, hacker))
            }
        }
        return stuff
    }

    private fun obtainMultihacks(hacker: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        MultihackType.values().forEach {
            if (Rng.random() < DropRates.multihackChance.getValue(it)) {
                stuff.add(Multihack(it, hacker))
            }
        }
        return stuff
    }

    private fun obtainPowerCubes(level: Int, hacker: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        Quality.values().map { quality ->
            val selectedLevel = PowerCubeLevel.find(level, quality).level
            while (Rng.random() < quality.chance * 0.3) {
                stuff.add(PowerCube.create(hacker, selectedLevel) as QgressItem)
            }
        }
        return stuff
    }

    private fun obtainVirus(hacker: Agent): List<QgressItem> {
        val stuff = mutableListOf<QgressItem>()
        VirusType.values().forEach {
            if (Rng.random() < DropRates.virusChance.getValue(it)) {
                stuff.add(Virus(it, hacker))
            }
        }
        return stuff
    }
}
