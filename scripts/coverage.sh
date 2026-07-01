#!/usr/bin/env bash
#
# Line-coverage for the pure functional core (commonMain), measured on the JVM test target.
#
#   ./scripts/coverage.sh            # run jvmTest, print the coverage summary, write the HTML report
#   ./scripts/coverage.sh --verify   # also fail if coverage is below the configured Kover bound
#   ./scripts/coverage.sh --open     # open the HTML report when done (Linux/macOS)
#
# Kover instruments JVM bytecode (it can't read Kotlin/JS), so this only sees the code that compiles
# for the `jvm()` target — i.e. commonMain + commonTest. That's the functional core we care about; the
# jsMain shell (3D/DOM/WebGL/audio) is intentionally excluded. See build.gradle.kts + docs/ARCHITECTURE.md.
#
set -euo pipefail
cd "$(dirname "$0")/.."

# Pick up the project toolchain (JDK 21) if the shell doesn't already have it.
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/java ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
fi

VERIFY=0
OPEN=0
for arg in "$@"; do
    case "$arg" in
        --verify) VERIFY=1 ;;
        --open) OPEN=1 ;;
        *) echo "Unknown option: $arg" >&2; exit 2 ;;
    esac
done

echo "==> Running JVM tests + generating the Kover coverage report…"
TASKS=(koverHtmlReport koverLog)
[ "$VERIFY" = "1" ] && TASKS+=(koverVerify)
./gradlew "${TASKS[@]}" --console=plain

REPORT="build/reports/kover/html/index.html"
echo
echo "==> Coverage HTML: $REPORT"
echo "    (per-line summary printed above by koverLog)"

if [ "$OPEN" = "1" ] && [ -f "$REPORT" ]; then
    if command -v xdg-open >/dev/null 2>&1; then xdg-open "$REPORT"
    elif command -v open >/dev/null 2>&1; then open "$REPORT"
    fi
fi
