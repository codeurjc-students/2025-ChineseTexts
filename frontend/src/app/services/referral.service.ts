import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/** Attribution window: a signup within this many days still credits the influencer. */
const MAX_AGE_DAYS = 60;
const STORAGE_KEY = 'referral_code';

/**
 * Influencer attribution: when a visitor lands through a shared link like
 * `chinesereads.com/?ref=MARIA`, the code is kept in localStorage so that if they
 * sign up later (same device) the account records where they came from. First-party,
 * non-identifying and functional — it holds a single campaign code, never tracking
 * data, and it is cleared as soon as it is used. Everything is browser-guarded so
 * prerendering never touches localStorage.
 */
@Injectable({ providedIn: 'root' })
export class ReferralService {

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  /**
   * Normalizes and stores a ?ref code (last click wins — the newest campaign gets
   * the credit, mirroring standard affiliate attribution). Junk values are ignored.
   */
  capture(raw: string | null | undefined): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const code = this.normalize(raw);
    if (!code) return;
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ code, ts: Date.now() }));
    } catch {
      // Storage full/blocked: attribution is best-effort, never break navigation.
    }
  }

  /** The stored code if still within the attribution window, else null. */
  get(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const { code, ts } = JSON.parse(raw);
      if (typeof code !== 'string' || typeof ts !== 'number' ||
          Date.now() - ts > MAX_AGE_DAYS * 24 * 3600 * 1000) {
        this.clear();
        return null;
      }
      return code;
    } catch {
      return null;
    }
  }

  /** Removes the stored code (called once the signup that used it succeeds). */
  clear(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // ignore
    }
  }

  /** Same rules as the backend sanitizer: [A-Za-z0-9_-] only, upper case, ≤40 chars. */
  private normalize(raw: string | null | undefined): string | null {
    if (!raw) return null;
    const clean = raw.trim().replace(/[^A-Za-z0-9_-]/g, '').toUpperCase();
    return clean ? clean.slice(0, 40) : null;
  }
}
