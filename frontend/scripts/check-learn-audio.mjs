/**
 * Verifies that the /learn tutorial audio assets are exactly what the lessons
 * need — no missing file, no orphan file, nothing left out of the generator:
 *
 *   referenced  = every '<name>.mp3' used by src/app/data/learn-*.ts and the
 *                 learn-* component templates
 *   manifest    = every entry of MANIFEST in generate-learn-audio.mjs
 *   on disk     = src/assets/audio/learn/*.mp3
 *
 * The three sets must be identical. Exit code 1 otherwise.
 * Usage (from frontend/): npm run check:learn-audio
 */

import { readdir, readFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const AUDIO_DIR = join(ROOT, 'src', 'assets', 'audio', 'learn');
const MP3 = /\b([a-z0-9]+\.mp3)\b/g;

async function filesUnder(dir, filter) {
  const out = [];
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) out.push(...await filesUnder(path, filter));
    else if (filter(entry.name)) out.push(path);
  }
  return out;
}

const referenced = new Set();
const sources = [
  ...await filesUnder(join(ROOT, 'src', 'app', 'data'), n => /^learn-.*\.ts$/.test(n) && !n.endsWith('.spec.ts')),
  ...await filesUnder(join(ROOT, 'src', 'app', 'components'), n => n.endsWith('.html')),
];
for (const file of sources) {
  if (file.includes('/components/') && !file.includes('/learn-')) continue;
  for (const m of (await readFile(file, 'utf8')).matchAll(MP3)) referenced.add(m[1]);
}

const manifest = new Set();
for (const m of (await readFile(join(ROOT, 'scripts', 'generate-learn-audio.mjs'), 'utf8')).matchAll(/file: '([a-z0-9]+\.mp3)'/g)) {
  manifest.add(m[1]);
}

const onDisk = new Set((await readdir(AUDIO_DIR).catch(() => [])).filter(n => n.endsWith('.mp3')));

const diff = (a, b) => [...a].filter(x => !b.has(x)).sort();
const problems = [
  ['referenced by a lesson but NOT in the generator manifest', diff(referenced, manifest)],
  ['in the generator manifest but NOT used by any lesson', diff(manifest, referenced)],
  ['referenced by a lesson but MISSING on disk (run generate-learn-audio.mjs)', diff(referenced, onDisk)],
  ['on disk but NOT used by any lesson (orphan, delete it)', diff(onDisk, referenced)],
].filter(([, list]) => list.length > 0);

console.log(`referenced: ${referenced.size}  manifest: ${manifest.size}  on disk: ${onDisk.size}`);
if (problems.length === 0) {
  console.log('OK — audio assets match the lessons exactly.');
} else {
  for (const [label, list] of problems) console.error(`\n${label}:\n  ${list.join('\n  ')}`);
  process.exit(1);
}
