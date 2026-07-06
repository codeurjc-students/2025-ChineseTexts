import { Pipe, PipeTransform } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { addLangPrefix, Lang } from './locale.util';

/**
 * Makes an absolute `routerLink` locale-aware: when the active language is
 * Spanish it prefixes the first (absolute) command segment with `/es`, so
 * navigation stays within the current language. English is passed through
 * unchanged.
 *
 * Usage: `[routerLink]="['/texts','HSK1'] | localizeLink"` or
 *        `[routerLink]="'/signup' | localizeLink"`.
 *
 * Impure because the output depends on the active language, which changes at
 * runtime; navigation already re-renders on language change, so the extra
 * evaluations are negligible.
 */
@Pipe({ name: 'localizeLink', standalone: true, pure: false })
export class LocalizeLinkPipe implements PipeTransform {
  constructor(private transloco: TranslocoService) {}

  transform(commands: unknown[] | string): unknown[] {
    const arr = Array.isArray(commands) ? [...commands] : [commands];
    const lang = this.transloco.getActiveLang() as Lang;
    if (lang !== 'es' || arr.length === 0) return arr;

    const [first, ...rest] = arr;
    // Only prefix absolute paths (leading '/'); relative commands are left alone.
    if (typeof first === 'string' && first.startsWith('/')) {
      return [addLangPrefix(first, 'es'), ...rest];
    }
    return arr;
  }
}
