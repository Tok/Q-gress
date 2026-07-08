#!/usr/bin/env bash
#
# Rebake one champion per net architecture and regenerate the committed genome library.
#
#   ./scripts/bake-champs.sh          # train every arch, regenerate ChampionGenomes.kt
#   ./scripts/bake-champs.sh --bench  # just time a match (size the training budget), no bake
#   ./scripts/bake-champs.sh --resume # skip archs already baked in build/champions/ (continue a run)
#
# The heavy lifting lives in the reusable harness `ai.net.ChampionBake` (jsTest): it trains a net for the
# arch named by BAKE_ARCH against the adaptive HeuristicPolicy baseline and, per arch, commits the ELITE
# genome with the best HELD-OUT (unseen-seed) margin — so an overfit champion is never shipped. It prints
# `BAKEGENOME|<label>|<json>`.
#
# We bake ONE arch per fresh Gradle/Node process (not all 25 in one long-lived process): a full sweep runs
# thousands of matches over ~1-2 h, and a single Node process can exhaust its heap partway through. Per-arch
# processes bound memory (each arch is ~600 matches, proven to fit), make the run resumable, and let a
# transient crash retry just that arch instead of losing everything.
#
set -uo pipefail
cd "$(dirname "$0")/.."

# Pick up the project toolchain (JDK 21) if the shell doesn't already have it.
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/java ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
fi
# Give the test Node process a generous heap for the long headless sims.
export NODE_OPTIONS="${NODE_OPTIONS:-} --max-old-space-size=4096"

GEN="src/jsMain/resources/champions"   # one <arch>.json per architecture + an index.json manifest
XML_DIR="build/test-results/jsNodeTest"
OUT="build/champions"
WIDTHS="4 8 16 24 32"

if [ "${1:-}" = "--bench" ]; then
    echo "==> Timing a bake match (BAKE_BENCH)…"
    BAKE_BENCH=1 ./gradlew jsNodeTest --tests 'ai.net.ChampionBake' -PmochaTimeout=600s --console=plain --rerun
    grep -rhoE 'BAKE bench[^<]*' "$XML_DIR" 2>/dev/null || echo "(no bench output found)"
    exit 0
fi

RESUME=0
[ "${1:-}" = "--resume" ] && RESUME=1
[ "$RESUME" = 1 ] || rm -rf "$OUT"
mkdir -p "$OUT"

echo "==> Baking one champion per architecture (one fresh process per arch; this takes ~1-2 h)…"
FAILED=""
for h1 in $WIDTHS; do
    for h2 in $WIDTHS; do
        key="$h1-$h2"
        if [ "$RESUME" = 1 ] && [ -s "$OUT/$key.line" ]; then
            echo "==> [$key] already baked — skipping (--resume)"
            continue
        fi
        echo "==> [$key] baking… (live per-generation progress streams below)"
        ok=0
        for attempt in 1 2 3 4; do
            if BAKE_CHAMPS=1 BAKE_ARCH="$key" ./gradlew jsNodeTest --tests 'ai.net.ChampionBake' \
                -PmochaTimeout=3600s -PbakeVerbose=1 --console=plain --rerun; then
                line=$(grep -rhoE 'BAKEGENOME\|[^<]*' "$XML_DIR"/*.xml 2>/dev/null | tail -1)
                if [ -n "$line" ]; then
                    printf '%s\n' "$line" > "$OUT/$key.line"
                    ok=1
                    break
                fi
                echo "   [$key] ran but produced no genome line (attempt $attempt)"
            else
                echo "   [$key] process failed (attempt $attempt)"
            fi
        done
        [ "$ok" = 1 ] || FAILED="$FAILED $key"
    done
done

echo "==> Harvesting baked genomes into $GEN/ (one file per arch + index.json)…"
python3 - "$OUT" "$GEN" <<'PY'
import sys, os, glob, html, re, json

out_dir, gen_dir = sys.argv[1], sys.argv[2]
DEFAULT_LABEL = "13 → 16 → 16 → 17"   # NetArch.DEFAULT — the fallback champion (keep in sync with ChampionLibrary)
SCHEMA_VERSION = 1                     # keep in sync with ChampionLibrary.SCHEMA_VERSION

def fname(label):   # "13 → 16 → 16 → 17" -> "13-16-16-17.json" (the per-arch file, no timestamp = a dev default)
    return label.replace(" → ", "-").replace(" ", "") + ".json"

# Each build/champions/<arch>.line holds a BAKEGENOME|<label>|<genome-json> record.
entries = {}
for path in sorted(glob.glob(os.path.join(out_dir, "*.line"))):
    with open(path, encoding="utf-8") as f:
        for raw in f:
            for m in re.finditer(r"BAKEGENOME\|[^\n]*", html.unescape(raw)):
                _, label, js = m.group(0).split("|", 2)
                entries[label] = json.loads(js.strip())

if not entries:
    sys.exit("ERROR: no baked genomes found in %s — nothing regenerated (%s untouched)" % (out_dir, gen_dir))
if DEFAULT_LABEL not in entries:
    sys.exit("ERROR: default champion %r not among the baked archs — refusing to write a library without it" % DEFAULT_LABEL)

os.makedirs(gen_dir, exist_ok=True)
archs = {}                              # arch label -> filename (the manifest body)
for label, genome in entries.items():
    fn = fname(label)
    archs[label] = fn
    with open(os.path.join(gen_dir, fn), "w", encoding="utf-8") as f:
        json.dump(genome, f, ensure_ascii=False, separators=(",", ":"))

# Prune any stale per-arch files no longer in the sweep (keep index.json + the current set).
keep = set(archs.values()) | {"index.json"}
for stale in glob.glob(os.path.join(gen_dir, "*.json")):
    if os.path.basename(stale) not in keep:
        os.remove(stale)

sample = entries[DEFAULT_LABEL]
index = {
    "schemaVersion": SCHEMA_VERSION,
    "inputs": sample["inputs"],
    "outputs": sample["outputs"],
    "default": DEFAULT_LABEL,
    "archs": archs,
}
with open(os.path.join(gen_dir, "index.json"), "w", encoding="utf-8") as f:
    json.dump(index, f, ensure_ascii=False, separators=(",", ":"))
print("wrote %d per-arch champions + index.json to %s/" % (len(entries), gen_dir))
PY

echo
if [ -n "$FAILED" ]; then
    echo "!!! These archs failed to bake (rerun with --resume to retry just them):$FAILED"
fi
echo "==> Done. Next:"
echo "    ./gradlew ktlintFormat && ./gradlew ktlintCheck detekt compileKotlinJs jsNodeTest"
echo "    git add -A && git commit   # after reviewing the diff"
