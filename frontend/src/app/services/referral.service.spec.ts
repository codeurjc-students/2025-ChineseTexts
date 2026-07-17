import { TestBed } from '@angular/core/testing';

import { ReferralService } from './referral.service';

describe('ReferralService', () => {
  let service: ReferralService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ReferralService);
    localStorage.removeItem('referral_code');
  });

  afterEach(() => {
    localStorage.removeItem('referral_code');
  });

  it('stores a captured code and returns it', () => {
    service.capture('MARIA20');
    expect(service.get()).toBe('MARIA20');
  });

  it('normalizes the code like the backend: charset, case and length', () => {
    service.capture('  maría-20! ');
    expect(service.get()).toBe('MARA-20');
  });

  it('ignores junk-only codes without touching a previously stored one', () => {
    service.capture('MARIA20');
    service.capture('¡¡¡···!!!');
    expect(service.get()).toBe('MARIA20');
  });

  it('last click wins: a newer campaign code replaces the stored one', () => {
    service.capture('MARIA20');
    service.capture('PEDRO10');
    expect(service.get()).toBe('PEDRO10');
  });

  it('returns null and self-cleans when the attribution window has expired', () => {
    localStorage.setItem('referral_code', JSON.stringify({
      code: 'OLD', ts: Date.now() - 61 * 24 * 3600 * 1000
    }));
    expect(service.get()).toBeNull();
    expect(localStorage.getItem('referral_code')).toBeNull();
  });

  it('returns null on corrupted storage instead of throwing', () => {
    localStorage.setItem('referral_code', 'not-json{{');
    expect(service.get()).toBeNull();
  });

  it('clear removes the stored code', () => {
    service.capture('MARIA20');
    service.clear();
    expect(service.get()).toBeNull();
  });
});
