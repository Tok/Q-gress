#!/usr/bin/env bash
#
# Headless-Chrome performance profiler for Q-Gress (PLAN phase D).
# Builds + serves the dev bundle, drives a world-gen + steady-state CPU profile in headless
# Chrome (software WebGL), and prints a self-time-per-function breakdown. Profiles + console
# land in build/profiles/ (open the .cpuprofile files in Chrome DevTools → Performance).
#
#   ./profiler.sh                                   # default: Large map + endgame roster
#   ./profiler.sh "local=true&start=mid&w=1600&h=1000&seed=1&debug=true"   # custom world
#   SKIP_BUILD=1 ./profiler.sh                      # reuse the existing dev bundle
#   GEN_TIMEOUT=400 RUNTIME_S=30 ./profiler.sh      # longer windows
#
# NB: headless uses software WebGL, so the *frame rate* is NOT representative of a real GPU —
# trust the world-gen breakdown + the sim-function self-times, not the headless FPS. For real
# FPS, run ./start.sh and read the ?debug FpsMeter overlay on your own machine.
set -euo pipefail
cd "$(dirname "$0")"

if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/java ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
fi

PORT="${PORT:-8099}"
# A Large (2x) map with the endgame roster + a density-scaled portal count — the heavy case.
QUERY="${1:-local=true&start=end&w=3200&h=2000&portals=24&seed=42&round=true&debug=true}"
GEN_TIMEOUT="${GEN_TIMEOUT:-320}"
RUNTIME_S="${RUNTIME_S:-22}"
OUT="${OUT:-build/profiles}"
DIST="build/dist/js/developmentExecutable"

if [ "${SKIP_BUILD:-0}" != "1" ]; then
    echo "==> Building dev bundle…"
    ./gradlew jsBrowserDevelopmentExecutableDistribution -q
fi
[ -f "$DIST/index.html" ] || { echo "ERROR: no dev bundle at $DIST (run without SKIP_BUILD)"; exit 1; }
mkdir -p "$OUT"

echo "==> Serving $DIST on http://127.0.0.1:$PORT/"
python3 -m http.server "$PORT" --directory "$DIST" >/tmp/qgress-prof-srv.log 2>&1 &
SRV=$!
cleanup() {
    kill "$SRV" 2>/dev/null || true
    pkill -f /tmp/qgress-chrome-prof 2>/dev/null || true
}
trap cleanup EXIT INT TERM
for _ in $(seq 1 40); do curl -fsS -o /dev/null "http://127.0.0.1:$PORT/" 2>/dev/null && break; sleep 0.25; done

echo "==> Profiling ?$QUERY"
node tools/profiling/profile.mjs "http://127.0.0.1:$PORT/?$QUERY" "$GEN_TIMEOUT" "$RUNTIME_S" "$OUT"

echo; echo "==> World-gen self-time (top functions):"
node tools/profiling/analyze.mjs "$OUT/worldgen.cpuprofile" 22
echo; echo "==> Runtime self-time (top functions — ignore GL/shader noise, software WebGL):"
node tools/profiling/analyze.mjs "$OUT/runtime.cpuprofile" 18
echo; echo "Profiles + console saved in $OUT/ (open *.cpuprofile in DevTools → Performance)."
