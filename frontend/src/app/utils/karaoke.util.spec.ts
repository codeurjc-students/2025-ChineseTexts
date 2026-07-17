import { karaokeWeights, karaokeIndexAt } from './karaoke.util';

describe('karaoke.util', () => {

  describe('karaokeWeights', () => {
    it('weighs one unit per hanzi and zero for layout tokens', () => {
      const w = karaokeWeights(['你好', '\n', '再见']);
      expect(w[0]).toBe(2);
      expect(w[1]).toBe(0);
      expect(w[2]).toBe(2);
    });

    it('gives punctuation a pause weight smaller than a syllable', () => {
      const [full, comma] = karaokeWeights(['。', '，']);
      expect(full).toBeGreaterThan(0);
      expect(full).toBeLessThan(1);
      expect(comma).toBeGreaterThan(0);
      expect(comma).toBeLessThan(full); // clause pause shorter than sentence pause
    });
  });

  describe('karaokeIndexAt', () => {
    // 我(1) 每天(2) 七点(2) 起床(2) 。(0.8) — total 7.8
    const tokens = ['我', '每天', '七点', '起床', '。'];

    it('maps the start of playback to the first word', () => {
      expect(karaokeIndexAt(0, tokens)).toBe(0);
      expect(karaokeIndexAt(0.05, tokens)).toBe(0);
    });

    it('advances through the words proportionally to their length', () => {
      expect(karaokeIndexAt(0.3, tokens)).toBe(1);  // target 2.34 → inside 每天 (1..3)
      expect(karaokeIndexAt(0.6, tokens)).toBe(2);  // target 4.68 → inside 七点 (3..5)
      expect(karaokeIndexAt(0.8, tokens)).toBe(3);  // target 6.24 → inside 起床 (5..7)
    });

    it('keeps the highlight on the last word while the voice pauses at punctuation', () => {
      // target lands inside the final 。 token → snapped back to 起床.
      expect(karaokeIndexAt(0.95, tokens)).toBe(3);
    });

    it('skips layout tokens: a \\n never gets highlighted', () => {
      const dialogue = ['你好', '\n', '再见'];
      // Second half of the audio is 再见 (index 2), never the '\n' (index 1).
      expect(karaokeIndexAt(0.75, dialogue)).toBe(2);
      expect(karaokeIndexAt(0.25, dialogue)).toBe(0);
    });

    it('returns -1 at or past the end, and for empty/unspeakable input', () => {
      expect(karaokeIndexAt(1, tokens)).toBe(-1);
      expect(karaokeIndexAt(1.5, tokens)).toBe(-1);
      expect(karaokeIndexAt(0.5, [])).toBe(-1);
      expect(karaokeIndexAt(0.5, ['\n', '\n'])).toBe(-1);
    });

    it('accepts precomputed weights (same result as computing them inline)', () => {
      const w = karaokeWeights(tokens);
      expect(karaokeIndexAt(0.6, tokens, w)).toBe(karaokeIndexAt(0.6, tokens));
    });
  });
});
