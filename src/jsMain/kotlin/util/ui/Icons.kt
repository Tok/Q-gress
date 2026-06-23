package util.ui

/** Tiny inline-SVG glyphs for the toolbar (icon-only controls), drawn in `currentColor`. */
object Icons {
    /** Speaker + sound wave — the Volume control. */
    const val VOLUME =
        "<svg viewBox=\"0 0 16 16\" width=\"16\" height=\"16\" fill=\"currentColor\" aria-hidden=\"true\">" +
            "<path d=\"M3 6h2.5L9 3v10L5.5 10H3z\"/>" +
            "<path d=\"M11 5.5a3.2 3.2 0 0 1 0 5\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.3\" stroke-linecap=\"round\"/>" +
            "</svg>"

    /** Video-camera body + lens — the Auto cam toggle. */
    const val CAM =
        "<svg viewBox=\"0 0 16 16\" width=\"16\" height=\"16\" fill=\"currentColor\" aria-hidden=\"true\">" +
            "<rect x=\"1\" y=\"4.5\" width=\"9\" height=\"7\" rx=\"1\"/><path d=\"M11 8l4-2.2v4.4L11 8z\"/></svg>"
}
