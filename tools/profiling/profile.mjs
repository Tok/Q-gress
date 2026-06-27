// Headless-Chrome CDP profiler for Q-Gress. No deps (Node 22+ global WebSocket/fetch).
// Attaches BEFORE navigating so world-gen is captured from t=0; waits for the "WORLD READY"
// console marker (logged by Profiler) — or genTimeout — before the steady-state runtime window.
// Records two CPU profiles (worldgen + runtime) and the console log into outDir.
//
// Usage: node profile.mjs "<url>" <genTimeoutSec> <runtimeSec> <outDir>
// Env:  CHROME_BIN (default google-chrome), WINDOW (default 1600,1000), CDP_PORT (default 9222)
import { spawn } from 'node:child_process';
import { writeFileSync } from 'node:fs';

const URL = process.argv[2];
const GEN_TIMEOUT = Number(process.argv[3] ?? 320);
const RUN_S = Number(process.argv[4] ?? 22);
const OUT = process.argv[5] ?? '/tmp';
const CHROME = process.env.CHROME_BIN ?? '/usr/bin/google-chrome';
const WINDOW = process.env.WINDOW ?? '1600,1000';
const PORT = Number(process.env.CDP_PORT ?? 9222);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// Software WebGL (swiftshader) so it runs without a GPU. NB: render/FPS headless is NOT
// representative of a real GPU — trust the world-gen profile + the sim-function self-times,
// not the headless frame rate.
const chrome = spawn(CHROME, [
  '--headless=new', `--remote-debugging-port=${PORT}`, `--window-size=${WINDOW}`,
  '--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader',
  '--no-sandbox', '--disable-dev-shm-usage', '--mute-audio', '--no-first-run',
  '--user-data-dir=/tmp/qgress-chrome-prof', 'about:blank',
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

const consoleLines = [];
let ready = false;
const main = async () => {
  const ws = new WebSocket(await pageWs());
  await new Promise((r) => (ws.onopen = r));
  ws.onmessage = (e) => {
    const m = JSON.parse(e.data);
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m.result); pending.delete(m.id); return; }
    if (m.method === 'Runtime.consoleAPICalled') {
      const txt = (m.params.args || []).map((a) => a.value ?? a.description ?? '').join(' ');
      consoleLines.push(`[${m.params.type}] ${txt}`);
      if (/\[perf\]|grid built|WORLD READY|island|UNHEALTHY|error/i.test(txt)) console.log('  ·', txt);
      if (txt.includes('WORLD READY')) ready = true;
    }
    if (m.method === 'Runtime.exceptionThrown') {
      console.log('  ! EXC', String(m.params.exceptionDetails?.exception?.description || '').slice(0, 200));
    }
  };
  await send(ws, 'Runtime.enable');
  await send(ws, 'Page.enable');
  await send(ws, 'Profiler.enable');
  await send(ws, 'Profiler.setSamplingInterval', { interval: 150 });

  console.log(`\n== world-gen (navigate; wait for WORLD READY, <=${GEN_TIMEOUT}s) ==`);
  await send(ws, 'Profiler.start');
  const t0 = Date.now();
  await send(ws, 'Page.navigate', { url: URL });
  while (!ready && (Date.now() - t0) / 1000 < GEN_TIMEOUT) await sleep(500);
  const genSec = ((Date.now() - t0) / 1000).toFixed(1);
  writeFileSync(`${OUT}/worldgen.cpuprofile`, JSON.stringify((await send(ws, 'Profiler.stop')).profile));
  console.log(`  world-gen ${ready ? 'READY' : 'TIMEOUT'} after ${genSec}s -> worldgen.cpuprofile`);

  console.log(`\n== runtime window (${RUN_S}s) ==`);
  await send(ws, 'Profiler.start');
  await sleep(RUN_S * 1000);
  writeFileSync(`${OUT}/runtime.cpuprofile`, JSON.stringify((await send(ws, 'Profiler.stop')).profile));
  console.log('  saved runtime.cpuprofile');

  writeFileSync(`${OUT}/console.log`, consoleLines.join('\n'));
  console.log(`  console: ${consoleLines.length} lines -> console.log`);
  ws.close();
  chrome.kill();
  process.exit(0);
};
main().catch((e) => { console.error(e); chrome.kill(); process.exit(1); });
