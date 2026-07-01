package items

import agent.Faction
import items.deployable.Mod
import items.deployable.Resonator
import items.deployable.Virus
import portal.PortalKey

private const val KEY_GOLD = "#FFD700"

/** How a reward item flies in: most loot is a small cube; viruses are a big faction-coloured sphere. */
enum class RewardShape { CUBE, SPHERE }

/** A reward fly-in mote: its [color] and [shape] (see [system.display.RewardFx]). */
data class RewardMote(val color: String, val shape: RewardShape)

/**
 * The colour a hacked-out item flies in for the reward FX ([system.display.RewardFx]): **gold** portal keys,
 * **level-coloured** power cubes + resonators, **rarity-coloured** mods, and the hacker's **faction colour**
 * for everything else (weapons, viruses). Keeps the drop animation readable at a glance.
 */
fun QgressItem.rewardColor(faction: Faction): String = when (this) {
    is PortalKey -> KEY_GOLD
    is PowerCube -> level.getColor()
    is Mod -> rarity.color
    is Resonator -> level.getColor()
    else -> faction.color
}

/** The full reward visual for this item: its [rewardColor] plus a shape (viruses fly as a sphere, the rest cubes). */
fun QgressItem.rewardMote(faction: Faction): RewardMote =
    RewardMote(rewardColor(faction), if (this is Virus) RewardShape.SPHERE else RewardShape.CUBE)
