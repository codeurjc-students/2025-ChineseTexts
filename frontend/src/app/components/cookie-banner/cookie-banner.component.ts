import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { isPlatformBrowser } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-cookie-banner',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './cookie-banner.component.html',
  styleUrl: './cookie-banner.component.scss'
})
export class CookieBannerComponent implements OnInit {

  visible = false;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const consent = localStorage.getItem('cookie_consent');
    if (!consent) {
      this.visible = true;
      this.disableAnalytics();
    } else if (consent === 'accepted') {
      this.enableAnalytics();
    }
  }

  accept(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('cookie_consent', 'accepted');
    this.visible = false;
    this.enableAnalytics();
  }

  reject(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('cookie_consent', 'rejected');
    this.visible = false;
    this.disableAnalytics();
  }

  private enableAnalytics(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    (window as any)['ga-disable-G-M6LFWC8Z2H'] = false;
    if (typeof (window as any).gtag === 'function') {
      (window as any).gtag('consent', 'update', {
        analytics_storage: 'granted'
      });
    }
  }

  private disableAnalytics(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    (window as any)['ga-disable-G-M6LFWC8Z2H'] = true;
    if (typeof (window as any).gtag === 'function') {
      (window as any).gtag('consent', 'update', {
        analytics_storage: 'denied'
      });
    }
  }
}