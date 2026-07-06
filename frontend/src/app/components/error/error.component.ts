import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { Meta, Title } from '@angular/platform-browser';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { LocaleNavService } from '../../i18n/locale-nav.service';

@Component({
  selector: 'app-error',
  standalone: true,
  imports: [CommonModule, TranslocoModule],
  templateUrl: './error.component.html',
  styleUrls: ['./error.component.scss']
})
export class ErrorComponent {

  errorMessage = '';

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private meta: Meta,
    private title: Title,
    private transloco: TranslocoService,
    private localeNav: LocaleNavService
  ) {
    this.errorMessage = this.transloco.translate('error.defaultMessage');

    // SEO
    this.title.setTitle(this.transloco.translate('error.seo.title'));
    this.meta.updateTag({
      name: 'description',
      content: this.transloco.translate<string>('error.seo.description')
    });

    // Recibir mensaje dinámico desde query params
    this.route.queryParams.subscribe(params => {
      if (params['msg']) {
        this.errorMessage = params['msg'];
      }
    });
  }

  goHome() {
    this.localeNav.navigate(['/']);
  }
}
