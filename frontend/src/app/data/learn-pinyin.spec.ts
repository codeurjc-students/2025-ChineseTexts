import {
  SIMPLE_VOWELS, CONSONANT_GROUPS, CONSONANTS, DIPHTHONGS, NASAL_FINALS, ALL_PINYIN_SOUNDS
} from './learn-pinyin';

describe('learn-pinyin', () => {

  it('follows the school chart exactly: vowels, consonants, diphthongs, nasal finals', () => {
    expect(SIMPLE_VOWELS.map(s => s.symbol)).toEqual(['a', 'o', 'e', 'i', 'u', 'ü']);
    expect(CONSONANTS.map(s => s.symbol)).toEqual([
      'b', 'p', 'm', 'f', 'd', 't', 'n', 'l', 'g', 'k', 'h', 'j', 'q', 'x',
      'zh', 'ch', 'sh', 'r', 'z', 'c', 's', 'y', 'w'
    ]);
    expect(CONSONANT_GROUPS.map(g => g.length)).toEqual([4, 4, 3, 3, 4, 3, 2]);
    expect(DIPHTHONGS.map(s => s.symbol)).toEqual(['ai', 'ei', 'ui', 'ao', 'ou', 'iu', 'ie', 'üe']);
    expect(NASAL_FINALS.map(s => s.symbol)).toEqual(['an', 'en', 'in', 'un', 'ün', 'ang', 'eng', 'ing', 'ong']);
    expect(ALL_PINYIN_SOUNDS.length).toBe(6 + 23 + 8 + 9);
  });

  it('has unique symbols inside each table', () => {
    for (const table of [SIMPLE_VOWELS, CONSONANTS, DIPHTHONGS, NASAL_FINALS]) {
      const symbols = table.map(s => s.symbol);
      expect(new Set(symbols).size).toBe(symbols.length);
    }
  });

  it('every sound is fully bilingual with a well-formed audio file', () => {
    for (const s of ALL_PINYIN_SOUNDS) {
      expect(s.soundsLikeEn.length).withContext(s.symbol).toBeGreaterThan(0);
      expect(s.soundsLikeEs.length).withContext(s.symbol).toBeGreaterThan(0);
      expect(s.exampleSyllable.length).withContext(s.symbol).toBeGreaterThan(0);
      expect(s.exampleHanzi.length).withContext(s.symbol).toBeGreaterThan(0);
      expect(s.exampleMeaningEn.length).withContext(s.symbol).toBeGreaterThan(0);
      expect(s.exampleMeaningEs.length).withContext(s.symbol).toBeGreaterThan(0);
      expect(s.audio).withContext(s.symbol).toMatch(/^[a-z0-9]+\.mp3$/);
    }
  });

  it('the example word really contains the sound it illustrates', () => {
    const TONELESS: Record<string, string> = {
      'ā': 'a', 'á': 'a', 'ǎ': 'a', 'à': 'a',
      'ō': 'o', 'ó': 'o', 'ǒ': 'o', 'ò': 'o',
      'ē': 'e', 'é': 'e', 'ě': 'e', 'è': 'e',
      'ī': 'i', 'í': 'i', 'ǐ': 'i', 'ì': 'i',
      'ū': 'u', 'ú': 'u', 'ǔ': 'u', 'ù': 'u',
      'ǖ': 'ü', 'ǘ': 'ü', 'ǚ': 'ü', 'ǜ': 'ü'
    };
    const toneless = (p: string) => [...p].map(c => TONELESS[c] ?? c).join('');
    // yi / wu / yu are how i / u / ü are spelled at the start of a syllable
    const STANDALONE: Record<string, string> = { i: 'yi', u: 'wu', 'ü': 'yu', 'üe': 'yue', 'ün': 'yun' };
    for (const s of ALL_PINYIN_SOUNDS) {
      const syl = toneless(s.exampleSyllable);
      const ok = syl.includes(s.symbol) || syl.includes(STANDALONE[s.symbol] ?? '\u0000');
      expect(ok).withContext(`${s.symbol} → ${s.exampleSyllable}`).toBeTrue();
    }
  });
});
