// Aggregate a CDP .cpuprofile by self-time per function. Usage: node analyze.mjs <file> [topN]
import { readFileSync } from 'node:fs';
const p = JSON.parse(readFileSync(process.argv[2], 'utf8'));
const topN = Number(process.argv[3] ?? 25);
const nodeById = new Map(p.nodes.map((n) => [n.id, n]));
const selfUs = new Map();
for (let i = 0; i < p.samples.length; i++) {
  const id = p.samples[i];
  selfUs.set(id, (selfUs.get(id) || 0) + p.timeDeltas[i]);
}
const short = (u) => (u || '').split('/').pop().split('?')[0];
const byFn = new Map();
for (const [id, us] of selfUs) {
  const n = nodeById.get(id);
  if (!n) continue;
  const f = n.callFrame;
  const key = `${(f.functionName || '(anonymous)').padEnd(34)} ${short(f.url)}:${f.lineNumber + 1}`;
  byFn.set(key, (byFn.get(key) || 0) + us);
}
const total = [...selfUs.values()].reduce((a, b) => a + b, 0);
console.log(`\n# ${process.argv[2].split('/').pop()} — ${(total / 1000).toFixed(0)}ms self-time, ${p.samples.length} samples`);
const top = [...byFn].sort((a, b) => b[1] - a[1]).slice(0, topN);
for (const [k, us] of top) {
  console.log(`${(us / 1000).toFixed(1).padStart(8)}ms ${((100 * us) / total).toFixed(1).padStart(5)}%  ${k}`);
}
