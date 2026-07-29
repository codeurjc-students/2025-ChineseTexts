import { TONES, TONE_QUIZ, QUIZ_TONE_OPTIONS, quizResultKey } from './learn-tones';

describe('learn-tones', () => {

  it('tone table covers tones 1..5 exactly once, fully bilingual', () => {
    expect(TONES.length).toBe(5);
    expect(TONES.map(t => t.tone)).toEqual([1, 2, 3, 4, 5]);
    for (const t of TONES) {
      expect(t.syllable.length).withContext(`tone ${t.tone}`).toBeGreaterThan(0);
      expect(t.hanzi.length).toBeGreaterThan(0);
      expect(t.meaningEn.length).toBeGreaterThan(0);
      expect(t.meaningEs.length).toBeGreaterThan(0);
      expect(t.contourEn.length).toBeGreaterThan(0);
      expect(t.contourEs.length).toBeGreaterThan(0);
      expect(t.exampleHanzi.length).toBeGreaterThan(0);
      expect(t.examplePinyin.length).toBeGreaterThan(0);
      expect(t.exampleMeaningEn.length).toBeGreaterThan(0);
      expect(t.exampleMeaningEs.length).toBeGreaterThan(0);
      expect(t.audio).toMatch(/^[a-z0-9]+\.mp3$/);
      expect(t.exampleAudio).toMatch(/^[a-z0-9]+\.mp3$/);
    }
  });

  it('quiz bank has 10 balanced, well-formed items', () => {
    expect(TONE_QUIZ.length).toBe(10);
    for (const tone of QUIZ_TONE_OPTIONS) {
      const count = TONE_QUIZ.filter(q => q.tone === tone).length;
      expect(count).withContext(`tone ${tone}`).toBeGreaterThanOrEqual(2);
    }
    // Distinct syllables — hearing the same one twice would feel like a bug.
    const bases = TONE_QUIZ.map(q => q.base);
    expect(new Set(bases).size).toBe(bases.length);
    for (const q of TONE_QUIZ) {
      expect(q.base.length).toBeGreaterThan(0);
      expect(q.syllable.length).toBeGreaterThan(0);
      expect(q.hanzi.length).toBeGreaterThan(0);
      // The audio file is the toneless base + the tone digit.
      expect(q.audio).withContext(q.base).toBe(`${q.base}${q.tone}.mp3`);
    }
  });

  it('quiz score buckets to the right result key', () => {
    expect(quizResultKey(10, 10)).toBe('perfect');
    expect(quizResultKey(9, 10)).toBe('perfect');
    expect(quizResultKey(8, 10)).toBe('good');
    expect(quizResultKey(6, 10)).toBe('good');
    expect(quizResultKey(5, 10)).toBe('keep');
    expect(quizResultKey(0, 10)).toBe('keep');
    expect(quizResultKey(0, 0)).toBe('keep');
  });
});
