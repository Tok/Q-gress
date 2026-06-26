package portal

import config.Time
import kotlin.math.E
import kotlin.math.atan
import kotlin.math.round

/**
 * Pure portal combat/cooldown math — the link-defense curve, retaliation damage, hack cooldown bucketing and
 * burnout check, extracted from [Portal] so they live in the shared functional core (`commonMain`) with no
 * `World`/WebGL/DOM coupling. JVM-unit-tested + Kover-covered; [Portal] delegates to these.
 */
object PortalMath {
    private const val ZAP_BASE_XM = 15 // retaliation XM damage per portal level
    private const val ZAP_SHIELD_XM = 1 // extra retaliation XM per point of mitigation (shields zap harder)

    // Link-mitigation curve: damage reduction (%) rises with the portal's total link count along an arctan
    // that saturates near the asymptote 400/9 × π/2 ≈ 69.8% — diminishing returns, so the first links matter
    // most and a heavily-linked portal can't become invulnerable from links alone. The total cap (links +
    // shields) is applied separately in [Portal.totalMitigation].
    private const val LINK_MITIGATION_SCALE = 400.0 / 9.0
    fun linkMitigationFor(linkCount: Int): Int = round(LINK_MITIGATION_SCALE * atan(linkCount / E)).toInt()

    /** Pure retaliation XM damage a defended portal deals: scales with portal [level] and, harder, with its
     *  total [mitigation] (a shielded portal zaps back more). See [Portal.retaliate]. */
    fun retaliationDamage(level: Int, mitigation: Int): Int = ZAP_BASE_XM * level + ZAP_SHIELD_XM * mitigation

    /** Pure hack cooldown: how much of the [baseCooldownS]-second window remains [ticksSinceLastHack] after
     *  the last hack, bucketed to a [Cooldown] (NONE once the window has elapsed). See [Portal.handleCooldown]. */
    fun cooldownAfter(ticksSinceLastHack: Int, baseCooldownS: Int): Cooldown =
        Cooldown.valueOf(Time.ticksToSeconds(Time.secondsToTicks(baseCooldownS) - ticksSinceLastHack))

    /** Pure burnout check: true once EVERY hack in [lastHackTicks] falls inside the burnout window ending at
     *  [tickNr] (i.e. none is old enough to have aged out), meaning the agent has hacked too much too fast. */
    fun isBurnedOut(lastHackTicks: List<Int>, tickNr: Int): Boolean =
        lastHackTicks.none { it < tickNr - Time.secondsToTicks(Cooldown.BURNOUT.seconds) }
}
