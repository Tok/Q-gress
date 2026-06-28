#!/usr/bin/env bash
#
# Build the Q-Gress browser bundle, serve it locally, open it in a browser,
# and tear the server down again on exit (Ctrl+C).
#
#   ./start.sh            # build + serve on :8099 + open browser
#   PORT=9000 ./start.sh  # use a different port
#
set -euo pipefail
cd "$(dirname "$0")"

PORT="${PORT:-8099}"

# Pick up the project toolchain (JDK 21) if the shell doesn't already have it.
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/java ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
fi

echo "==> Building browser bundle (development)…"
./gradlew jsBrowserDevelopmentExecutableDistribution

DIST="build/dist/js/developmentExecutable"
[ -f "$DIST/index.html" ] || { echo "ERROR: build output not found at $DIST"; exit 1; }

echo "==> Serving $DIST on http://localhost:$PORT/"
python3 -m http.server "$PORT" --directory "$DIST" >/tmp/qgress-server.log 2>&1 &
SERVER_PID=$!
cleanup() {
    echo
    echo "==> Stopping server (pid $SERVER_PID)…"
    kill "$SERVER_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

# Wait until the server answers.
for _ in $(seq 1 20); do
    curl -fsS -o /dev/null "http://localhost:$PORT/" 2>/dev/null && break
    sleep 0.25
done

URL="http://localhost:$PORT/"
echo "==> Q-Gress is running at $URL  (desktop browser with WebGL required)"
# Chromium-based browsers (Chrome/Brave/Chromium) ship with speech-dispatcher — the backend for the Web Speech
# TTS announcer — DISABLED by default on Linux, so launch one with --enable-speech-dispatcher when present (the
# daemon + an espeak voice must be installed: `sudo apt install speech-dispatcher espeak-ng`). Fall back to the
# default browser otherwise (macOS / Firefox already expose voices).
CHROME="$(command -v brave-browser || command -v google-chrome || command -v chromium || command -v chromium-browser || true)"
if [ -n "$CHROME" ]; then
    "$CHROME" --enable-speech-dispatcher "$URL" >/dev/null 2>&1 &
else
    ( xdg-open "$URL" || open "$URL" ) >/dev/null 2>&1 &
fi

echo "==> Press Ctrl+C to stop."
wait "$SERVER_PID"
