import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationStart, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { TranslocoService } from '@jsverse/transloco';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { HomeComponent } from './components/home/home.component';
import { ToastComponent } from './components/toast/toast.component';
import { SeoService } from './services/seo.service';
import { ReferralService } from './services/referral.service';
import { resolveSeo } from './services/seo.config';
import { langFromUrl, stripLangPrefix } from './i18n/locale.util';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, HeaderComponent, FooterComponent, HomeComponent, ToastComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'chinesereads';

  constructor(private router: Router, private seo: SeoService, private transloco: TranslocoService,
      private referral: ReferralService) {
    // Set the active language from the URL prefix BEFORE the target component
    // renders (NavigationStart fires first, on both server and client). The
    // inline loader is synchronous, so the correct dictionary is applied in the
    // same tick — this is what makes the prerendered HTML come out in the right
    // language. Deriving lang from the URL (not navigator/localStorage) keeps
    // server and client in agreement and avoids hydration mismatches.
    this.router.events
      .pipe(filter((e): e is NavigationStart => e instanceof NavigationStart))
      .subscribe(e => this.transloco.setActiveLang(langFromUrl(e.url)));

    // Update SEO tags on every navigation (also runs during SSR, so crawlers get
    // route-specific title/description/canonical in the initial HTML). Components
    // with dynamic content (e.g. an individual text) may refine these afterwards.
    // resolveSeo receives the un-prefixed path plus the active language.
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => {
        const url = e.urlAfterRedirects;
        const lang = langFromUrl(url);
        this.seo.update(resolveSeo(stripLangPrefix(url), lang), lang);
        // Influencer attribution: remember the ?ref=CODE an inbound campaign link
        // carries, so a later signup can credit it. Browser-only inside the service;
        // canonicals/SEO ignore query params, so these URLs never leak into metadata.
        const ref = this.router.parseUrl(url).queryParams['ref'];
        if (ref) this.referral.capture(ref);
      });
  }
}
