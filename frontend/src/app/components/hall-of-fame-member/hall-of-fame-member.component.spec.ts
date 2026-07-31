import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TranslocoService } from '@jsverse/transloco';
import { of } from 'rxjs';

import { HallOfFameMemberComponent } from './hall-of-fame-member.component';
import { HallOfFameEntry } from '../../services/hall-of-fame.service';
import { SeoService } from '../../services/seo.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';

import { translocoTesting } from '../../i18n/transloco-testing';

describe('HallOfFameMemberComponent', () => {
  let component: HallOfFameMemberComponent;
  let fixture: ComponentFixture<HallOfFameMemberComponent>;
  let httpMock: HttpTestingController;
  let seo: SeoService;
  let localeNav: LocaleNavService;

  const mockEntry: HallOfFameEntry = {
    id: 3, name: 'María López', slug: 'maria-lopez',
    taglineEn: 'Chinese teacher', taglineEs: 'Profesora de chino',
    bioEn: 'Teaches Chinese online.', bioEs: '',
    discountCode: 'MARIA10', displayOrder: 0, hasPhoto: true,
    badges: ['pioneer', 'star'],
    socials: [
      { id: 1, label: 'Instagram', icon: 'bi-instagram', url: 'https://instagram.com/maria', displayOrder: 0 }
    ]
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), HallOfFameMemberComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        // Ruta con slug fijo: paramMap emite una vez al suscribirse.
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ slug: 'maria-lopez' })) } }
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    seo = TestBed.inject(SeoService);
    localeNav = TestBed.inject(LocaleNavService);
    fixture = TestBed.createComponent(HallOfFameMemberComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.match(() => true).forEach(r => r.flush(null, { status: 200, statusText: 'OK' }));
  });

  // Test 1: carga por slug y pinta nombre, badges, redes y código
  it('loads the member by slug and renders the profile', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne('/api/hall-of-fame/slug/maria-lopez');
    expect(req.request.method).toBe('GET');
    req.flush(mockEntry);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('María López');
    expect(el.querySelectorAll('.hof-member-badge').length).toBe(2);
    expect(el.querySelector('a[href="https://instagram.com/maria"]')).toBeTruthy();
    expect(el.querySelector('.hof-member-code-value')?.textContent).toContain('MARIA10');
  });

  // Test 2: tagline y bio con fallback de idioma (patrón bioOf/taglineOf)
  it('falls back to the other language when the active-language field is empty', () => {
    const transloco = TestBed.inject(TranslocoService);
    component.entry = { ...mockEntry };

    transloco.setActiveLang('es');
    expect(component.tagline).toBe('Profesora de chino');
    expect(component.bio).toContain('Teaches Chinese'); // bioEs vacía → cae al EN

    transloco.setActiveLang('en');
    expect(component.tagline).toBe('Chinese teacher');
  });

  // Test 3: SEO dinámico + ProfilePage/Person JSON-LD, en ese orden
  it('applies member SEO and the ProfilePage JSON-LD after update()', () => {
    const updateSpy = spyOn(seo, 'update').and.callThrough();
    const jsonLdSpy = spyOn(seo, 'setPageJsonLd').and.callThrough();

    fixture.detectChanges();
    httpMock.expectOne('/api/hall-of-fame/slug/maria-lopez').flush(mockEntry);

    const config = updateSpy.calls.mostRecent().args[0];
    expect(config.path).toBe('/hall-of-fame/maria-lopez');
    expect(config.title).toContain('María López');
    expect(config.image).toContain('/api/hall-of-fame/3/photo');

    const jsonLd = jsonLdSpy.calls.mostRecent().args[0] as Record<string, any>;
    expect(jsonLd['@type']).toBe('ProfilePage');
    expect(jsonLd['mainEntity']['@type']).toBe('Person');
    expect(jsonLd['mainEntity']['name']).toBe('María López');
    expect(jsonLd['mainEntity']['sameAs']).toEqual(['https://instagram.com/maria']);
    // El orden importa: el JSON-LD se fija DESPUÉS del update() que lo limpia.
    expect(updateSpy).toHaveBeenCalledBefore(jsonLdSpy);
  });

  // Test 4: slug desconocido → soft-404 noindex
  it('navigates to the soft-404 page when the slug does not exist (404)', () => {
    const navSpy = spyOn(localeNav, 'navigate');

    fixture.detectChanges();
    httpMock.expectOne('/api/hall-of-fame/slug/maria-lopez')
      .flush(null, { status: 404, statusText: 'Not Found' });

    expect(navSpy).toHaveBeenCalledWith(['/not-found'], { skipLocationChange: true });
  });

  // Test 5: errores transitorios no desindexan (se conserva el fallback indexable)
  it('does NOT navigate away (nor noindex) on transient non-404 errors', () => {
    const navSpy = spyOn(localeNav, 'navigate');
    const updateSpy = spyOn(seo, 'update');

    fixture.detectChanges();
    httpMock.expectOne('/api/hall-of-fame/slug/maria-lopez')
      .flush(null, { status: 500, statusText: 'Server Error' });

    expect(navSpy).not.toHaveBeenCalled();
    expect(updateSpy).not.toHaveBeenCalled();
  });
});
