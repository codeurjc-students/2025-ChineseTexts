import { Injectable, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { Title, Meta } from '@angular/platform-browser';
import { Lang, addLangPrefix } from '../i18n/locale.util';

/**
 * Per-page SEO metadata. `path` is the absolute route path (e.g. '/texts'),
 * used to build the canonical + Open Graph URL. When omitted the site root is used.
 * `path` is always the un-prefixed (English) path; the service adds the `/es`
 * prefix when rendering the Spanish variant.
 */
export interface SeoConfig {
  title: string;
  description: string;
  keywords?: string;
  path?: string;
  image?: string;
  /** Private / authenticated pages should not be indexed by search engines. */
  noindex?: boolean;
}

export const SITE_URL = 'https://chinesereads.com';
const SITE_NAME = 'ChineseReads';
/** id of the per-page JSON-LD <script>, so it never collides with the static site-wide graph. */
const PAGE_JSONLD_ID = 'seo-page-jsonld';
const DEFAULT_IMAGE = `${SITE_URL}/icon-512.png`;
const DEFAULT_KEYWORDS =
  'learn chinese, chinese reads, chinese texts, learn chinese by reading, ' +
  'read chinese texts, learn chinese with graded texts, chinese graded readers, ' +
  'HSK reading practice, mandarin reading, chinese reading practice';

const OG_LOCALE: Record<Lang, string> = { en: 'en_US', es: 'es_ES' };

/**
 * Centralises all SEO tag management (title, description, keywords, canonical,
 * Open Graph, Twitter Card, hreflang alternates and document language) in one
 * reusable, SSR-safe place.
 *
 * It relies exclusively on Angular's Title/Meta services and the injected
 * DOCUMENT, all of which work during server-side rendering / prerendering — so
 * crawlers receive fully-populated, route- and language-specific tags in the
 * initial HTML.
 */
@Injectable({ providedIn: 'root' })
export class SeoService {

  constructor(
    private titleService: Title,
    private meta: Meta,
    @Inject(DOCUMENT) private doc: Document
  ) {}

  update(config: SeoConfig, lang: Lang = 'en'): void {
    const title = config.title;
    const description = config.description;
    const enPath = this.normalizePath(config.path);       // canonical English path
    const url = SITE_URL + addLangPrefix(enPath, lang);    // self-URL for this locale
    const image = config.image ?? DEFAULT_IMAGE;
    const robots = config.noindex ? 'noindex, nofollow' : 'index, follow';

    // Document language (affects <html lang>) — important for accessibility & SEO.
    this.doc.documentElement.lang = lang;

    this.titleService.setTitle(title);

    this.meta.updateTag({ name: 'description', content: description });
    this.meta.updateTag({ name: 'keywords', content: config.keywords ?? DEFAULT_KEYWORDS });
    this.meta.updateTag({ name: 'robots', content: robots });
    this.meta.updateTag({ name: 'googlebot', content: robots });

    // Open Graph
    this.meta.updateTag({ property: 'og:type', content: 'website' });
    this.meta.updateTag({ property: 'og:site_name', content: SITE_NAME });
    this.meta.updateTag({ property: 'og:title', content: title });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ property: 'og:image', content: image });
    this.meta.updateTag({ property: 'og:locale', content: OG_LOCALE[lang] });

    // Twitter Card
    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ name: 'twitter:title', content: title });
    this.meta.updateTag({ name: 'twitter:description', content: description });
    this.meta.updateTag({ name: 'twitter:image', content: image });

    this.setCanonical(url);
    this.setAlternates(enPath, config.noindex === true);
    // Per-page JSON-LD is cleared on every update so it can never leak into the
    // next route; pages that want structured data re-set it after each update.
    this.clearPageJsonLd();
    this.setJsonLdLanguage(lang);
  }

  /**
   * Injects page-specific structured data (schema.org JSON-LD) alongside the
   * static site-wide graph from index.html. Idempotent: one script element,
   * replaced on every call and removed by the next `update()`.
   */
  setPageJsonLd(data: object): void {
    let script = this.doc.getElementById(PAGE_JSONLD_ID) as HTMLScriptElement | null;
    if (!script) {
      script = this.doc.createElement('script');
      script.type = 'application/ld+json';
      script.id = PAGE_JSONLD_ID;
      this.doc.head.appendChild(script);
    }
    script.textContent = JSON.stringify(data);
  }

  clearPageJsonLd(): void {
    this.doc.getElementById(PAGE_JSONLD_ID)?.remove();
  }

  /** Ensures a single <link rel="canonical"> pointing at the current URL. */
  private setCanonical(url: string): void {
    let link = this.doc.querySelector("link[rel='canonical']") as HTMLLinkElement | null;
    if (!link) {
      link = this.doc.createElement('link');
      link.setAttribute('rel', 'canonical');
      this.doc.head.appendChild(link);
    }
    link.setAttribute('href', url);
  }

  /**
   * Emits <link rel="alternate" hreflang="en|es|x-default"> so Google serves the
   * right language variant. Only indexable pages get alternates; on noindex pages
   * they are removed (a private page has no public language twin to advertise).
   */
  private setAlternates(enPath: string, noindex: boolean): void {
    const alternates: Array<[string, string]> = [
      ['en', SITE_URL + enPath],
      ['es', SITE_URL + addLangPrefix(enPath, 'es')],
      ['x-default', SITE_URL + enPath],
    ];

    for (const [hreflang, href] of alternates) {
      const selector = `link[rel='alternate'][hreflang='${hreflang}']`;
      let link = this.doc.querySelector(selector) as HTMLLinkElement | null;
      if (noindex) {
        if (link) link.remove();
        continue;
      }
      if (!link) {
        link = this.doc.createElement('link');
        link.setAttribute('rel', 'alternate');
        link.setAttribute('hreflang', hreflang);
        this.doc.head.appendChild(link);
      }
      link.setAttribute('href', href);
    }
  }

  /**
   * Keeps the static JSON-LD `inLanguage` fields in sync with the active
   * language. Wrapped in try/catch so a malformed script can never break rendering.
   */
  private setJsonLdLanguage(lang: Lang): void {
    try {
      // :not() keeps this from touching the per-page JSON-LD, whose inLanguage
      // is the language of the CONTENT (e.g. zh-Hans), not of the UI.
      const script = this.doc.querySelector(`script[type="application/ld+json"]:not(#${PAGE_JSONLD_ID})`);
      if (!script || !script.textContent) return;
      const data = JSON.parse(script.textContent);
      const nodes = Array.isArray(data['@graph']) ? data['@graph'] : [data];
      let changed = false;
      for (const node of nodes) {
        if (node && typeof node === 'object' && 'inLanguage' in node) {
          node.inLanguage = lang;
          changed = true;
        }
      }
      if (changed) script.textContent = JSON.stringify(data);
    } catch {
      // Leave the static JSON-LD untouched on any parse error.
    }
  }

  private normalizePath(path?: string): string {
    if (!path || path === '/') return '/';
    return path.startsWith('/') ? path : `/${path}`;
  }
}
