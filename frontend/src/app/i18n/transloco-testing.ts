import { TranslocoTestingModule, TranslocoTestingOptions } from '@jsverse/transloco';
import en from '../../assets/i18n/en.json';
import es from '../../assets/i18n/es.json';

/**
 * Test helper that makes the REAL translation dictionaries available synchronously
 * in a TestBed, so components using the `transloco` pipe / `localizeLink` pipe and
 * services calling `TranslocoService.translate(...)` resolve to actual English text
 * (default lang) during unit tests. Add it to a spec's `imports: [...]`.
 */
export function translocoTesting(options: TranslocoTestingOptions = {}) {
  return TranslocoTestingModule.forRoot({
    langs: { en, es },
    translocoConfig: { availableLangs: ['en', 'es'], defaultLang: 'en' },
    preloadLangs: true,
    ...options,
  });
}
