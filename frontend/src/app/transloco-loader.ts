import { Injectable } from '@angular/core';
import { Translation, TranslocoLoader } from '@jsverse/transloco';
import { of } from 'rxjs';

import en from '../assets/i18n/en.json';
import es from '../assets/i18n/es.json';

/**
 * Synchronous, in-bundle translation loader.
 *
 * The dictionaries are imported STATICALLY (not fetched over HTTP), so they are
 * part of the JS bundle and available immediately. `getTranslation` returns a
 * synchronous `of(...)` observable, which is essential for prerendering: when
 * `setActiveLang('es')` runs, the Spanish dictionary is applied within the same
 * change-detection tick, so the prerendered HTML is baked in the right language.
 * An async HTTP loader would risk emitting blank/English HTML during prerender.
 */
@Injectable({ providedIn: 'root' })
export class InlineTranslocoLoader implements TranslocoLoader {
  private readonly dictionaries: Record<string, Translation> = { en, es };

  getTranslation(lang: string) {
    return of(this.dictionaries[lang] ?? this.dictionaries['en']);
  }
}
