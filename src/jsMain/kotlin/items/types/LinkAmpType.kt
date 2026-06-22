package items.types

/**
 * Link amps boost link range / outbound-link count. **Inactive** in Q-Gress for now (linking range
 * isn't a balance lever yet): defined + drawable, but not dropped on hack and with no gameplay effect.
 * ~2018 Ingress: standard Rare + Very Rare (VR never drops — passcode only), plus the SoftBank Ultra
 * Link (SBUL). See docs/MECHANICS.
 */
enum class LinkAmpType(val level: Int, val abbr: String, val rarity: Rarity) {
    RARE(1, "LA", Rarity.RARE),
    VERY_RARE(2, "VRLA", Rarity.VERY_RARE),
    SBUL(3, "SBUL", Rarity.VERY_RARE),
}
