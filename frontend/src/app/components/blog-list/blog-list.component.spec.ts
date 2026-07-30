import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TranslocoService } from '@jsverse/transloco';

import { BlogListComponent } from './blog-list.component';
import { BlogPostSummary } from '../../services/blog.service';
import { SeoService } from '../../services/seo.service';

import { translocoTesting } from '../../i18n/transloco-testing';

describe('BlogListComponent', () => {
  let component: BlogListComponent;
  let fixture: ComponentFixture<BlogListComponent>;
  let httpMock: HttpTestingController;
  let seo: SeoService;

  const mockPost: BlogPostSummary = {
    id: 1, slug: 'learn-hsk1-fast', titleEn: 'Learn HSK1 fast', titleEs: null,
    excerptEn: 'A quick guide', excerptEs: null, hasCover: false,
    published: true, publishedOn: '2026-07-01', updatedAt: '2026-07-15T12:00:00'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), BlogListComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    seo = TestBed.inject(SeoService);
    fixture = TestBed.createComponent(BlogListComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    // Drena peticiones pendientes (LoginService consulta /api/users/me al arrancar).
    httpMock.match(() => true).forEach(r => r.flush(null, { status: 200, statusText: 'OK' }));
  });

  it('loads the published posts and renders a card with the title fallback', () => {
    fixture.detectChanges(); // dispara ngOnInit
    const req = httpMock.expectOne('/api/blog');
    expect(req.request.method).toBe('GET');
    req.flush([mockPost]);
    fixture.detectChanges();

    expect(component.posts.length).toBe(1);
    expect(component.loading).toBeFalse();
    const cards = fixture.nativeElement.querySelectorAll('.blog-card');
    expect(cards.length).toBe(1);
    // titleEs vacío → fallback al inglés también con el idioma es activo.
    TestBed.inject(TranslocoService).setActiveLang('es');
    expect(component.titleOf(mockPost)).toBe('Learn HSK1 fast');
  });

  it('refines the SEO in the HTTP callback and sets the Blog JSON-LD only when there are posts', () => {
    const updateSpy = spyOn(seo, 'update').and.callThrough();
    const jsonLdSpy = spyOn(seo, 'setPageJsonLd').and.callThrough();

    fixture.detectChanges();
    httpMock.expectOne('/api/blog').flush([mockPost]);

    expect(updateSpy).toHaveBeenCalled();
    expect(updateSpy.calls.mostRecent().args[0].path).toBe('/blog');
    expect(jsonLdSpy).toHaveBeenCalled();
    const jsonLd = jsonLdSpy.calls.mostRecent().args[0] as Record<string, unknown>;
    expect(jsonLd['@type']).toBe('Blog');
  });

  it('cards of drafts link to the editor (their public URL would 404) and published ones to the post', () => {
    expect(component.cardLink({ ...mockPost, published: true })).toBe('/blog/learn-hsk1-fast');
    expect(component.cardLink({ ...mockPost, published: false })).toBe('/blog-editor/1');
  });

  it('on error it still applies the base SEO with an empty state (prerender safety)', () => {
    const updateSpy = spyOn(seo, 'update').and.callThrough();
    const jsonLdSpy = spyOn(seo, 'setPageJsonLd').and.callThrough();

    fixture.detectChanges();
    httpMock.expectOne('/api/blog').flush(null, { status: 500, statusText: 'Server Error' });

    expect(component.loading).toBeFalse();
    expect(component.posts.length).toBe(0);
    expect(updateSpy).toHaveBeenCalled();
    expect(jsonLdSpy).not.toHaveBeenCalled(); // sin posts no hay JSON-LD
  });
});
