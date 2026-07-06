/**
 * Pure, framework-free helpers for the URL-prefix locale scheme:
 *   English lives at the root (`/texts`), Spanish under `/es` (`/es/texts`).
 *
 * The active language is derived SOLELY from the URL — never from
 * `navigator.language` or `localStorage` — so the server (prerender) and the
 * client always agree, avoiding hydration mismatches.
 */

export type Lang = 'en' | 'es';

export const LANGS: readonly Lang[] = ['en', 'es'];

/** Language implied by a URL path. `/es`, `/es/...` → 'es'; everything else → 'en'. */
export function langFromUrl(url: string): Lang {
  const path = url.split('?')[0].split('#')[0];
  return path === '/es' || path.startsWith('/es/') ? 'es' : 'en';
}

/** Removes the `/es` prefix, yielding the canonical English path. `/es/texts` → `/texts`, `/es` → `/`. */
export function stripLangPrefix(url: string): string {
  if (url === '/es') return '/';
  if (url.startsWith('/es/')) return url.slice(3);
  if (url.startsWith('/es?') || url.startsWith('/es#')) return '/' + url.slice(3);
  return url;
}

/** Adds the locale prefix to an un-prefixed path. English is unchanged; Spanish gets `/es`. */
export function addLangPrefix(path: string, lang: Lang): string {
  const p = path.startsWith('/') ? path : '/' + path;
  if (lang !== 'es') return p;
  return p === '/' ? '/es' : '/es' + p;
}
