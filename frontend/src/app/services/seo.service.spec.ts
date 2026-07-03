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
    doc.querySelectorAll("link[rel='canonical']").forEach(l => l.remove());
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
});
