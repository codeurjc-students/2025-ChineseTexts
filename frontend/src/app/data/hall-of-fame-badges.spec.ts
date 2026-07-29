import { HALL_OF_FAME_BADGES } from './hall-of-fame-badges';

import en from '../../assets/i18n/en.json';
import es from '../../assets/i18n/es.json';

describe('hall-of-fame-badges', () => {

  it('catalog is well-formed: unique keys and Bootstrap Icons classes', () => {
    expect(HALL_OF_FAME_BADGES.length).toBeGreaterThan(0);
    const keys = HALL_OF_FAME_BADGES.map(b => b.key);
    expect(new Set(keys).size).toBe(keys.length);
    for (const badge of HALL_OF_FAME_BADGES) {
      expect(badge.key).withContext(badge.key).toMatch(/^[a-z]+(-[a-z]+)*$/);
      expect(badge.icon).withContext(badge.key).toMatch(/^bi-[a-z0-9-]+$/);
    }
  });

  it('every badge has label and desc in both i18n dictionaries', () => {
    for (const badge of HALL_OF_FAME_BADGES) {
      for (const [lang, dict] of [['en', en], ['es', es]] as const) {
        const entry = (dict as any).hofBadges?.[badge.key];
        expect(entry?.label?.length).withContext(`${badge.key} (${lang}) label`).toBeGreaterThan(0);
        expect(entry?.desc?.length).withContext(`${badge.key} (${lang}) desc`).toBeGreaterThan(0);
      }
    }
  });
});
