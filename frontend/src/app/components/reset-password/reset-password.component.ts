import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { LoginService } from '../../services/login.service';
import { AuthUiService } from '../../services/auth-ui.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';

/**
 * Página "Nueva contraseña" (/reset-password?token=…), destino del enlace del
 * email de restablecimiento. Con token válido fija la nueva contraseña (un solo
 * uso) y ofrece iniciar sesión; sin token o con token inválido/caducado ofrece
 * pedir un enlace nuevo. Página noindex y fuera del sitemap, como /forgot-password.
 */
@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent implements OnInit {

  token = '';
  newPassword = '';
  confirmPassword = '';
  passwordError = '';
  errorMessage = '';
  saving = false;
  done = false;
  /** Sin ?token en la URL (o consumido/caducado según el backend): estado inválido. */
  invalidLink = false;

  constructor(
    private route: ActivatedRoute,
    private login: LoginService,
    private authUi: AuthUiService,
    private localeNav: LocaleNavService,
    private transloco: TranslocoService
  ) {}

  ngOnInit(): void {
    // El token viaja en query param; suscripción (no snapshot) por si el usuario
    // abre un segundo enlace de reset sin salir de la página.
    this.route.queryParamMap.subscribe(params => {
      this.token = params.get('token') ?? '';
      this.invalidLink = !this.token;
    });
  }

  submit(): void {
    this.passwordError = '';
    this.errorMessage = '';
    // Mismo mínimo que el formulario de registro (6) + confirmación como en el perfil.
    if (this.newPassword.length < 6) {
      this.passwordError = this.transloco.translate('resetPassword.form.tooShort');
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.passwordError = this.transloco.translate('resetPassword.form.mismatch');
      return;
    }
    this.saving = true;
    this.login.resetPassword(this.token, this.newPassword).subscribe({
      next: () => { this.saving = false; this.done = true; },
      error: (err) => {
        this.saving = false;
        if (err?.error?.code === 'INVALID_OR_EXPIRED_TOKEN') {
          // Token consumido o caducado: mismo estado que un enlace sin token,
          // con el CTA de pedir un enlace nuevo.
          this.invalidLink = true;
        } else {
          this.errorMessage = this.transloco.translate('resetPassword.errorGeneric');
        }
      }
    });
  }

  /** Tras el éxito: a la home y se abre el modal de login del header. */
  goToLogin(): void {
    this.localeNav.navigate(['/']);
    this.authUi.openLogin();
  }
}
