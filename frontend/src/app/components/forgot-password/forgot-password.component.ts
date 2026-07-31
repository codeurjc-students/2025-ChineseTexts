import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { LoginService } from '../../services/login.service';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';

/**
 * Página "¿Olvidaste tu contraseña?" (/forgot-password, enlazada desde el modal
 * de login): pide el email y solicita al backend un enlace de restablecimiento.
 *
 * Anti-enumeración: el backend responde 200 exista o no la cuenta, así que aquí
 * se muestra SIEMPRE el mismo mensaje neutro de éxito ("si existe una cuenta...").
 * Página noindex (seo.config) y fuera del sitemap, como el resto de páginas
 * transaccionales.
 */
@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss'
})
export class ForgotPasswordComponent {

  email = '';
  emailError = '';
  errorMessage = '';
  sending = false;
  sent = false;

  constructor(
    private login: LoginService,
    private transloco: TranslocoService
  ) {}

  submit(): void {
    this.emailError = '';
    this.errorMessage = '';
    // Misma validación estricta que el login y el signup.
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email.trim())) {
      this.emailError = this.transloco.translate('forgotPassword.form.emailInvalid');
      return;
    }
    this.sending = true;
    this.login.forgotPassword(this.email.trim()).subscribe({
      next: () => { this.sending = false; this.sent = true; },
      error: (err) => {
        this.sending = false;
        this.errorMessage = this.transloco.translate(
          err?.status === 429 ? 'forgotPassword.errors.tooManyRequests'
                              : 'forgotPassword.errorGeneric');
      }
    });
  }
}
