import { TestBed } from '@angular/core/testing';
import { CookieBannerComponent } from './cookie-banner.component';
import { provideRouter } from '@angular/router';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('CookieBannerComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), CookieBannerComponent],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(CookieBannerComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });
});