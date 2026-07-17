/**
 * Karaoke helpers shared by both readers: while the full-text audio plays, the
 * word currently being spoken is highlighted.
 *
 * The sync is ESTIMATED client-side instead of using Google TTS timepoints on
 * purpose: SSML `<mark>` tags are billed as characters (a mark per word would
 * multiply the cost of every audio ~10×) and would break the byte-identical
 * TTS cache. Mandarin TTS is close to syllable-timed — each hanzi takes a
 * near-constant slice of audio, with pauses at punctuation — so a weighted
 * character count over the token list tracks the voice closely at zero cost.
 */

const CJK_RE = /[㐀-鿿]/;
/** Sentence-final punctuation: the voice makes a long pause. */
const MAJOR_PAUSE = '。！？…；.!?;';
/** Clause punctuation: a shorter pause. */
const MINOR_PAUSE = '，、：,:';

/** Estimated speech-time weight of one character. */
function charWeight(ch: string): number {
  if (CJK_RE.test(ch)) return 1;          // one syllable
  if (MAJOR_PAUSE.includes(ch)) return 0.8;
  if (MINOR_PAUSE.includes(ch)) return 0.4;
  if (/\s/.test(ch)) return 0;            // includes '\n' layout tokens
  if (/[0-9A-Za-z]/.test(ch)) return 0.35; // digits/latin are read faster per char
  return 0.1;                              // quotes, brackets, dashes…
}

/** Estimated speech-time weight of each token (sum of its characters). */
export function karaokeWeights(tokens: string[]): number[] {
  return tokens.map(t => {
    let w = 0;
    for (const ch of t || '') w += charWeight(ch);
    return w;
  });
}

/** True when the token contains something the voice actually pronounces. */
function isSpeakable(token: string): boolean {
  return /[㐀-鿿0-9A-Za-z]/.test(token || '');
}

/**
 * Maps a playback position (`ratio` = currentTime / duration, 0..1) to the
 * index of the token being spoken. Pause-only tokens (a lone 。 or ，) keep the
 * highlight on the previous word — the voice is finishing that word's pause,
 * and highlighting punctuation looks broken. Returns -1 when nothing sensible
 * can be highlighted (no tokens, ratio past the end).
 */
export function karaokeIndexAt(ratio: number, tokens: string[], weights?: number[]): number {
  const w = weights ?? karaokeWeights(tokens);
  const total = w.reduce((a, b) => a + b, 0);
  if (total <= 0 || ratio >= 1) return -1;

  const target = Math.max(0, ratio) * total;
  let acc = 0;
  let index = -1;
  for (let i = 0; i < tokens.length; i++) {
    acc += w[i];
    if (target < acc) { index = i; break; }
  }
  if (index === -1) return -1;

  // Snap a pause-only token back to the last speakable word.
  while (index >= 0 && !isSpeakable(tokens[index])) index--;
  return index;
}
