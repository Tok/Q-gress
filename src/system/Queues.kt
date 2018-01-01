package system

import World
import agent.Agent
import items.XmpBurster
import util.SoundUtil
import util.Util
import util.data.Coords
import util.data.Damage


object Queues {
    val attackQueue: MutableMap<Int, MutableMap<Coords, List<XmpBurster>>> = mutableMapOf()
    val damageQueue: MutableMap<Int, List<Damage>> = mutableMapOf()
    val attackDelayTicks: Int = Util.secondsToTicks(10).toInt()
    val damageDelayTicks: Int = Util.secondsToTicks(10).toInt()

    fun registerAttack(agent: Agent, xmps: List<XmpBurster>) {
        val attackFutureTick = World.tick + attackDelayTicks
        var attackMap = attackQueue.get(attackFutureTick)
        if (attackMap == null) {
            attackMap = mutableMapOf()
        }
        attackMap.put(agent.pos, xmps)
        attackQueue.put(attackFutureTick, attackMap)

        val damageFutureTick = World.tick + damageDelayTicks
        val damageList: List<Damage> = xmps.flatMap { xmp -> xmp.dealDamage(agent) }

        val soundLimit = 4
        xmps.take(soundLimit).forEach { SoundUtil.playXmpSound(it.level, agent.pos) }
        damageQueue.put(damageFutureTick, damageList)
    }

    fun endTick(tick: Int) {
        val futureAttacks = attackQueue.filter { it.key > tick }
        attackQueue.clear()
        attackQueue.putAll(futureAttacks)

        val futureDamages = damageQueue.filter { it.key > tick }
        damageQueue.clear()
        damageQueue.putAll(futureDamages)
    }
}
