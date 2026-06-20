#!/usr/bin/env bash
#
# Run the Q-Gress unit-test suite (Node/Mocha, 62 tests).
#
#   ./test.sh             # run the tests
#   ./test.sh --coverage  # also produce an experimental coverage report (see note)
#
# Coverage note: Kover has no Kotlin/JS support, and c8-over-source-maps reports
# inflated numbers for transpiled Kotlin (untested .kt files can't be seen).
# Real line-coverage arrives with the functional-core / JVM-test split — see
# PLAN.md. The --coverage report is therefore INDICATIVE ONLY.
#
set -euo pipefail
cd "$(dirname "$0")"

# Pick up the project toolchain (JDK 21) if the shell doesn't already have it.
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/java ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
fi

COVERAGE=0
[ "${1:-}" = "--coverage" ] && COVERAGE=1

echo "==> Running unit tests (Node/Mocha)…"
./gradlew jsNodeTest --console=plain
echo "==> Test report: build/reports/tests/jsNodeTest/index.html"

if [ "$COVERAGE" = "1" ]; then
    echo
    echo "==> Coverage (EXPERIMENTAL — see note in this script / PLAN.md)…"
    TESTJS="build/js/node_modules/Q-Gress-test/kotlin/Q-Gress-test.js"
    if [ -f "$TESTJS" ]; then
        ( cd build/js && npx --yes c8@10 \
            --reporter=text-summary --reporter=html \
            --report-dir="$OLDPWD/build/reports/coverage" \
            node_modules/.bin/_mocha "node_modules/Q-Gress-test/kotlin/Q-Gress-test.js" ) || true
        echo "==> Coverage HTML: build/reports/coverage/index.html  (indicative only)"
    else
        echo "    Compiled test bundle not found — run ./test.sh once first."
    fi
fi
