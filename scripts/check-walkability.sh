#!/usr/bin/env bash
#
# Walkability survey of every location in locations.json (PLAN map-curation aid).
# Builds + serves the dev bundle, then drives a headless Chrome through each location at the
# TINY map size, scraping the "grid built: walkability N%" line each emits during world-gen.
# Prints a table sorted least → most walkable (the removal candidates rise to the top) and
# writes build/walkability.json. Bridges are flagged 🌉 (we keep only a few).
#
#   ./scripts/check-walkability.sh            # survey all locations
#   SKIP_BUILD=1 ./scripts/check-walkability.sh   # reuse the existing dev bundle
#   PER_TIMEOUT=90 ./scripts/check-walkability.sh # longer per-location budget
#
# NB: headless uses software WebGL (swiftshader) and fetches real map tiles — needs network.
# Walkability is map-geometry driven (read from the rendered tiles), so headless is fine.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/java ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
fi

PORT="${PORT:-8099}"
DIST="build/dist/js/developmentExecutable"
LOCATIONS="src/jsMain/resources/locations.json"
OUT="${OUT:-build/walkability.json}"
# Tiny map side (sim px): inscribed circle covers Sim.TINY_KM2 (0.20 km²) at MPP_REF — see config/Sim.kt.
SIDE="${SIDE:-1690}"

if [ "${SKIP_BUILD:-0}" != "1" ]; then
    echo "==> Building dev bundle…"
    ./gradlew jsBrowserDevelopmentExecutableDistribution -q
fi
[ -f "$DIST/index.html" ] || { echo "ERROR: no dev bundle at $DIST (run without SKIP_BUILD)"; exit 1; }
mkdir -p "$(dirname "$OUT")"

echo "==> Serving $DIST on http://127.0.0.1:$PORT/"
python3 -m http.server "$PORT" --directory "$DIST" >/tmp/qgress-walk-srv.log 2>&1 &
SRV=$!
cleanup() {
    kill "$SRV" 2>/dev/null || true
    pkill -f /tmp/qgress-chrome-walk 2>/dev/null || true
}
trap cleanup EXIT INT TERM
for _ in $(seq 1 40); do curl -fsS -o /dev/null "http://127.0.0.1:$PORT/" 2>/dev/null && break; sleep 0.25; done

echo "==> Surveying $(grep -c '"name"' "$LOCATIONS") locations at the tiny map (${SIDE}px)…"
node tools/walkability/walk.mjs "http://127.0.0.1:$PORT/" "$SIDE" "$LOCATIONS" "$OUT"
