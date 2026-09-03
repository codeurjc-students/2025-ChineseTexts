/**
 * One-time generator for the /learn tutorial audio assets.
 *
 * Synthesizes every mp3 under src/assets/audio/learn/ with Google Cloud
 * Text-to-Speech, using the SAME voice as the app's TTS service
 * (tts-service/ttsService.py: cmn-CN-Wavenet-A, MP3, speakingRate 0.9) so the
 * tutorial sounds identical to the readers. The files are committed to git and
 * served as static assets — zero per-visit API cost, no login needed.
 *
 * Usage (from frontend/), with either credential type:
 *   - Service account JSON (the usual credentials.json):
 *       GOOGLE_TTS_CREDENTIALS=/path/to/credentials.json node scripts/generate-learn-audio.mjs
 *   - Or a plain API key:
 *       GOOGLE_TTS_API_KEY=<your-key> node scripts/generate-learn-audio.mjs
 *
 * Idempotent: existing files are skipped, so a partial run can just be re-run.
 * Afterwards run `npm run check:learn-audio` to verify the assets match the lessons.
 */

import { mkdir, writeFile, access, readFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createSign } from 'node:crypto';

const OUT_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'assets', 'audio', 'learn');
const API_KEY = process.env.GOOGLE_TTS_API_KEY;
const CREDENTIALS_FILE = process.env.GOOGLE_TTS_CREDENTIALS;

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
  // Pinyin sound chart (/learn/pinyin) — one example word per sound that the
  // tones/characters lessons do not already provide
  { file: 'ba1.mp3', text: '八' },
  { file: 'he1.mp3', text: '喝' },
  { file: 'qi1.mp3', text: '七' },
  { file: 'cai4.mp3', text: '菜' },
  { file: 'wu3.mp3', text: '五' },
  { file: 'pao3.mp3', text: '跑' },
  { file: 'fan4.mp3', text: '饭' },
  { file: 'gou3.mp3', text: '狗' },
  { file: 'kan4.mp3', text: '看' },
  { file: 'jia1.mp3', text: '家' },
  { file: 'chi1.mp3', text: '吃' },
  { file: 'zi4.mp3', text: '字' },
  { file: 'ai4.mp3', text: '爱' },
  { file: 'bei3.mp3', text: '北' },
  { file: 'liu4.mp3', text: '六' },
  { file: 'xie4.mp3', text: '谢' },
  { file: 'yue4.mp3', text: '月' },
  { file: 'xin1.mp3', text: '新' },
  { file: 'chun1.mp3', text: '春' },
  { file: 'yun2.mp3', text: '云' },
  { file: 'mang2.mp3', text: '忙' },
  { file: 'leng3.mp3', text: '冷' },
];

async function exists(path) {
  try { await access(path); return true; } catch { return false; }
}

/**
 * Exchanges a service-account JSON for a short-lived OAuth2 access token
 * (standard JWT-bearer flow, no dependencies needed).
 */
async function accessTokenFromServiceAccount(file) {
  const sa = JSON.parse(await readFile(file, 'utf8'));
  if (!sa.client_email || !sa.private_key) {
    throw new Error(`${file} does not look like a service-account JSON (missing client_email/private_key)`);
  }
  const b64url = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url');
  const iat = Math.floor(Date.now() / 1000);
  const unsigned =
    b64url({ alg: 'RS256', typ: 'JWT' }) + '.' +
    b64url({
      iss: sa.client_email,
      scope: 'https://www.googleapis.com/auth/cloud-platform',
      aud: 'https://oauth2.googleapis.com/token',
      iat,
      exp: iat + 3600,
    });
  const signature = createSign('RSA-SHA256').update(unsigned).sign(sa.private_key, 'base64url');
  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: `${unsigned}.${signature}`,
    }),
  });
  if (!res.ok) {
    throw new Error(`Token exchange failed (HTTP ${res.status}): ${(await res.text()).slice(0, 300)}`);
  }
  return (await res.json()).access_token;
}

let accessToken = null;

async function synthesize(text) {
  const url = API_KEY
    ? `https://texttospeech.googleapis.com/v1/text:synthesize?key=${API_KEY}`
    : 'https://texttospeech.googleapis.com/v1/text:synthesize';
  const headers = { 'Content-Type': 'application/json' };
  if (!API_KEY) headers['Authorization'] = `Bearer ${accessToken}`;
  const res = await fetch(url, {
    method: 'POST',
    headers,
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

if (!API_KEY && !CREDENTIALS_FILE) {
  console.error('Missing credentials. Provide ONE of:');
  console.error('  GOOGLE_TTS_CREDENTIALS=/path/to/credentials.json node scripts/generate-learn-audio.mjs');
  console.error('  GOOGLE_TTS_API_KEY=<your-key> node scripts/generate-learn-audio.mjs');
  process.exit(1);
}
if (!API_KEY) {
  accessToken = await accessTokenFromServiceAccount(CREDENTIALS_FILE);
  console.log('Authenticated with service account.\n');
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
