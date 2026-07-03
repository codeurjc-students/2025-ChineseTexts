import { Injectable, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { Title, Meta } from '@angular/platform-browser';

/**
 * Per-page SEO metadata. `path` is the absolute route path (e.g. '/texts'),
 * used to build the canonical + Open Graph URL. When omitted the site root is used.
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

const SITE_URL = 'https://chinesereads.com';
const SITE_NAME = 'ChineseReads';
const DEFAULT_IMAGE = `${SITE_URL}/icon-512.png`;
const DEFAULT_KEYWORDS =
  'learn chinese, chinese reads, chinese texts, learn chinese by reading, ' +
  'read chinese texts, learn chinese with graded texts, chinese graded readers, ' +
  'HSK reading practice, mandarin reading, chinese reading practice';

/**
 * Centralises all SEO tag management (title, description, keywords, canonical,
 * Open Graph and Twitter Card) in one reusable, SSR-safe place.
 *
 * It relies exclusively on Angular's Title/Meta services and the injected
 * DOCUMENT, all of which work during server-side rendering — so crawlers receive
 * fully-populated, route-specific tags in the initial HTML.
 */
@Injectable({ providedIn: 'root' })
export class SeoService {

  constructor(
    private titleService: Title,
    private meta: Meta,
    @Inject(DOCUMENT) private doc: Document
  ) {}

  update(config: SeoConfig): void {
    const title = config.title;
    const description = config.description;
    const url = SITE_URL + this.normalizePath(config.path);
    const image = config.image ?? DEFAULT_IMAGE;
    const robots = config.noindex ? 'noindex, nofollow' : 'index, follow';

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

    // Twitter Card
    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ name: 'twitter:title', content: title });
    this.meta.updateTag({ name: 'twitter:description', content: description });
    this.meta.updateTag({ name: 'twitter:image', content: image });

    this.setCanonical(url);
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

  private normalizePath(path?: string): string {
    if (!path || path === '/') return '/';
    return path.startsWith('/') ? path : `/${path}`;
  }
}
