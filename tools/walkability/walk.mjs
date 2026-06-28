// Headless-Chrome walkability survey for Q-Gress. No deps (Node 22+ global WebSocket/fetch).
// Loads every location in locations.json at the TINY map size, scrapes the "grid built:
// walkability N%" console line each emits during world-gen, and prints a table sorted by
// walkability (least → most). Drives a single headless Chrome, reloading per location.
//
// Usage: node walk.mjs "<baseUrl>" <side> <locations.json> [outJson]
// Env:  CHROME_BIN (default /usr/bin/google-chrome), WINDOW (1600,1000), CDP_PORT (9222),
//       PER_TIMEOUT (per-location seconds, default 70)
import { spawn } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';

const BASE = process.argv[2];
const SIDE = Number(process.argv[3] ?? 1690);
const LOCATIONS = JSON.parse(readFileSync(process.argv[4], 'utf8'));
const OUT = process.argv[5] ?? '';
const CHROME = process.env.CHROME_BIN ?? '/usr/bin/google-chrome';
const WINDOW = process.env.WINDOW ?? '1600,1000';
const PORT = Number(process.env.CDP_PORT ?? 9222);
const PER_TIMEOUT = Number(process.env.PER_TIMEOUT ?? 70);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// Software WebGL (swiftshader) so it runs without a GPU — walkability is map-geometry driven
// (read from the rendered tiles), not the frame rate, so headless GL is fine here.
const chrome = spawn(CHROME, [
  '--headless=new', `--remote-debugging-port=${PORT}`, `--window-size=${WINDOW}`,
  '--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader',
  '--no-sandbox', '--disable-dev-shm-usage', '--mute-audio', '--no-first-run',
  '--user-data-dir=/tmp/qgress-chrome-walk', 'about:blank',
], { stdio: ['ignore', 'pipe', 'pipe'] });
chrome.stderr.on('data', () => {});

async function pageWs() {
  for (let i = 0; i < 60; i++) {
    try {
      const page = (await (await fetch(`http://127.0.0.1:${PORT}/json`)).json())
        .find((t) => t.type === 'page' && t.webSocketDebuggerUrl);
      if (page) return page.webSocketDebuggerUrl;
    } catch {}
    await sleep(500);
  }
  throw new Error('no page target — is Chrome installed at ' + CHROME + '?');
}

let nextId = 1;
const pending = new Map();
const send = (ws, method, params = {}) => {
  const id = nextId++;
  ws.send(JSON.stringify({ id, method, params }));
  return new Promise((res) => pending.set(id, res));
};

// The "grid built: walkability N%" line ShadowGridBuilder logs during world-gen.
const WALK_RE = /grid built: walkability (\d+)%/;
let latestWalk = null; // set by the console listener once the current navigation emits the line

const url = (loc) => `${BASE}?local=true&round=true&w=${SIDE}&h=${SIDE}` +
  `&lng=${loc.lng}&lat=${loc.lat}&name=${encodeURIComponent(loc.displayName)}`;

const main = async () => {
  const ws = new WebSocket(await pageWs());
  await new Promise((r) => (ws.onopen = r));
  ws.onmessage = (e) => {
    const m = JSON.parse(e.data);
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m.result); pending.delete(m.id); return; }
    if (m.method === 'Runtime.consoleAPICalled') {
      const txt = (m.params.args || []).map((a) => a.value ?? a.description ?? '').join(' ');
      const hit = WALK_RE.exec(txt);
      if (hit && latestWalk === null) latestWalk = Number(hit[1]);
    }
  };
  await send(ws, 'Runtime.enable');
  await send(ws, 'Page.enable');

  const results = [];
  for (let i = 0; i < LOCATIONS.length; i++) {
    const loc = LOCATIONS[i];
    latestWalk = null;
    await send(ws, 'Page.navigate', { url: url(loc) });
    const t0 = Date.now();
    while (latestWalk === null && (Date.now() - t0) / 1000 < PER_TIMEOUT) await sleep(250);
    const secs = ((Date.now() - t0) / 1000).toFixed(0);
    const pct = latestWalk;
    results.push({ name: loc.name, displayName: loc.displayName, walkability: pct });
    const label = pct === null ? 'TIMEOUT' : `${String(pct).padStart(3)}%`;
    process.stdout.write(`  [${String(i + 1).padStart(2)}/${LOCATIONS.length}] ${label}  ${secs}s  ${loc.displayName}\n`);
  }

  // Sort least-walkable first (the removal candidates rise to the top); nulls (timeouts) last.
  const rank = (r) => (r.walkability === null ? 999 : r.walkability);
  results.sort((a, b) => rank(a) - rank(b));

  console.log('\n== Walkability (tiny map, least → most) ==');
  for (const r of results) {
    const bridge = /\bbridge\b/i.test(r.displayName) ? ' 🌉' : '';
    const pct = r.walkability === null ? 'TIMEOUT' : `${String(r.walkability).padStart(3)}%`;
    console.log(`  ${pct}  ${r.name.padEnd(20)} ${r.displayName}${bridge}`);
  }

  if (OUT) { writeFileSync(OUT, JSON.stringify(results, null, 2)); console.log(`\nWrote ${OUT}`); }
  ws.close();
  chrome.kill();
  process.exit(0);
};
main().catch((e) => { console.error(e); chrome.kill(); process.exit(1); });
