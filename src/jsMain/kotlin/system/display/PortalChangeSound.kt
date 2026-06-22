package system.display

import portal.Portal
import util.SoundUtil

/**
 * Plays the portal upgrade / downgrade / neutralize sounds by diffing each portal against the
 * previous [Scene3D] sync (level + ownership). Split out of [Scene3D] (size limit); [check] is
 * called once per portal per sync.
 */
object PortalChangeSound {
    private val prevLevel = mutableMapOf<String, Int>()
    private val prevOwned = mutableMapOf<String, Boolean>()

    fun check(id: String, portal: Portal) {
        val lvl = portal.getLevel().toInt()
        prevLevel[id]?.let { prev ->
            when {
                lvl > prev -> SoundUtil.playUpgradeSound(portal.location, lvl)
                lvl < prev && portal.owner != null -> SoundUtil.playDowngradeSound(portal.location, lvl)
            }
        }
        prevLevel[id] = lvl
        val owned = portal.owner != null
        if (prevOwned[id] == true && !owned) SoundUtil.playNeutralizeSound(portal.location)
        prevOwned[id] = owned
    }
}
