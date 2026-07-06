import { TestBed } from '@angular/core/testing';
import { Title, Meta } from '@angular/platform-browser';
import { DOCUMENT } from '@angular/common';

import { SeoService } from './seo.service';

describe('SeoService', () => {
  let service: SeoService;
  let title: Title;
  let meta: Meta;
  let doc: Document;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [SeoService, Title, Meta] });
    service = TestBed.inject(SeoService);
    title = TestBed.inject(Title);
    meta = TestBed.inject(Meta);
    doc = TestBed.inject(DOCUMENT);
  });

  afterEach(() => {
    doc.querySelectorAll("link[rel='canonical'], link[rel='alternate']").forEach(l => l.remove());
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should set title, description and og:title', () => {
    service.update({ title: 'My Title', description: 'My description', path: '/texts' });

    expect(title.getTitle()).toBe('My Title');
    expect(meta.getTag("name='description'")?.content).toBe('My description');
    expect(meta.getTag("property='og:title'")?.content).toBe('My Title');
  });

  it('should build an absolute canonical URL from the path', () => {
    service.update({ title: 'T', description: 'D', path: '/texts/HSK3' });

    const canonical = doc.querySelector("link[rel='canonical']") as HTMLLinkElement;
    expect(canonical).toBeTruthy();
    expect(canonical.getAttribute('href')).toBe('https://chinesereads.com/texts/HSK3');
  });

  it('should keep a single canonical link across successive updates', () => {
    service.update({ title: 'A', description: 'D', path: '/a' });
    service.update({ title: 'B', description: 'D', path: '/b' });

    const links = doc.querySelectorAll("link[rel='canonical']");
    expect(links.length).toBe(1);
    expect((links[0] as HTMLLinkElement).getAttribute('href')).toBe('https://chinesereads.com/b');
  });

  it('should mark private pages as noindex', () => {
    service.update({ title: 'Private', description: 'D', path: '/profile', noindex: true });
    expect(meta.getTag("name='robots'")?.content).toBe('noindex, nofollow');
  });

  it('should set the document language and og:locale per language', () => {
    service.update({ title: 'T', description: 'D', path: '/texts' }, 'es');
    expect(doc.documentElement.lang).toBe('es');
    expect(meta.getTag("property='og:locale'")?.content).toBe('es_ES');

    service.update({ title: 'T', description: 'D', path: '/texts' }, 'en');
    expect(doc.documentElement.lang).toBe('en');
    expect(meta.getTag("property='og:locale'")?.content).toBe('en_US');
  });

  it('should build a Spanish canonical under /es', () => {
    service.update({ title: 'T', description: 'D', path: '/texts/HSK3' }, 'es');
    const canonical = doc.querySelector("link[rel='canonical']") as HTMLLinkElement;
    expect(canonical.getAttribute('href')).toBe('https://chinesereads.com/es/texts/HSK3');
  });

  it('should emit hreflang alternates (en, es, x-default) for indexable pages', () => {
    service.update({ title: 'T', description: 'D', path: '/texts' }, 'en');
    const en = doc.querySelector("link[rel='alternate'][hreflang='en']") as HTMLLinkElement;
    const es = doc.querySelector("link[rel='alternate'][hreflang='es']") as HTMLLinkElement;
    const xd = doc.querySelector("link[rel='alternate'][hreflang='x-default']") as HTMLLinkElement;
    expect(en.getAttribute('href')).toBe('https://chinesereads.com/texts');
    expect(es.getAttribute('href')).toBe('https://chinesereads.com/es/texts');
    expect(xd.getAttribute('href')).toBe('https://chinesereads.com/texts');
  });

  it('should not emit hreflang alternates for noindex pages', () => {
    service.update({ title: 'P', description: 'D', path: '/profile', noindex: true }, 'en');
    expect(doc.querySelectorAll("link[rel='alternate']").length).toBe(0);
  });
});
