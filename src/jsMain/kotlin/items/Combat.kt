package items

import kotlin.math.max
import kotlin.math.min

/**
 * The pure XMP / Ultra-Strike combat model (Ingress-flavoured), shared by gameplay attacks
 * ([XmpBurster.dealDamage] + [XmpBurster.knockMods]) and the title blast ([XmpBurster.blastAt]).
 *
 * Design (from Ingress): **Bursters** take out **resonators**; **Ultra-Strikes** barely scratch
 * resonators but excel at **knocking slotted mods out** — and have a much smaller blast radius (you
 * stand on the portal). Reso damage uses Ingress' stepped **quintile** range falloff and is reduced by
 * **mitigation** (shields + links, capped 95%, 1-XM floor). A mod's **stickiness** resists knock-out:
 * low-tier items (link amps, common shields) pop out easily, high-tier shields (AEGIS) cling on, so a
 * fully-shielded L8 portal takes many strikes to strip. All functions are pure → unit-tested in CombatTest.
 */
object Combat {
    // Tuned for a DYNAMIC sim (portals must flip often), not strict Ingress realism: higher base damage,
    // a softer mitigation cap, and shields that an XMP volley actually chips off over a sustained assault.
    const val GLOBAL_DAMAGE_MULTIPLIER = 0.45 // overall reso-damage scaler (tuning knob)
    const val CRIT_MULTIPLIER = 3 // a lucky point-blank hit triples reso damage
    const val CRIT_RATE = 0.2 // crit chance when point-blank (first quintile)
    const val MAX_MITIGATION = 80 // damage-reduction cap (softened from Ingress' 95% so defended portals still fall)
    const val ULTRA_RESO_MULT = 0.12 // an Ultra-Strike does very little direct reso damage

    const val RANGE_FRAC = 0.5 // XMP effective px range = rangeM × this (matches Agent.findResosInAttackRange)
    const val ULTRA_RANGE_FRAC = 0.4 // an Ultra-Strike's blast radius is ~⅓–½ of an XMP's (you stand on the portal)

    const val XMP_KNOCK_BASE = 0.35 // per-attack chance a Burster knocks a stickiness-0 mod out, point-blank
    const val ULTRA_KNOCK_BASE = 0.7 // …Ultra-Strikes are far better at it
    const val STICKINESS_HALF = 50.0 // a mod this sticky has its knock-out chance halved

    /** Effective blast radius in sim px for an XMP/US of [rangeM] metres. */
    fun rangePx(rangeM: Int, ultra: Boolean): Double = rangeM * RANGE_FRAC * (if (ultra) ULTRA_RANGE_FRAC else 1.0)

    /** 0..1 distance fraction (1 = edge of range, ≥1 = out of range) for [rawDistPx] from the blast. */
    fun distanceFraction(rawDistPx: Double, rangeM: Int, ultra: Boolean): Double = rawDistPx / rangePx(rangeM, ultra)

    /** Stepped quintile range falloff — AUTHENTIC Ingress: 100/50/25/12.5/6.25% over fifths of max range. */
    fun rangeFalloff(distFrac: Double): Double = when {
        distFrac < 0.2 -> 1.0
        distFrac < 0.4 -> 0.5
        distFrac < 0.6 -> 0.25
        distFrac < 0.8 -> 0.125
        distFrac < 1.0 -> 0.0625
        else -> 0.0
    }

    /**
     * XM damage one resonator takes from a single hit. [weaponDamage] = the XMP level's damage; [ultra]
     * slashes it. Returns 0 if out of range, else ≥1 (Ingress 1-XM floor) after [mitigation] (0..95+).
     */
    fun resoDamage(weaponDamage: Int, distFrac: Double, mitigation: Int, ultra: Boolean, crit: Boolean): Int {
        val falloff = rangeFalloff(distFrac)
        if (falloff <= 0.0) return 0
        val critMult = if (crit) CRIT_MULTIPLIER else 1
        val ultraMult = if (ultra) ULTRA_RESO_MULT else 1.0
        val raw = weaponDamage * falloff * GLOBAL_DAMAGE_MULTIPLIER * critMult * ultraMult
        val mit = min(MAX_MITIGATION, max(0, mitigation))
        return max(1, (raw * (100 - mit) / 100).toInt())
    }

    /** Probability (0..1) that one attack knocks a mod of [stickiness] out, at [distFrac] from the centre. */
    fun knockChance(stickiness: Int, distFrac: Double, ultra: Boolean): Double {
        if (distFrac >= 1.0) return 0.0
        val base = if (ultra) ULTRA_KNOCK_BASE else XMP_KNOCK_BASE
        val prox = (1.0 - distFrac).coerceIn(0.0, 1.0) // closer to the centre → easier to knock out
        val stick = STICKINESS_HALF / (STICKINESS_HALF + max(0, stickiness)) // stickier → harder
        return (base * prox * stick).coerceIn(0.0, 1.0)
    }
}
