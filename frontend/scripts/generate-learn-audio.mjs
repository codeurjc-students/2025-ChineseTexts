/**
 * One-time generator for the /learn tutorial audio assets.
 *
 * Synthesizes every mp3 under src/assets/audio/learn/ with Google Cloud
 * Text-to-Speech, using the SAME voice as the app's TTS service
 * (tts-service/ttsService.py: cmn-CN-Wavenet-A, MP3, speakingRate 0.9) so the
 * tutorial sounds identical to the readers. The files are committed to git and
 * served as static assets — zero per-visit API cost, no login needed.
 *
 * Usage (from frontend/):
 *   GOOGLE_TTS_API_KEY=<your-key> node scripts/generate-learn-audio.mjs
 *
 * Idempotent: existing files are skipped, so a partial run can just be re-run.
 */

import { mkdir, writeFile, access } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const OUT_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'assets', 'audio', 'learn');
const API_KEY = process.env.GOOGLE_TTS_API_KEY;

// Filename = pinyin without diacritics + tone digit per syllable (neutral = 5).
// Text is always hanzi so Google resolves pronunciation (incl. tone sandhi) itself.
const MANIFEST = [
  // The classic "ma" tone set (/learn/tones)
  { file: 'ma1.mp3', text: '妈' },
  { file: 'ma2.mp3', text: '麻' },
  { file: 'ma3.mp3', text: '马' },
  { file: 'ma4.mp3', text: '骂' },
  { file: 'ma5.mp3', text: '吗' },
  // Tone example words
  { file: 'tian1.mp3', text: '天' },
  { file: 'ma1ma5.mp3', text: '妈妈' },
  // Ear-training quiz syllables
  { file: 'lai2.mp3', text: '来' },
  { file: 'mai3.mp3', text: '买' },
  { file: 'shu1.mp3', text: '书' },
  { file: 'ren2.mp3', text: '人' },
  { file: 'ba4.mp3', text: '爸' },
  { file: 'ting1.mp3', text: '听' },
  { file: 'yu2.mp3', text: '鱼' },
  // The 12 starter characters (/learn/characters)
  { file: 'wo3.mp3', text: '我' },
  { file: 'ni3.mp3', text: '你' },
  { file: 'hao3.mp3', text: '好' },
  { file: 'shi4.mp3', text: '是' },
  { file: 'da4.mp3', text: '大' },
  { file: 'xiao3.mp3', text: '小' },
  { file: 'shui3.mp3', text: '水' },
  { file: 'yi1.mp3', text: '一' },
  { file: 'er4.mp3', text: '二' },
  { file: 'san1.mp3', text: '三' },
  { file: 'zhong1.mp3', text: '中' },
  // Example words of the starter characters
  { file: 'wo3men5.mp3', text: '我们' },
  { file: 'ni3hao3.mp3', text: '你好' },
  { file: 'hen3hao3.mp3', text: '很好' },
  { file: 'wo3shi4.mp3', text: '我是' },
  { file: 'zhong1guo2ren2.mp3', text: '中国人' },
  { file: 'da4ren2.mp3', text: '大人' },
  { file: 'da4xiao3.mp3', text: '大小' },
  { file: 'he1shui3.mp3', text: '喝水' },
  { file: 'yi1ge4.mp3', text: '一个' },
  { file: 'er4shi2.mp3', text: '二十' },
  { file: 'san1ge4.mp3', text: '三个' },
  { file: 'zhong1guo2.mp3', text: '中国' },
  // Pinyin lesson extras (/learn/pinyin)
  { file: 'ba1.mp3', text: '八' },
  { file: 'he1.mp3', text: '喝' },
  { file: 'qi1.mp3', text: '七' },
  { file: 'cai4.mp3', text: '菜' },
];

async function exists(path) {
  try { await access(path); return true; } catch { return false; }
}

async function synthesize(text) {
  const res = await fetch(`https://texttospeech.googleapis.com/v1/text:synthesize?key=${API_KEY}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      input: { text },
      voice: { languageCode: 'cmn-CN', name: 'cmn-CN-Wavenet-A' },
      audioConfig: { audioEncoding: 'MP3', speakingRate: 0.9 },
    }),
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${(await res.text()).slice(0, 300)}`);
  }
  const { audioContent } = await res.json();
  return Buffer.from(audioContent, 'base64');
}

if (!API_KEY) {
  console.error('Missing GOOGLE_TTS_API_KEY environment variable.');
  console.error('Usage: GOOGLE_TTS_API_KEY=<your-key> node scripts/generate-learn-audio.mjs');
  process.exit(1);
}

await mkdir(OUT_DIR, { recursive: true });
let ok = 0, skipped = 0, failed = 0;

for (const { file, text } of MANIFEST) {
  const path = join(OUT_DIR, file);
  if (await exists(path)) {
    skipped++;
    console.log(`skip  ${file} (already exists)`);
    continue;
  }
  try {
    const mp3 = await synthesize(text);
    await writeFile(path, mp3);
    ok++;
    console.log(`ok    ${file}  ←  ${text}`);
  } catch (err) {
    failed++;
    console.error(`FAIL  ${file}  ←  ${text}: ${err.message}`);
  }
  // Be gentle with the API.
  await new Promise(r => setTimeout(r, 250));
}

console.log(`\nDone: ${ok} generated, ${skipped} skipped, ${failed} failed (${MANIFEST.length} total).`);
if (failed > 0) process.exit(1);
