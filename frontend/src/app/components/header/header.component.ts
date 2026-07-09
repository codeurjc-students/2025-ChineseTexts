import { CommonModule } from '@angular/common';
import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { LoginService } from '../../services/login.service';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { startWith } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { AuthUiService } from '../../services/auth-ui.service';
import { Lang, addLangPrefix, stripLangPrefix } from '../../i18n/locale.util';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent implements OnInit {

  loginEmail = '';
  loginPassword = '';
  messageError = '';
  emailError = '';
  passwordError = '';
  loginAttempted = false;
  isLoggedIn$: Observable<boolean>;
  authReady = false;
  /** Active UI language, for highlighting the correct flag. */
  lang$: Observable<string>;

  constructor(
    public loginService: LoginService,
    private router: Router,
    private transloco: TranslocoService,
    private localeNav: LocaleNavService,
    private authUi: AuthUiService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isLoggedIn$ = this.loginService.loggedIn$;
    this.lang$ = this.transloco.langChanges$.pipe(startWith(this.transloco.getActiveLang()));
  }

  /**
   * Switches the UI language: sets the active Transloco language and navigates to
   * the SAME route under the target language's URL prefix, preserving the current
   * path, params (e.g. /text/:id) and query string. SSR-safe (only runs on click).
   */
  public switchLang(target: Lang): void {
    if (this.transloco.getActiveLang() === target) return;
    const stripped = stripLangPrefix(this.router.url);
    this.transloco.setActiveLang(target);
    this.router.navigateByUrl(addLangPrefix(stripped, target));
  }

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      // Esperamos a que el LoginService haya verificado el estado real
      this.loginService.loggedIn$.subscribe(() => {
        this.authReady = true;
      });
      // A "Log in" action elsewhere (e.g. a toast) opens this header's login modal.
      this.authUi.openLogin$.subscribe(() => this.openLoginModal());
    } else {
      // En SSR no mostramos nada condicionado al login
      this.authReady = false;
    }
  }

  /** Opens the Bootstrap login modal programmatically (browser only). */
  private openLoginModal(): void {
    const el = document.getElementById('loginModal');
    const bs = (window as any).bootstrap;
    if (el && bs?.Modal) {
      bs.Modal.getOrCreateInstance(el).show();
    }
  }

  public login() {
    this.loginAttempted = true;
    this.emailError = '';
    this.passwordError = '';
    this.messageError = '';

    let valid = true;

    if (!this.loginEmail.trim()) {
      this.emailError = this.transloco.translate('header.login.emailRequired');
      valid = false;
    } else if (!this.isValidEmail(this.loginEmail)) {
      this.emailError = this.transloco.translate('header.login.emailInvalid');
      valid = false;
    }

    if (!this.loginPassword.trim()) {
      this.passwordError = this.transloco.translate('header.login.passwordRequired');
      valid = false;
    }

    if (!valid) return;

    this.loginService.login(this.loginEmail, this.loginPassword).subscribe({
      next: () => {
        this.loginService.reqIsLogged().subscribe({
          next: () => {
            this.loginEmail = '';
            this.loginPassword = '';
            this.loginAttempted = false;
            this.emailError = '';
            this.passwordError = '';
            this.messageError = '';
            const modalEl = document.getElementById('loginModal');
            if (modalEl) {
              const modal = (window as any).bootstrap?.Modal?.getInstance(modalEl);
              modal?.hide();
            }
            document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
            document.body.classList.remove('modal-open');
            document.body.style.removeProperty('overflow');
            document.body.style.removeProperty('padding-right');
          }
        });
      },
      error: (err) => {
        this.loginEmail = '';
        this.loginPassword = '';
        this.loginAttempted = false;
        // A blocked account returns 403; anything else is treated as invalid
        // credentials. Both messages are rendered in the active UI language.
        this.messageError = err?.status === 403
          ? this.transloco.translate('header.login.blocked')
          : this.transloco.translate('header.login.incorrect');
      }
    });
  }

  public logout(): void {
    this.loginService.logout().subscribe({
      next: () => {
        this.localeNav.navigate(['/']);
      },
      error: (error) => {
        console.error('Error en logout:', error);
      }
    });
  }

  private isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }
}