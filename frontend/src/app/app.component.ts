import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { HomeComponent } from './components/home/home.component';
import { CookieBannerComponent } from './components/cookie-banner/cookie-banner.component';
import { SeoService } from './services/seo.service';
import { resolveSeo } from './services/seo.config';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, HeaderComponent, FooterComponent, HomeComponent, CookieBannerComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'chinesereads';

  constructor(private router: Router, private seo: SeoService) {
    // Update SEO tags on every navigation (also runs during SSR, so crawlers get
    // route-specific title/description/canonical in the initial HTML). Components
    // with dynamic content (e.g. an individual text) may refine these afterwards.
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => this.seo.update(resolveSeo(e.urlAfterRedirects)));
  }
}
