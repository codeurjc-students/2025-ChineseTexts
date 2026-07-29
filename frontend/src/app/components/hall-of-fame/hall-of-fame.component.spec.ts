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
    id: 1, name: 'María López', slug: 'maria-lopez', tagline: 'Chinese teacher',
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
});
