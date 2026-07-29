import { SAMPLE_INITIALS, SAMPLE_FINALS } from './learn-pinyin';

describe('learn-pinyin', () => {

  it('sample tables have the expected sizes and unique symbols', () => {
    expect(SAMPLE_INITIALS.length).toBe(8);
    expect(SAMPLE_FINALS.length).toBe(6);
    const symbols = [...SAMPLE_INITIALS, ...SAMPLE_FINALS].map(s => s.symbol);
    expect(new Set(symbols).size).toBe(symbols.length);
  });

  it('every sound is fully bilingual with a well-formed audio file', () => {
    for (const s of [...SAMPLE_INITIALS, ...SAMPLE_FINALS]) {
      expect(s.soundsLikeEn.length).withContext(s.symbol).toBeGreaterThan(0);
      expect(s.soundsLikeEs.length).withContext(s.symbol).toBeGreaterThan(0);
      expect(s.exampleSyllable.length).toBeGreaterThan(0);
      expect(s.exampleHanzi.length).toBeGreaterThan(0);
      expect(s.exampleMeaningEn.length).toBeGreaterThan(0);
      expect(s.exampleMeaningEs.length).toBeGreaterThan(0);
      expect(s.audio).toMatch(/^[a-z0-9]+\.mp3$/);
    }
  });
});
