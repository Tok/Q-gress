package items

import agent.Faction
import items.deployable.Mod
import items.deployable.Resonator
import portal.PortalKey

private const val KEY_GOLD = "#FFD700"

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
