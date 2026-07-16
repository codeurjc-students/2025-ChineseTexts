/**
 * Sentence helpers shared by the public text reader (text.component) and the
 * private reader (my-text-reader.component), so both split sentences and lay
 * out the full translation with exactly the same rules.
 */

/** Sentence-final terminators — the same set the backend uses to build sentences. */
const TERMINATORS = '。！？…；.!?;';

/** True when the token ends a sentence (its last char is a terminator). */
export function endsSentence(token: string): boolean {
  return !!token && TERMINATORS.includes(token.charAt(token.length - 1));
}

/** Splits a translated block into sentences (after a terminator followed by space/end). */
export function splitTranslatedSentences(text: string): string[] {
  return (text || '').split(/(?<=[。！？…；.!?;])(?=\s|$)/).filter(s => s.trim() !== '');
}

/**
 * Break-after flags per sentence, derived from the token stream with '\n'
 * layout markers. A line break only counts when it falls BETWEEN sentences
 * (right after a terminator): breaks inside a sentence — a speaker label like
 * 王芳: on its own line, or a cosmetic wrap in the photo — have no equivalent
 * position in the translation, so they are ignored here (the words view still
 * shows them). A text without '\n' tokens returns no flags at all.
 */
export function sentenceBreaksFromTokens(tokens: string[]): boolean[] {
  const breaks: boolean[] = [];
  let sentence = 0;   // index of the sentence being built
  let open = false;   // tokens accumulated in the current sentence?
  for (const token of tokens) {
    if (token === '\n') {
      if (!open && sentence > 0) breaks[sentence - 1] = true;
      continue;
    }
    open = true;
    if (endsSentence(token)) {
      sentence++;
      open = false;
    }
  }
  return breaks;
}

/**
 * Groups per-sentence translations into display lines: sentences sharing a
 * line in the original stay together separated by a space; a break flag ends
 * the line. Empty translations are skipped without leaving blank lines.
 */
export function groupTranslationLines(sentences: string[], breaks: boolean[]): string[] {
  const lines: string[] = [];
  let current: string[] = [];
  sentences.forEach((s, i) => {
    if (s) current.push(s);
    if (breaks[i] && current.length) {
      lines.push(current.join(' '));
      current = [];
    }
  });
  if (current.length) lines.push(current.join(' '));
  return lines;
}
