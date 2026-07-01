package system.map

/**
 * MapLibre style JSON + the keyless tile sources behind them — split out of [MapController] (size). Street
 * backdrop: hosted OpenFreeMap style. Satellite: Esri World Imagery raster. Passability "shadow":
 * OpenFreeMap vector tiles rendered as a white-roads-on-black mask that we read back via WebGL.
 */
object MapStyles {
    // --- Open, keyless tile sources (no access token / billing) ---
    const val OPENMAPTILES_URL = "https://tiles.openfreemap.org/planet" // TileJSON; BuildingTiles reads .tiles from it
    const val STREET_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"
    private const val ESRI_IMAGERY_TILES =
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"

    // Keyless DEM: AWS "Terrain Tiles" (Mapzen terrarium PNG encoding), CORS-enabled, no token.
    private const val TERRAIN_DEM_TILES = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png"
    const val TERRAIN_SOURCE = "terrain"
    const val TERRAIN_EXAGGERATION = 1.3 // height boost; tune for relief vs realism
    const val SKY_COLOR = "#2a6cc9" // sky overhead (deep blue, slight cyan)
    const val SKY_HORIZON_COLOR = "#bfe0f5" // pale cyan at the horizon
    const val SKY_FOG_COLOR = "#dfeefb" // soft haze where the ground meets the sky

    // Cap the satellite source at its globally-available native zoom; past this MapLibre upscales the
    // last good tiles (a little blurry) instead of showing "map data not yet available" placeholders.
    private const val SATELLITE_MAX_NATIVE_ZOOM = 19

    val SATELLITE_STYLE = """{
        "version": 8,
        "glyphs": "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
        "sources": {
            "satellite": {
                "type": "raster",
                "tiles": ["$ESRI_IMAGERY_TILES"],
                "tileSize": 256,
                "maxzoom": $SATELLITE_MAX_NATIVE_ZOOM,
                "attribution": "Imagery © Esri, Maxar, Earthstar Geographics"
            },
            "$TERRAIN_SOURCE": {
                "type": "raster-dem",
                "encoding": "terrarium",
                "tiles": ["$TERRAIN_DEM_TILES"],
                "tileSize": 256,
                "maxzoom": 15,
                "attribution": "Elevation © Mapzen, AWS Terrain Tiles"
            },
            "openmaptiles": { "type": "vector", "url": "$OPENMAPTILES_URL", "generateId": true }
        },
        "layers": [
            { "id": "satellite", "type": "raster", "source": "satellite", "paint": { "raster-saturation": -1 } }
        ]
    }"""

    // Demo scenes: a plain gray backdrop so effects/portals read clearly. The satellite raster is
    // present but hidden (visibility:none) so a demo checkbox can toggle it on via setLayoutProperty.
    val DEMO_STYLE = """{
        "version": 8,
        "glyphs": "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
        "sources": {
            "satellite": { "type": "raster", "tiles": ["$ESRI_IMAGERY_TILES"], "tileSize": 256, "maxzoom": $SATELLITE_MAX_NATIVE_ZOOM },
            "$TERRAIN_SOURCE": {"type":"raster-dem","encoding":"terrarium","tiles":["$TERRAIN_DEM_TILES"],"tileSize":256,"maxzoom":15},
            "openmaptiles": { "type": "vector", "url": "$OPENMAPTILES_URL", "generateId": true }
        },
        "layers": [
            { "id": "bg", "type": "background", "paint": { "background-color": "#8c8c8c" } },
            { "id": "satellite", "type": "raster", "source": "satellite", "layout": { "visibility": "none" } }
        ]
    }"""

    // Grayscale passability mask, read back via readPixels to build the movement grid.
    // Brightness = walkability: white FOOTPATHS (pedestrians prefer them) > bright-grey grass/park >
    // darker-grey default ground > darker-grey STREETS (roads — a touch darker than the built-up ground so
    // NPCs/agents avoid walking down them) > black buildings & water (impassable). Layer order matters (later
    // paints over earlier): roads paint over the ground, then FOOTPATHS paint over roads — so where a footpath
    // overlaps a street (sidewalks run along/under roads) the white path wins → brighter beats darker.
    val SHADOW_STYLE = """{
        "version": 8,
        "sources": {
            "openmaptiles": { "type": "vector", "url": "$OPENMAPTILES_URL", "generateId": true }
        },
        "layers": [
            { "id": "bg", "type": "background", "paint": { "background-color": "#555555" } },
            {
                "id": "landcover",
                "type": "fill",
                "source": "openmaptiles",
                "source-layer": "landcover",
                "paint": { "fill-color": ["match", ["get", "class"],
                    "wood", "#6e6e6e",
                    "wetland", "#5e5e5e",
                    ["grass", "farmland", "scrub"], "#9a9a9a",
                    ["sand", "rock", "ice"], "#cccccc",
                    "#9a9a9a"
                ] }
            },
            {
                "id": "landuse-green",
                "type": "fill",
                "source": "openmaptiles",
                "source-layer": "landuse",
                "filter": ["match", ["get", "class"],
                    ["park", "garden", "recreation_ground", "pitch", "grass", "cemetery"], true, false],
                "paint": { "fill-color": ["match", ["get", "class"],
                    ["pitch", "recreation_ground"], "#bdbdbd",
                    "#9a9a9a"
                ] }
            },
            {
                "id": "water",
                "type": "fill",
                "source": "openmaptiles",
                "source-layer": "water",
                "paint": { "fill-color": "#000000" }
            },
            {
                "id": "buildings",
                "type": "fill",
                "source": "openmaptiles",
                "source-layer": "building",
                "paint": { "fill-color": "#000000" }
            },
            {
                "id": "roads",
                "type": "line",
                "source": "openmaptiles",
                "source-layer": "transportation",
                "filter": ["!", ["match", ["get", "class"], ["path", "pedestrian"], true, false]],
                "paint": {
                    "line-color": "#4a4a4a",
                    "line-width": ["interpolate", ["linear"], ["zoom"], 14, 6, 18, 24]
                }
            },
            {
                "id": "footpaths",
                "type": "line",
                "source": "openmaptiles",
                "source-layer": "transportation",
                "filter": ["match", ["get", "class"], ["path", "pedestrian"], true, false],
                "paint": {
                    "line-color": "#ffffff",
                    "line-width": ["interpolate", ["linear"], ["zoom"], 14, 5, 18, 18]
                }
            }
        ]
    }"""
}
