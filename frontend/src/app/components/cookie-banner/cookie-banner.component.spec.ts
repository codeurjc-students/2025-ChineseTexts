import { TestBed } from '@angular/core/testing';
import { CookieBannerComponent } from './cookie-banner.component';
import { provideRouter } from '@angular/router';

describe('CookieBannerComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CookieBannerComponent],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(CookieBannerComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });
});