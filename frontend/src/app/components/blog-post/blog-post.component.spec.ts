import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TranslocoService } from '@jsverse/transloco';
import { of } from 'rxjs';

import { BlogPostComponent } from './blog-post.component';
import { BlogPost } from '../../services/blog.service';
import { SeoService } from '../../services/seo.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';

import { translocoTesting } from '../../i18n/transloco-testing';

describe('BlogPostComponent', () => {
  let component: BlogPostComponent;
  let fixture: ComponentFixture<BlogPostComponent>;
  let httpMock: HttpTestingController;
  let seo: SeoService;
  let localeNav: LocaleNavService;

  const mockPost: BlogPost = {
    id: 7, slug: 'my-first-post', titleEn: 'My first post', titleEs: 'Mi primer post',
    excerptEn: 'The excerpt', excerptEs: null,
    contentEn: '<h2>Hello</h2><p>Body <strong>bold</strong></p>', contentEs: null,
    hasCover: true, published: true, publishedOn: '2026-07-01', updatedAt: '2026-07-15T12:00:00'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), BlogPostComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        // Ruta con slug fijo: paramMap emite una vez al suscribirse.
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ slug: 'my-first-post' })) } }
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    seo = TestBed.inject(SeoService);
    localeNav = TestBed.inject(LocaleNavService);
    fixture = TestBed.createComponent(BlogPostComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.match(() => true).forEach(r => r.flush(null, { status: 200, statusText: 'OK' }));
  });

  it('loads the post by slug and renders the sanitized body via innerHTML', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne('/api/blog/slug/my-first-post');
    expect(req.request.method).toBe('GET');
    req.flush(mockPost);
    fixture.detectChanges();

    expect(component.post?.id).toBe(7);
    const body = fixture.nativeElement.querySelector('.blog-post-body');
    expect(body).toBeTruthy();
    expect(body.innerHTML).toContain('<h2>Hello</h2>');
    expect(body.innerHTML).toContain('<strong>bold</strong>');
  });

  it('falls back to the other language when the active-language content is empty', () => {
    const transloco = TestBed.inject(TranslocoService);
    component.post = { ...mockPost };

    transloco.setActiveLang('es');
    expect(component.title).toBe('Mi primer post');
    expect(component.contentHtml).toContain('Hello'); // contentEs vacío → cae al EN

    transloco.setActiveLang('en');
    expect(component.title).toBe('My first post');
  });

  it('normalizes legacy &nbsp;-encoded spaces when rendering the body', () => {
    // Posts guardados antes de que el backend normalizara el export de Quill.
    component.post = { ...mockPost, contentEn: '<p>Hola&nbsp;mundo&nbsp;bonito</p>' };
    expect(component.contentHtml).toBe('<p>Hola mundo bonito</p>');
  });

  it('applies article SEO and the BlogPosting JSON-LD after update()', () => {
    const updateSpy = spyOn(seo, 'update').and.callThrough();
    const jsonLdSpy = spyOn(seo, 'setPageJsonLd').and.callThrough();

    fixture.detectChanges();
    httpMock.expectOne('/api/blog/slug/my-first-post').flush(mockPost);

    const config = updateSpy.calls.mostRecent().args[0];
    expect(config.path).toBe('/blog/my-first-post');
    expect(config.type).toBe('article');
    expect(config.publishedTime).toBe('2026-07-01');
    expect(config.image).toContain('/api/blog/7/cover');

    const jsonLd = jsonLdSpy.calls.mostRecent().args[0] as Record<string, unknown>;
    expect(jsonLd['@type']).toBe('BlogPosting');
    expect(jsonLd['datePublished']).toBe('2026-07-01');
    // El orden importa: el JSON-LD se fija DESPUÉS del update() que lo limpia.
    expect(updateSpy).toHaveBeenCalledBefore(jsonLdSpy);
  });

  it('navigates to the soft-404 page when the slug does not exist (404)', () => {
    const navSpy = spyOn(localeNav, 'navigate');

    fixture.detectChanges();
    httpMock.expectOne('/api/blog/slug/my-first-post')
      .flush(null, { status: 404, statusText: 'Not Found' });

    expect(navSpy).toHaveBeenCalledWith(['/not-found'], { skipLocationChange: true });
  });

  it('does NOT navigate away (nor noindex) on transient non-404 errors', () => {
    const navSpy = spyOn(localeNav, 'navigate');
    const updateSpy = spyOn(seo, 'update');

    fixture.detectChanges();
    httpMock.expectOne('/api/blog/slug/my-first-post')
      .flush(null, { status: 500, statusText: 'Server Error' });

    expect(navSpy).not.toHaveBeenCalled();
    expect(updateSpy).not.toHaveBeenCalled(); // se conserva el fallback indexable
  });
});
