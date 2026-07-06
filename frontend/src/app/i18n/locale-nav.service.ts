import { Injectable } from '@angular/core';
import { NavigationExtras, Router } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { addLangPrefix, Lang } from './locale.util';

/**
 * Locale-aware wrapper around imperative router navigation. Use this instead of
 * `router.navigate([...])` so programmatic redirects keep the user in the
 * current language (e.g. a login redirect from `/es/...` lands on `/es`, not `/`).
 *
 * Only absolute command arrays are prefixed; relative navigations are passed
 * through untouched.
 */
@Injectable({ providedIn: 'root' })
export class LocaleNavService {
  constructor(private router: Router, private transloco: TranslocoService) {}

  navigate(commands: unknown[], extras?: NavigationExtras): Promise<boolean> {
    const localized = this.localize(commands);
    // Avoid passing a second `undefined` argument so callers/tests that inspect
    // the underlying router.navigate call see the same arity as a direct call.
    return extras ? this.router.navigate(localized, extras) : this.router.navigate(localized);
  }

  private localize(commands: unknown[]): unknown[] {
    const lang = this.transloco.getActiveLang() as Lang;
    if (lang !== 'es' || commands.length === 0) return commands;
    const [first, ...rest] = commands;
    if (typeof first === 'string' && first.startsWith('/')) {
      return [addLangPrefix(first, 'es'), ...rest];
    }
    return commands;
  }
}
