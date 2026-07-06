import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { Meta, Title } from '@angular/platform-browser';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { LocaleNavService } from '../../i18n/locale-nav.service';

@Component({
  selector: 'app-success',
  standalone: true,
  imports: [CommonModule, TranslocoModule],
  templateUrl: './success.component.html',
  styleUrls: ['./success.component.scss']
})
export class SuccessComponent {

  successMessage = '';

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private meta: Meta,
    private title: Title,
    private transloco: TranslocoService,
    private localeNav: LocaleNavService
  ) {
    this.successMessage = this.transloco.translate('success.defaultMessage');

    // SEO
    this.title.setTitle(this.transloco.translate('success.seo.title'));
    this.meta.updateTag({
      name: 'description',
      content: this.transloco.translate<string>('success.seo.description')
    });

    // Recibir mensaje dinámico desde query params
    this.route.queryParams.subscribe(params => {
      if (params['msg']) {
        this.successMessage = params['msg'];
      }
    });
  }

  goHome() {
    this.localeNav.navigate(['/']);
  }
}
