import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoModule } from '@jsverse/transloco';
import { LocaleNavService } from '../../i18n/locale-nav.service';

/**
 * Wildcard (`**`) route target: any URL that matches no real route lands here
 * instead of silently failing to navigate. SEO for this page (noindex soft-404
 * metadata) is applied globally by AppComponent via resolveSeo()'s fallback,
 * so no per-component override is needed.
 */
@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, TranslocoModule],
  templateUrl: './not-found.component.html',
  styleUrls: ['./not-found.component.scss']
})
export class NotFoundComponent {

  constructor(private localeNav: LocaleNavService) {}

  goHome(): void {
    this.localeNav.navigate(['/']);
  }

  goTexts(): void {
    this.localeNav.navigate(['/texts']);
  }
}
