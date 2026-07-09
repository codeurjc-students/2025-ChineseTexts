import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';
import { LoginService } from '../../services/login.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {

  constructor(
    private loginService: LoginService,
    private localeNav: LocaleNavService,
    private toast: ToastService,
    private transloco: TranslocoService
  ) {}

  /**
   * "Go Premium" from the home pricing card. Logged-in users go to the pricing page
   * to pick a plan and pay; anonymous users get a notice with a sign-up action
   * instead of bouncing through /premium → signup (fewer hops, less confusion).
   */
  goPremium(): void {
    if (this.loginService.isLogged()) {
      this.localeNav.navigate(['/premium']);
    } else {
      this.toast.show(
        this.transloco.translate('notice.signUpForPremium'),
        this.transloco.translate('notice.signUp'),
        () => this.localeNav.navigate(['/signup'])
      );
    }
  }
}
