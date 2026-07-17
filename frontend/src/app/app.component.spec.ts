import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { Component } from '@angular/core';
import { translocoTesting } from './i18n/transloco-testing';
import { ReferralService } from './services/referral.service';

@Component({ template: '' })
class DummyComponent {}

describe('AppComponent', () => {
  let referralServiceSpy: jasmine.SpyObj<ReferralService>;

  beforeEach(async () => {
    referralServiceSpy = jasmine.createSpyObj('ReferralService', ['capture', 'get', 'clear']);
    await TestBed.configureTestingModule({
      imports: [AppComponent, translocoTesting()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '', component: DummyComponent }]),
        { provide: ReferralService, useValue: referralServiceSpy },
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('captures the ?ref code from an inbound campaign link', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/?ref=MARIA20');

    expect(referralServiceSpy.capture).toHaveBeenCalledWith('MARIA20');
  });

  it('does not touch the referral store when navigating without ?ref', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/');

    expect(referralServiceSpy.capture).not.toHaveBeenCalled();
  });
});
