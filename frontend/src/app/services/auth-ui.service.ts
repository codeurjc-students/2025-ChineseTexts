import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { TranslocoService } from '@jsverse/transloco';

import { ToastService } from './toast.service';
import { LocaleNavService } from '../i18n/locale-nav.service';

/**
 * Small helper for "you must be signed in" moments. Shows a non-intrusive toast with
 * BOTH a "Create account" action (→ /signup) and a "Log in" action (→ opens the header
 * login modal, for users who already have an account) instead of yanking the user to
 * another page. The header subscribes to {@link openLogin$} to open its login modal.
 */
@Injectable({ providedIn: 'root' })
export class AuthUiService {

  private readonly openLoginSubject = new Subject<void>();
  /** Emits when something requests the login modal (HeaderComponent opens it). */
  readonly openLogin$ = this.openLoginSubject.asObservable();

  constructor(
    private toast: ToastService,
    private localeNav: LocaleNavService,
    private transloco: TranslocoService
  ) {}

  openLogin(): void {
    this.openLoginSubject.next();
  }

  /** Shows a sign-in notice with "Create free account" + "Log in" actions. */
  promptAuth(messageKey: string): void {
    this.toast.show(this.transloco.translate(messageKey), [
      { label: this.transloco.translate('notice.signUp'), run: () => this.localeNav.navigate(['/signup']) },
      { label: this.transloco.translate('notice.logIn'), run: () => this.openLogin() }
    ]);
  }
}
