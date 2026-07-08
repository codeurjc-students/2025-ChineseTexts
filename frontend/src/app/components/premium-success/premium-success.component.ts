import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';

import { LoginService } from '../../services/login.service';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';

/**
 * Post-checkout landing page (/premium/success). Stripe redirects here after payment.
 * We refresh the cached user so the freshly-set premiumUntil is reflected immediately
 * (the webhook may land a moment before or after this page loads; the user data is
 * re-fetched from the backend either way).
 */
@Component({
  selector: 'app-premium-success',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './premium-success.component.html',
  styleUrl: './premium-success.component.scss'
})
export class PremiumSuccessComponent implements OnInit {

  constructor(private loginService: LoginService) {}

  ngOnInit(): void {
    this.loginService.reqIsLogged().subscribe();
  }
}
