import { LEARN_CHARACTERS } from './learn-characters';

describe('learn-characters', () => {

  it('has exactly 12 unique starter characters', () => {
    expect(LEARN_CHARACTERS.length).toBe(12);
    const hanzi = LEARN_CHARACTERS.map(c => c.hanzi);
    expect(new Set(hanzi).size).toBe(hanzi.length);
  });

  it('every character is fully bilingual with example word and audio', () => {
    for (const c of LEARN_CHARACTERS) {
      expect(c.hanzi.length).withContext(c.pinyin).toBe(1);
      expect(c.pinyin.length).toBeGreaterThan(0);
      expect(c.meaningEn.length).toBeGreaterThan(0);
      expect(c.meaningEs.length).toBeGreaterThan(0);
      expect(c.exampleHanzi.length).toBeGreaterThan(0);
      expect(c.examplePinyin.length).toBeGreaterThan(0);
      expect(c.exampleMeaningEn.length).toBeGreaterThan(0);
      expect(c.exampleMeaningEs.length).toBeGreaterThan(0);
      expect(c.audio).toMatch(/^[a-z0-9]+\.mp3$/);
      expect(c.exampleAudio).toMatch(/^[a-z0-9]+\.mp3$/);
      // The example word must actually contain the character being taught.
      expect(c.exampleHanzi).withContext(c.hanzi).toContain(c.hanzi);
    }
  });
});
