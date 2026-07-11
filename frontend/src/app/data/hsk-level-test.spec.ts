import {
  QUESTION_BANK, MIN_LEVEL, MAX_LEVEL, nextLevel, estimateLevel
} from './hsk-level-test';

describe('hsk-level-test', () => {

  it('question bank has 10 well-formed questions per level', () => {
    for (let level = 1; level <= 6; level++) {
      const of = QUESTION_BANK.filter(q => q.level === level);
      expect(of.length).withContext(`HSK${level}`).toBe(10);
    }
    for (const q of QUESTION_BANK) {
      expect(q.chinese.length).toBeGreaterThan(0);
      expect(q.wrongEn.length).toBe(3);
      expect(q.wrongEs.length).toBe(3);
      // The correct answer must never be duplicated among its distractors.
      expect(q.wrongEn).not.toContain(q.correctEn);
      expect(q.wrongEs).not.toContain(q.correctEs);
    }
  });

  it('staircase climbs on correct and drops on wrong, clamped to 1..6', () => {
    expect(nextLevel(2, true)).toBe(3);
    expect(nextLevel(2, false)).toBe(1);
    expect(nextLevel(MAX_LEVEL, true)).toBe(MAX_LEVEL);
    expect(nextLevel(MIN_LEVEL, false)).toBe(MIN_LEVEL);
  });

  it('estimate takes the median of the converged tail', () => {
    // Climbed early, then oscillated around 4: the estimate should be 4.
    expect(estimateLevel([2, 3, 4, 5, 4, 5, 4, 3, 4, 5, 4, 4])).toBe(4);
    // All answers wrong: stays at the floor.
    expect(estimateLevel([2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1])).toBe(1);
    // All answers right: reaches the ceiling.
    expect(estimateLevel([2, 3, 4, 5, 6, 6, 6, 6, 6, 6, 6, 6])).toBe(6);
    expect(estimateLevel([])).toBe(MIN_LEVEL);
  });
});
