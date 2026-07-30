import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TranslocoService } from '@jsverse/transloco';

import { HallOfFameComponent } from './hall-of-fame.component';
import { HallOfFameEntry } from '../../services/hall-of-fame.service';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('HallOfFameComponent', () => {
  let component: HallOfFameComponent;
  let fixture: ComponentFixture<HallOfFameComponent>;
  let httpMock: HttpTestingController;

  const mockEntry: HallOfFameEntry = {
    id: 1, name: 'María López', slug: 'maria-lopez', taglineEn: 'Chinese teacher', taglineEs: '',
    bioEn: 'English bio', bioEs: 'Bio en español', discountCode: 'MARIA10',
    displayOrder: 0, hasPhoto: false, badges: ['pioneer', 'star'], socials: []
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), HallOfFameComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(HallOfFameComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    // Drena peticiones pendientes (LoginService consulta /api/users/me al arrancar).
    httpMock.match(() => true).forEach(r => r.flush(null, { status: 200, statusText: 'OK' }));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads the entries from the API on init and renders a card with its badges', () => {
    fixture.detectChanges(); // dispara ngOnInit
    const req = httpMock.expectOne('/api/hall-of-fame');
    expect(req.request.method).toBe('GET');
    req.flush([mockEntry]);
    fixture.detectChanges();

    expect(component.entries.length).toBe(1);
    expect(component.loading).toBeFalse();

    const cards = fixture.nativeElement.querySelectorAll('.hof-card');
    expect(cards.length).toBe(1);
    const chips = fixture.nativeElement.querySelectorAll('.hof-badge-chip');
    expect(chips.length).toBe(2);
    expect(chips[0].getAttribute('title')).toBeTruthy();
  });

  it('falls back to the other language when the active-language bio is empty', () => {
    const transloco = TestBed.inject(TranslocoService);

    transloco.setActiveLang('en');
    expect(component.bioOf({ ...mockEntry })).toBe('English bio');
    expect(component.bioOf({ ...mockEntry, bioEn: '' })).toBe('Bio en español');

    transloco.setActiveLang('es');
    expect(component.bioOf({ ...mockEntry })).toBe('Bio en español');
    expect(component.bioOf({ ...mockEntry, bioEs: '' })).toBe('English bio');
  });

  it('applies the same language fallback to the tagline', () => {
    const transloco = TestBed.inject(TranslocoService);

    transloco.setActiveLang('es');
    // taglineEs vacía → cae a la inglesa.
    expect(component.taglineOf({ ...mockEntry })).toBe('Chinese teacher');
    expect(component.taglineOf({ ...mockEntry, taglineEs: 'Profesora de chino' })).toBe('Profesora de chino');

    transloco.setActiveLang('en');
    expect(component.taglineOf({ ...mockEntry, taglineEn: '', taglineEs: 'Profesora de chino' })).toBe('Profesora de chino');
  });

  it('toggleBadge adds and removes keys immutably', () => {
    const entry = { ...mockEntry, badges: ['star'] };
    const before = entry.badges;

    component.toggleBadge(entry, 'pioneer');
    expect(entry.badges).toEqual(['star', 'pioneer']);
    expect(before).toEqual(['star']); // la lista original no se muta

    component.toggleBadge(entry, 'star');
    expect(entry.badges).toEqual(['pioneer']);
  });

  it('computes initials for the default avatar', () => {
    expect(component.initialsOf({ ...mockEntry, name: 'José Víctor' })).toBe('JV');
    expect(component.initialsOf({ ...mockEntry, name: '' })).toBe('?');
  });

  it('addSocial with a preset pre-fills the network icon and label and closes the picker', () => {
    const entry = { ...mockEntry, socials: [] };
    const instagram = component.socialPresets.find(p => p.key === 'instagram')!;
    component.socialPickerFor = entry.id!;

    component.addSocial(entry, instagram);
    const req = httpMock.expectOne('/api/hall-of-fame/1/socials');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.icon).toBe('bi-instagram');
    expect(req.request.body.label).toBe('Instagram');
    req.flush({ id: 5, label: 'Instagram', icon: 'bi-instagram', url: '', displayOrder: 0 });

    expect(entry.socials.length).toBe(1);
    expect(component.socialPickerFor).toBeNull();
  });

  it('addSocial without a preset keeps the generic custom-link draft', () => {
    const entry = { ...mockEntry, socials: [] };

    component.addSocial(entry);
    const req = httpMock.expectOne('/api/hall-of-fame/1/socials');
    expect(req.request.body.icon).toBe('bi-link-45deg');
    req.flush({ id: 6, label: 'New link', icon: 'bi-link-45deg', url: '', displayOrder: 0 });

    expect(entry.socials.length).toBe(1);
  });

  it('auto-detects the network from the URL while the icon is still generic', () => {
    const social = { id: 5, label: 'New link', icon: 'bi-link-45deg', url: 'https://www.tiktok.com/@user', displayOrder: 0 };
    component.onSocialUrlChange(social);
    expect(social.icon).toBe('bi-tiktok');
    expect(social.label).toBe('TikTok');
  });

  it('never overrides an explicitly chosen icon on URL change', () => {
    const social = { id: 6, label: 'Mi canal', icon: 'bi-youtube', url: 'https://instagram.com/user', displayOrder: 1 };
    component.onSocialUrlChange(social);
    expect(social.icon).toBe('bi-youtube');
    expect(social.label).toBe('Mi canal');
  });
});
