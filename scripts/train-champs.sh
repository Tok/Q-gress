#!/usr/bin/env bash
#
# 2nd-generation champion rebake: NN-vs-NN. Where `bake-champs.sh` trains each arch's champion against the
# HeuristicPolicy baseline (gen 1), this trains a fresh challenger against that arch's CURRENT repo champion,
# runs a round-robin decider, and **replaces the per-arch JSON only when a challenger actually beats the
# incumbent**. Then it prints a cross-arch **overall ladder** report. The reference genomes are always in git,
# so overwriting a champion file is safe — a regression is a `git checkout` away.
#
#   ./scripts/train-champs.sh            # train + decide every arch, harvest winners, then the overall ladder
#   ./scripts/train-champs.sh --resume   # skip archs already decided in build/champions-train/ (continue a run)
#   ./scripts/train-champs.sh --overall  # just run the overall-ladder report (no training, no file changes)
#
# The heavy lifting is the reusable harness `ai.net.ChampionTrainNN` (jsTest) — it reuses the same
# Evolution / Tournament / SimRunner / GenomeIO the in-game TrainerPanel + LeaderboardPanel run on, so a
# scripted rebake and in-game training are mechanism-equivalent. One arch per fresh Node process (bounds heap,
# resumable), same as bake-champs.
#
set -uo pipefail
cd "$(dirname "$0")/.."

# Pick up the project toolchain (JDK 21) if the shell doesn't already have it.
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/java ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
fi
export NODE_OPTIONS="${NODE_OPTIONS:-} --max-old-space-size=4096"

GEN="src/jsMain/resources/champions"   # per-arch <arch>.json + index.json
XML_DIR="build/test-results/jsNodeTest"
OUT="build/champions-train"
WIDTHS="4 8 16 24 32"

# Stream ONLY our own "TRAIN " progress lines from a Gradle run — no `> Task :…` list, no JUnit STANDARD_OUT
# banner, no BUILD-SUCCESSFUL summary, and NOT the huge `TRAINGENOME|…weights…` line (no trailing space, so
# `TRAIN ` doesn't match it). The full winning genome is harvested from the JUnit XML into the .line file.
train_filter() { grep --line-buffered -E '^[[:space:]]*TRAIN ' | sed -u -E 's/^[[:space:]]*//'; }

run_overall() {
    echo "==> Overall ladder — cross-arch NN-vs-NN round-robin over all current champions…"
    TRAIN_OVERALL=1 ./gradlew jsNodeTest --tests 'ai.net.ChampionTrainNN' \
        -PmochaTimeout=3600s -PbakeVerbose=1 --console=plain --rerun 2>&1 | train_filter || true
}

if [ "${1:-}" = "--overall" ]; then
    run_overall
    exit 0
fi

RESUME=0
[ "${1:-}" = "--resume" ] && RESUME=1
[ "$RESUME" = 1 ] || rm -rf "$OUT"
mkdir -p "$OUT"

echo "==> Training a challenger per arch vs its own champion (one fresh process per arch; this takes a while)…"
NEW=""
for h1 in $WIDTHS; do
    for h2 in $WIDTHS; do
        key="$h1-$h2"
        if [ "$RESUME" = 1 ] && [ -f "$OUT/$key.line" ]; then
            echo "==> [$key] already decided — skipping (--resume)"
            continue
        fi
        echo "==> [$key] training + deciding… (live progress below)"
        ok=0
        for attempt in 1 2 3; do
            TRAIN_CHAMPS=1 TRAIN_ARCH="$key" ./gradlew jsNodeTest --tests 'ai.net.ChampionTrainNN' \
                -PmochaTimeout=3600s -PbakeVerbose=1 --console=plain --rerun 2>&1 | train_filter
            rc=${PIPESTATUS[0]} # the Gradle exit code, not the filter's
            if [ "$rc" = 0 ]; then
                # A winner emits one TRAINGENOME line; an incumbent that held emits none → an empty marker file.
                line=$(grep -rhoE 'TRAINGENOME\|[^<]*' "$XML_DIR"/*.xml 2>/dev/null | tail -1)
                printf '%s\n' "$line" > "$OUT/$key.line"
                if [ -n "$line" ]; then
                    NEW="$NEW $key"
                    echo "   [$key] ✓ NEW champion → $OUT/$key.line"
                else
                    echo "   [$key] incumbent held (no change)"
                fi
                ok=1
                break
            fi
            echo "   [$key] process failed (attempt $attempt)"
        done
        [ "$ok" = 1 ] || echo "   [$key] gave up after retries — leaving its champion unchanged"
    done
done

echo "==> Harvesting winners into $GEN/ (overwrite winners; rebuild index over all archs)…"
python3 - "$OUT" "$GEN" <<'PY'
import sys, os, glob, html, re, json

out_dir, gen_dir = sys.argv[1], sys.argv[2]
SCHEMA_VERSION = 1
DEFAULT_LABEL = "13 → 16 → 16 → 17"   # keep in sync with ChampionLibrary / NetArch.DEFAULT

def fname(label):
    return label.replace(" → ", "-").replace(" ", "") + ".json"

# Winners only — overwrite each winning arch's per-arch file.
new = {}
for path in sorted(glob.glob(os.path.join(out_dir, "*.line"))):
    for raw in open(path, encoding="utf-8"):
        for m in re.finditer(r"TRAINGENOME\|[^\n]*", html.unescape(raw)):
            _, label, js = m.group(0).split("|", 2)
            new[label] = json.loads(js.strip())
for label, genome in new.items():
    with open(os.path.join(gen_dir, fname(label)), "w", encoding="utf-8") as f:
        json.dump(genome, f, ensure_ascii=False, separators=(",", ":"))

# Rebuild index.json from EVERY per-arch file present (winners + untouched incumbents), deriving each label
# from its genome's arch so a non-updated champion is never dropped.
archs, inputs, outputs = {}, None, None
for p in sorted(glob.glob(os.path.join(gen_dir, "*.json"))):
    if os.path.basename(p) == "index.json":
        continue
    g = json.load(open(p, encoding="utf-8"))
    inputs, outputs = g["inputs"], g["outputs"]
    label = " → ".join([str(g["inputs"])] + [str(h) for h in g["arch"]["hiddens"]] + [str(g["outputs"])])
    archs[label] = os.path.basename(p)
if DEFAULT_LABEL not in archs:
    sys.exit("ERROR: default champion %r missing after harvest — refusing to write index" % DEFAULT_LABEL)
index = {"schemaVersion": SCHEMA_VERSION, "inputs": inputs, "outputs": outputs, "default": DEFAULT_LABEL, "archs": archs}
with open(os.path.join(gen_dir, "index.json"), "w", encoding="utf-8") as f:
    json.dump(index, f, ensure_ascii=False, separators=(",", ":"))
print("harvest: %d new champion(s); index rebuilt over %d archs" % (len(new), len(archs)))
PY

echo
run_overall

echo
echo "==> Done."
if [ -n "$NEW" ]; then
    echo "    NEW champions this run:$NEW"
    echo "    Review the diff, then: ./gradlew ktlintCheck detekt compileKotlinJs jsNodeTest && git add -A && git commit"
else
    echo "    No new champions — every incumbent held (no file changes)."
fi
