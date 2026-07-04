import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { FounderComponent } from './founder.component';
import { FounderProfile } from '../../services/founder.service';

describe('FounderComponent', () => {
  let component: FounderComponent;
  let fixture: ComponentFixture<FounderComponent>;
  let httpMock: HttpTestingController;

  const mockProfile: FounderProfile = {
    id: 1, name: 'Test Name', role: '', tagline: '', location: '', summary: '',
    hasPhoto: false, socials: [], sections: []
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FounderComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(FounderComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    // Drena peticiones pendientes (LoginService consulta /api/users/me al arrancar).
    httpMock.match(() => true).forEach(r => r.flush(null, { status: 200, statusText: 'OK' }));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads the profile from the API on init', () => {
    fixture.detectChanges(); // dispara ngOnInit
    const req = httpMock.expectOne('/api/founder');
    expect(req.request.method).toBe('GET');
    req.flush(mockProfile);
    expect(component.profile?.name).toBe('Test Name');
    expect(component.loading).toBeFalse();
  });

  it('computes initials from the name', () => {
    component.profile = { ...mockProfile, name: 'José Víctor' };
    expect(component.initials).toBe('JV');
  });
});
