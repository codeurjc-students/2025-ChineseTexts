import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoModule } from '@jsverse/transloco';

import { LoginService } from '../../services/login.service';
import { PremiumService } from '../../services/premium.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';

/**
 * Public pricing / upgrade page (/premium). Shows the plan cards for anonymous and
 * free users, or the "manage subscription" panel for active premium users. Payment
 * happens on Stripe's hosted page — this component only redirects to the URL the
 * backend returns.
 */
@Component({
  selector: 'app-premium',
  standalone: true,
  imports: [CommonModule, TranslocoModule],
  templateUrl: './premium.component.html',
  styleUrl: './premium.component.scss'
})
export class PremiumComponent implements OnInit {

  loading: 'monthly' | 'yearly' | 'portal' | null = null;
  errored = false;

  constructor(
    public loginService: LoginService,
    private premiumService: PremiumService,
    private localeNav: LocaleNavService
  ) {}

  ngOnInit(): void {
    // Refresh cached user so the login / premium state on this page is current.
    this.loginService.reqIsLogged().subscribe();
  }

  /**
   * Resets the button state when the page is restored from the browser's back/forward
   * cache (bfcache). Redirecting to Stripe leaves a button in its loading state; without
   * this, hitting "Back" restores the frozen page with the spinner still spinning and the
   * button disabled. `event.persisted` is true only for a bfcache restore.
   */
  @HostListener('window:pageshow', ['$event'])
  onPageShow(event: PageTransitionEvent): void {
    if (event.persisted) {
      this.loading = null;
      this.errored = false;
    }
  }

  get isLogged(): boolean {
    return this.loginService.isLogged();
  }

  get isPremium(): boolean {
    return this.loginService.isPremiumActive();
  }

  get premiumUntil(): string | null | undefined {
    return this.loginService.currentUser()?.premiumUntil;
  }

  subscribe(plan: 'monthly' | 'yearly'): void {
    if (!this.isLogged) {
      // Anonymous visitors sign up first, then come back to subscribe.
      this.localeNav.navigate(['/signup']);
      return;
    }
    this.errored = false;
    this.loading = plan;
    this.premiumService.checkout(plan).subscribe({
      next: ({ url }) => { window.location.href = url; },
      error: () => { this.loading = null; this.errored = true; }
    });
  }

  manage(): void {
    this.errored = false;
    this.loading = 'portal';
    this.premiumService.portal().subscribe({
      next: ({ url }) => { window.location.href = url; },
      error: () => { this.loading = null; this.errored = true; }
    });
  }
}
